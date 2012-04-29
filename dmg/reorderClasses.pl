#!/usr/bin/perl

use strict;

my $data = "";

while (<>) {
    next if not /^[cfm];/;
    $data .= $_;
}

my $reorder = Reorder_Class(\$data);

print $reorder;
exit(0);

sub Reorder_Class
{
    my ($ref) = @_;
    
    my $origLen = length($$ref);


    my @lines = split('\n', $$ref);
    my $len = scalar(@lines);

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
    die "noooot same lenght " if $origLen != length($ret);

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
