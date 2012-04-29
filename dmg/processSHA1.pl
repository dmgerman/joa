#!/usr/bin/perl

# we are going to output the SHA1 of the signature of a method...

#<classname>;<filename>;<sha1>;<level>;<containerLevel0>;..<containerLevel-n>

# that is, we are going to output the containers to the level we need

use strict;
use Digest::SHA1  qw(sha1 sha1_hex sha1_base64);

# we have to program a finite state machine


while (<>) {
    chomp;
    my $f = $_;
    if (-f $f) {
        my $text = Sha_File($f);
        my $digest = sha1_hex($text);
        print "SHA1;$f;0;$digest\n";
    } else {
        print "SHA1;$f;-1;\n";        
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
        return $data;
    }
}

