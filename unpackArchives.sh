#!/bin/sh
dirname=`dirname $0`

java -cp ${dirname}/build/sig-extractor.jar:${dirname}/lib/asm-commons-3.3.1.jar:${dirname}/lib/asm-3.3.1.jar:${dirname}/lib/ad-client.jar:${dirname}/lib/tools.jar   ca.juliusdavies.signature.Unpacker  $*

 
