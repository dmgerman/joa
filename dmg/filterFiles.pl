#!/usr/bin/perl


use strict;
use File::Basename;
use File::Temp qw/ tempfile tempdir /;

# autoflush
# yes, perl is unreadable by the uninnitiated

my $tempDirWork = tempdir( "/tmp/j.XXXXXXX" );
my $tempFileName = $tempDirWork . "/tempXXXX.out";

my @packSuffixList = (qr/\.tar.gz$/, qr/\.jar$/, qr/\.tar.bz2$/, qr/\.zip$/,qr/\.ear$/,qr/\.war$/, qr/\.tgz$/);

my @javaSuffixList     = (qr/\.class$/, qr/\.java$/);

my @suffixList = (@packSuffixList, @javaSuffixList);


# so we are going to open a pipe to the signature extractor, and send each file to it.
# then save the pack files to do afterwards 

while (<>) {
    my $f = $_;
    my $lowerF= $f;
    $lowerF =~ tr/[A-Z/[a-z]/;

    my ($name,$path,$suffixLower) = fileparse($lowerF,@suffixList);

    next unless grep($suffixLower, @suffixList);
    # subclass jars are handled by the parser

    print $_;
}
