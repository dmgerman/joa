#!/usr/bin/perl

#use DBI;
use strict;
use Getopt::Long;
use Digest::SHA1  qw(sha1 sha1_hex sha1_base64);



my $countFiles = 0;
my $countSigs = 0;

my $currentClass;
my $currentFile;
my $sig;
my $loseSig;
my $currentType;
my %shas;
my $containerSha= "";
my $containerLevel = '';
my $first = 1;

while (<>) {
    chomp;
    my @f = split(';');
    my $recordType = $f[0];

    if ($recordType eq "S") {
        # we begin a new source file.
        #No longer needed;
        print "$_\n";
    } elsif ($recordType eq "A") {
        # ignore these records
        ;
    } elsif ($recordType eq "T") {
        # ignore these records
        ;
    } elsif ($recordType eq "F") {
        # ignore these records
        # splice it in
        $f[5] = $containerSha;
        $f[4] = $containerLevel;
        print join(';', @f),"\n";
    } elsif ($recordType eq "Z") {
        # a new pack is starting...
        %shas = ();
        $containerSha = $f[3];
        if ($first) {
            my ($filename,$basename, $path, $ext) = ();
            $filename = $f[2];
            if ($filename =~ m@^(.*/)([^/]+)(\.[^.]+)$@) {
                $basename = $2;
                $path = $1;
                $ext = $3;
            }
            print "F;", join(';', $path, $basename, $ext,-1, 'NULL', $containerSha,'NULL;NULL;NULL;NULL'), "\n";
            $first = 0;
        }
    } elsif ($recordType eq "P") {
        # P records tell us the file, its sha, and where it is found
        # P;level;filename;basename;path;extension;systemPath;sha1
        # P;0;mail-1.4.1.jar;mail-1.4.1;./;.jar;/home/dmg/dontBackup/glassfish/glassfishJars.zip;3a411e666f1930af26f4e3b02c20b3e289c923ff
        # save its signature...
        $shas{$f[2]} = $f[7];
        $containerLevel = $f[1];
#        print "inserting [$f[2]][$f[7]]\n";
        if (not ($f[5] eq ".class" or
                 $f[5] eq ".java")) {
            Print_File(@f);
        }
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

sub Print_File
{
    my ($key, $level, $filename, $basename, $path, $ext, $infilesha1, $filesha1) = @_;
    
#F;./;Main;.class;-1;NULL;e20b5e4bd6234322d2d1bce6f06f1c19ac60f8a7;NULL;NULL;NULL;NULL
    # path has fullname, remove filename
    $path =~ s@[^/]+$@@;

#    $insertFiles->execute($f[2],$f[1],$f[3],$f[4],$f[5],$f[6],$f[7]);
    print "F;", join(';', $path, $basename, $ext,$level, $infilesha1, $filesha1,'NULL;NULL;NULL;NULL'), "\n";
    $countFiles++;
}

