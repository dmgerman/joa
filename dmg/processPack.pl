#!/usr/bin/perl


use strict;
use File::Basename;
use Cwd;
use File::Temp qw/ tempfile tempdir /;
use Digest::SHA1  qw(sha1 sha1_hex sha1_base64);

# autoflush
# yes, perl is unreadable by the uninnitiated
$| = 1;



my $pid = $$;
my $startDir = cwd();

#my $command = sha1sum `which sha1sum` | perl -pe s'/^([^ ]+) +([^ ]+)\n$/$2;$1\n/;'


#my $command = "extractSig.sh"

my $tempDirWork = tempdir( "/tmp/j.XXXXXXX" );
my $tempFileName = $tempDirWork . "/tempXXXX.out";

my $command = shift;
my $level = shift;
my $file = shift;
my $shaFile = shift;

my %shaFiles;

die "Usage $0 <command> <level> <file>" unless 
    $command ne "" and
    $level ne "" and
    $file;


    
print STDERR "$file;$level\n";

if (not ($file =~ m@^/@)) {
    # file is relative. fix
    $file = $startDir . "/", $file;
}

if ($shaFile eq "") {
    $shaFile = Sha_File($file);
}
print "Z;Starting;$file;$shaFile\n";


#create working directories
chdir($tempDirWork);

# first do the sha1 of it

my @files = Extract_Files($file);

# start the output process

open(OUT, "|$command > $tempFileName") or die "unable to start output command [$command]\n";

#die "[$command][$tempFileName]";


# So now the array files has all the files in the tar
#we can now process the array

my @packSuffixList = (qr/\.tar.gz$/, qr/\.jar$/, qr/\.tar.bz2$/, qr/\.zip$/,qr/\.ear$/,qr/\.war$/, qr/\.tgz$/);
my @javaSuffixList     = (qr/\.class$/, qr/\.java$/);

my @suffixList = (@packSuffixList, @javaSuffixList);


# so we are going to open a pipe to the signature extractor, and send each file to it.
# then save the pack files to do afterwards 

my @packFiles = ();

my @todoFiles = ();

foreach my $f (@files) {
    my $lowerF= $f;
    $lowerF =~ tr/[A-Z/[a-z]/;

    my ($name,$path,$suffixLower) = fileparse($lowerF,@suffixList);

    next unless grep($suffixLower, @suffixList);
    # subclass jars are handled by the parser

    next if ($suffixLower eq ".class" and  $name =~ /\$/) ;

    my $suffix;
    ($name,$path,$suffix) = fileparse($f, @suffixList);
    my $sha = Sha_File($f);

    $shaFiles{$f} = $sha;

    print "P;${level};$f;$name;$path;$suffix;$shaFile;$sha\n";

    if ($suffixLower eq ".java" or
        $suffixLower eq ".class") {
        push (@todoFiles, $f);
    } elsif ($suffixLower =~ /^\.(jar|zip|tar\.gz|tar\.bz2|tgz|war|ear)$/) {
        push @packFiles, $f;
    } else {
        printf "^E;To implement this type of [$file][$f][$lowerF][$suffixLower][$suffix]\n'";
    }
}

if (scalar(@todoFiles) > 0) {
    open(OUT, "|$command > $tempFileName") or die "unable to start output command [$command]\n";
    foreach my $f (@todoFiles) {
#        print STDERR "Todo [$f]\n";
        print OUT "$f\n";
    }
    close OUT;
# ok, at this point we open the signatures file and concatenae it to stdout
    
    open(IN, $tempFileName) or die "Unable to open temp file [$tempFileName]";
    while (<IN>){
        print ;
    }
    close IN;
}



foreach my $f (@packFiles) {
    print STDERR "$f\n";

    $level++;

    print `$0 '$command' $level $tempDirWork/$f $shaFiles{$f}`;
    print "T;END;$f\n";
    $level--;
}



chdir($startDir);
# delete working directories

#print STDERR "Deleting temporary directory...";

unlink($tempFileName);

my $error = `rm -rf $tempDirWork`;

if ($error ne "") { print STDERR "Remdir error: $error\n";}

#`rmdir -f /tmp/$$`;


print "Z;Ending;$file\n";

exit 0;

#----------------------------------------------------------------------
sub Extract_Files
{
    my ($file) = @_;
    my @files = ();

    Unpack($file);
    
    # find the files in the directory. We sort to make it deterministic
    @files = split('\n', `find $tempDirWork -type f | sort `);

    # now we can safely remove the prefix
    map(s@^${tempDirWork}/@@, @files);    

    # are there any filenames with "::" in its name? we need to use that as a separator
    if (scalar(grep(m@::/@, @files)) > 0) {
        die "files with :: in the name [$file]", join("\n", grep(m@::/@, @files)), "\n";
    }

    return @files;
}


sub Unpack 
{
    my ($file) = @_;
    print "A;BeginUnpack;$file\n";
    print STDERR "A;BeginUnpack;$file\n";
    if ($file =~ /\.tar\.gz$/ or $file =~ /\.tgz$/) {
        my_Exec("tar", "-xivzf", $file);
    } elsif ($file =~ /\.tar$/) {
        my_Exec("tar", "-xivf", $file);
    } elsif ($file =~ /\.(zip|jar|war|ear)$/) {
#        my_Exec("unzip", "-o", "-x", $file, "-d", $tempDirWork);
        my $here = cwd();
        chdir($tempDirWork);
        my_Exec("jar", "xf", $file);
        chdir($here);
    } elsif ($file =~ /\.tar\.bz2$/) {
        my_Exec("tar", "-xivjf", $file);
#        `bunzip2 -c '$file' | tar -C '$tempDirWork' -xvf  - `;
    } else {
        print "E;@@@@@@@@@@@@@@@@@@@@@@@@@@@@@;$file\n";
    }
    print STDERR "A;EndUnpack;$file\n";

    print "A;EndUnpack;$file\n";
    return ();
}


sub my_Exec
{
    my @args = @_;
    
    my $line = join(' ', @args);
    
    my $ret = `$line`;

#    system(@args) == 0
#        or die "system @args failed: $?";
        
    if ($? == -1) {
        print "E;failed to execute: $!\n";
        return -1;
    } elsif ($? & 127) {
        return $ret;
    } else {
        return $? >> 8;
    }
    
}
  

sub Sha_File
{
    my ($file)= @_;
    if (not open(IN, "<$file")) {
        return "-1";
    } else {
        my $data = "";
        while (<IN>) {
            $data .= $_;
        }
        close IN;
        return sha1_hex($data);
    }
}
