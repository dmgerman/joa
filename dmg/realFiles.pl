#!/usr/bin/perl

while (<>) {
    chomp;
    next unless -f $_ and not -l $_;
    print $_, "\n";
}
