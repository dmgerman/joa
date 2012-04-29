#!/usr/bin/perl

#use DBI;
use strict;
use Getopt::Long;
use Digest::SHA1  qw(sha1 sha1_hex sha1_base64);

#my $db = shift;

#die "$0 <db> " unless $db ne "";

#my $dbh = DBI->connect("dbi:Pg:dbname=$db", "", "",
#		       { RaiseError => 1});



my $countFiles = 0;
my $countSigs = 0;
#my $insertSig = $dbh->prepare("insert into sigs(filesha1, classname, sigsha1, losesigsha1,sigsha1re, losesigsha1re) values (?,?,?,?,?,?);");
#my $insertFiles = $dbh->prepare("insert into files(filename, level, basename, path, suffix, infilesha1, filesha1) values (?,?,?,?,?,?,?);");


#my $sth = $dbh->prepare("select id from roles4 where trim(id) ~ '\\\\(as [^\\)]+\\\\)';");

#my $rc = $sth->execute();

# finite state machine... let us wait for a ^S; record. then we process until we find another S record or
#  ... to be determined

my $currentClass;
my $currentFile;
my $sig;
my $loseSig;
my $currentType;
my %shas;

while (<>) {
    chomp;
    my @f = split(';');
    my $recordType = $f[0];

    # it this the part  of a signature?
    if ($recordType eq "c") {
        # concatenate
        # ok, this is the case where we want to replace the signature...
        
        # make the lose signature...
        my $temp = $_;
        $temp =~ s/([^. ]+\.)+//;

        $sig .= $_ . "\n";
        $loseSig .= $temp . "\n";
        # simply jump to next record

#        print STDERR "$_;$temp\n";

        next;
    } 
    if ($recordType eq "f" or
        $recordType eq "m" ) {
        # concatenate
        $sig .= $_ . "\n";
        $loseSig .= $_ . "\n";
        # simply jump to next record
        next;
    } 
    # then it is a control record
    # check if we have to process current signature

    if ($sig ne "") {
        die "i don't have this file [$currentFile]" unless defined($shas{$currentFile});
 
        Insert_Signature($shas{$currentFile}, $currentClass, $currentType, $sig,$loseSig,0);
        $loseSig = $sig = "";
    }
    
    if ($recordType eq "S") {
        # we begin a new source file.
        #No longer needed;
        ;
    } elsif ($recordType eq "n") {
        $currentFile = $f[1];
        $currentClass = $f[2];
    } elsif ($recordType eq "SHA1") {
        # empty line end of signature.. will be processed in next record
        ;
    } elsif ($recordType eq "") {
        # empty line end of signature.. will be processed in next record
        ;
    } elsif ($recordType eq "A") {
        # ignore these records
        ;
    } elsif ($recordType eq "Z") {
        # a new pack is starting...
        %shas = ();
    } elsif ($recordType eq "P") {
        # P records tell us the file, its sha, and where it is found
        # P;level;filename;basename;path;extension;systemPath;sha1
        # P;0;mail-1.4.1.jar;mail-1.4.1;./;.jar;/home/dmg/dontBackup/glassfish/glassfishJars.zip;3a411e666f1930af26f4e3b02c20b3e289c923ff
        # save its signature...
        $shas{$f[2]} = $f[7];
#        print "inserting [$f[2]][$f[7]]\n";
        Insert_Files(@f);
        ;
    } elsif ($recordType eq "E" or
             $recordType eq "e") {
        # error creeating the signature! insert null
        die "it is supposed to be empty sig [$sig] in E record [$currentClass][$currentFile]" 
            if $sig ne "";
 #       Insert_Signature($shas{$currentFile}, $currentClass, "E", "", "", 1);
    } else {
        die "Illegal record [$recordType][$_]\n";
    }
}
# process the very last signature
if ($sig ne "") {
    die "Last record is invalid\n";
}


#$dbh -> disconnect;

print STDERR "Inserted [$countFiles] files [$countSigs] signature\n";

exit(0);

sub Insert_Files
{
    my ($key, $level, $filename, $basename, $path, $ext, $infilesha1, $filesha1) = @_;
    
#F;./;Main;.class;-1;NULL;e20b5e4bd6234322d2d1bce6f06f1c19ac60f8a7;NULL;NULL;NULL;NULL
    # path has fullname, remove filename
    $path =~ s@[^/]+$@@;

#    $insertFiles->execute($f[2],$f[1],$f[3],$f[4],$f[5],$f[6],$f[7]);
    print "F;", join(';', $path, $basename, $ext,$level, $infilesha1, $filesha1,'NULL;NULL;NULL;NULL'), "\n";
    $countFiles++;
}

sub Insert_Signature
{
    my ($file, $class, $type, $sig, $loseSign, $sigIsNull) = @_;

    die "illegal signature File [$file] class [$class] type [$type] sig [$sig] isnullsig[$sigIsNull]" if $class eq "" or $file eq "";

    my $sha1reordered = sha1_hex(Reorder_Class_Signature(\$sig));
    my $loseSha1reordered = sha1_hex(Reorder_Class_Signature(\$loseSig));

    my $sha1 = sha1_hex($sig);
    my $loseSha1 = sha1_hex($loseSig);

#S;e20b5e4bd6234322d2d1bce6f06f1c19ac60f8a7;Main;d7ee119112b035734ab0eb4172608dd5474cca98;b4d2dc4739b6a86cae46a5c8973b0e762feaa36

    print "S;", join(';', $file, $class, $sha1reordered, $loseSha1reordered), "\n";
#    $insertSig->execute($file, $class,  $sha1, $loseSha1,$sha1reordered, $loseSha1reordered);
    $countSigs++;


#    my $sth = $dbh->prepare("insert into ';");

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



sub Reorder_Class_Signature
{
    my ($ref) = @_;
    
    my $origLen = length($$ref);


    my @lines = split('\n', $$ref);
    my $len = scalar(@lines);

    my ($last, @reordered) = Parse_Class(0, 0, \@lines);

    die "incorrect reorder\n",  join("\n", @lines), "-----\n", join("\n", @reordered), "\nEND\n" unless $len == scalar(@reordered);


    # actually, another assertion is that the lines should not be lost
    my @a = sort @lines;
    my @b = sort @reordered;
    
    for my $i (0..scalar(@a) -1 ) {
        die "noooooooooooooooooooooo" if $a[$i] ne $b[$i];
    }

    my $ret = join("\n", @reordered);
    if ($ret ne '') {
        $ret .= "\n";
    }
    my $len2 = length($ret);
    die "noooot same lenght [$origLen][$len2]\n[$$ref][$ret]\n" if $origLen != $len2;

    return $ret;

}

sub Parse_Class
{
    my ($level, $start, $ref) = @_;

    my @classes = ();
    my @childrenClasses = ();

    my $end = scalar(@$ref);
    my $next ;

    my %class;
    my %childrens;

    my $i = $start;
    my @results = ();
    my $currentClass = "";
    # keep doing while we have lines
    while ($i < $end) {
        my $thisLine = $$ref[$i];
#        print "Seetting this line: thisLine[$thisLine]i[$i]\n";
        my @f = split(';', $thisLine);
        my @thisClass;
        # is it at a level above ourselfs
        # then we are done
        if ($f[1] < $level) {
            # return, but make sure current line is reread
            $i--;
            last;
        } elsif ($f[1] == $level) {
            # if we are at the same level, 
            #it is is a class... then we must parse it
            if ($f[0] eq "c") {

#                print "Another class at the same level [$level] [$thisLine]\n";
                # we hit a class. so if we have been processing one, we need to save it
                if ($currentClass ne "") { 
                    # first find its children...
                    foreach my $a (sort @childrenClasses) {
                        my $ref = $childrens{$a};
                        push (@results, @$ref);
                    }
                    # Now save the whole result
                    $class{$currentClass}  = [@results];
                    push(@classes, $currentClass);
                }
                # now a new class is coming in
                $currentClass = $thisLine;
                @results = ($thisLine);
                %childrens = ();
                @childrenClasses = ();
                
            } else {
                # just contatenate 
#                print "Pushighn [$level][$currentClass]thisLine[$thisLine]i[$i]\n";
                push(@results, $thisLine);
            }
        } else {
            # let us assert that we only go down in level and 
            # it is a new class
            die "we incorrectly assumed a class" if $f[0] ne "c";
            # now we are going down... a level, so parse the class
#            print "NewLevel;$i;$thisLine\n";
            ($i, @thisClass) = Parse_Class($level+1, $i, $ref);
            push(@childrenClasses, $thisLine);                
            $childrens{$thisLine} = [@thisClass];
        }
        $i++;
    }
#    print "End of loop [$currentClass]\n";
    # first find its children...
    foreach my $a (sort @childrenClasses) {
        $ref = $childrens{$a};
        push (@results, @$ref);
    }
    # Now save the whole result
    $class{$currentClass}  = [@results];
    push(@classes, $currentClass);

    @results = ();
    
    foreach my $a (sort @classes) {
        $ref = $class{$a};
        push (@results, @$ref);
    }

    return ($i, @results);
}
