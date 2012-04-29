#!/usr/bin/perl

# we are going to output the SHA1 of the signature of a method...

#<classname>;<filename>;<sha1>;<level>;<containerLevel0>;..<containerLevel-n>

# that is, we are going to output the containers to the level we need

use strict;
use Digest::SHA1  qw(sha1 sha1_hex sha1_base64);

# we have to program a finite state machine


my @containers; # array of the containers files of the currently processed file

# example input

#>>>>>>>;Starting;/tmp/glassfish/jars.tar.bz2
#P;0;bin/glassfish-jmac-api-2.1.1-b31.jar;glassfish-jmac-api-2.1.1-b31;bin/;.jar
#>>>>>>>;Starting;/tmp/j.19998/a/bin/glassfish-jmac-api-2.1.1-b31.jar
#P;1;javax/security/auth/message/MessageInfo.class;MessageInfo;javax/security/auth/message/;.class
#
#n;javax/security/auth/message/MessageInfo.class
#c;0;public interface javax.security.auth.message.MessageInfo
#m;0;  public Object getRequestMessage()
#m;0;  public Object getResponseMessage()
#m;0;  public void setRequestMessage(Object)
#m;0;  public void setResponseMessage(Object)
#m;0;  public Map getMap()
#P;1;javax/security/auth/message/ClientAuth.class;ClientAuth;javax/security/auth/message/;.class
#

# soo.. >> and <<< are to be skipped
# empty lines, means a new class is coming

# several types of records
#P;  processing
#n;  beginning of a filename
#c;  beginning of a class
#m;  method

#each of these records has 
#<type>;<level>;data
# P record has more info at the ned, but it does not matter

my $currLevel = -1;
my $currFile = "";
my $currContainer = "";

my @toProcess;

while (<>) {
    chomp;
    # skip lines that we don't care about
    next if $_ eq "";
    next if $_ =~ /^>>>>>/;
    next if $_ =~ /^<<<<</;

    my @fields = split(/;/);
    
    my $recordID = $fields[0];
    
    if ($recordID eq "P") {

        # check if we have something to do
        if (scalar(@toProcess) > 0) {
            Process_Data(\@toProcess);
            @toProcess = ();
        }

# format of this record
#P;<level>;<filename>;<basename>;<directory>;<extension>;<container>
        # we need to maintain a stack of containers 
        ##########end of processing record P
        Process_Type_P(@fields);

        print "P;$fields[2];;$currLevel;", join(';', @containers), "\n";
        $currFile = $fields[2];

        # ok, now we have the containers
    } elsif ($recordID =~ /^[<>]+/) {
        # ignore;
        ;
    } else {
        # save the record to process later
        push(@toProcess, $_);
    }
}
if (scalar(@toProcess) > 0) {
    Process_Data(\@toProcess);
}

sub Process_Data
{
    my ($arrayRef) = @_;
    my @record;
    my $name  = "";
    foreach my $line (@$arrayRef) {
        # skip empty lines
        next if $line eq "";
        if ($line =~ /^n;\[virtual\](.+)$/) {
            Process_Record($name, \@record);
            @record = ();
            # virtual class
            $name = substr($line, 2);
        } elsif ($line =~ /^n;/) {
            Process_Record($name, \@record);
            @record = ();
            $name = substr($line, 2);
        } else {
           push (@record, $line);
        }
    }
    Process_Record($name, \@record);
}

sub Process_Record
{
    my ($name, $arrayRef) = @_;
    return if (scalar(@$arrayRef) == 0);
    my $temp = join("\n", @$arrayRef);
    my $digest = sha1_hex($temp);

    print "SHA1;$name;$digest;$currFile;$currLevel;", join(';', @containers), "\n";
    
}


sub Process_Type_P
{
    my (@f) = @_;
    
    my $thisContainer = $f[6];
    my $thisLevel = $f[1];
    my $thisFile = $f[0];
    
    if ($currLevel != $thisLevel) {
        my $diff = $thisLevel - $currLevel ;
        die "Assertion failed [$diff]"  unless $diff <= 1;
        if ($diff > 0) {
            #we are into a new level, so push the current level
            push(@containers, $thisContainer);
        } else {
            # we are back... pop
            while ($diff < 0) {
                pop(@containers);
                $diff++;
            }
        }
#        print "New container level ", join(';', @containers), "\n";
        $currLevel = $thisLevel;
    } else {
        # if the levels are the same then we need to check if the top of the containers is the same
        die "empty containers " unless scalar(@containers) > 0;
        if ($containers[scalar(@containers) -1 ] ne $thisContainer) {
            #not the same, replace
            $containers[scalar(@containers) -1 ] = $thisContainer;
        }
    }
    
}
