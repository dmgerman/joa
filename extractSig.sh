#!/bin/sh
dirname=`dirname $0`

. ${dirname}/classpath.sh

$JAVA_HOME/bin/java -cp $CLASSPATH   ca.juliusdavies.signature.SimpleScan  $*

 
