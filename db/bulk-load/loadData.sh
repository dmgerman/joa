#!/bin/sh

dirname=`dirname $0`

if [ -z "$1" ]; then
  echo ''
  echo 'Usage: ./loadData.sh [/path/to/files.xz] [/path/to/sigs.xz]'
  echo ''
  echo 'PostgreSQL data loader for the Java Signature Extractor.'
  echo 'by Julius Davies, Daniel M. German.  August 25, 2011.'
  echo ''
  echo ''
  echo 'Note:  You must have a PostgreSQL database named "maven" already'
  echo '       created, and it must not require a password for the current'
  echo '       user (e.g., the command "psql maven" must work).'
  echo ''
  exit 1
fi

f=`readlink -f $1`
s=`readlink -f $2`
cd $dirname

export DB=little

echo 'DROP TABLE IF EXISTS files' | psql $DB 

echo 'SELECT NOW()' | psql $DB 

unxz -c $f | cut -b 3- | cat files-definition.sql - end-data | psql $DB 

echo 'DROP TABLE IF EXISTS sigs' | psql $DB

echo 'SELECT NOW()' | psql $DB 

unxz -c $s | cut -b 3- | cat sigs-definition.sql - end-data | psql $DB 

echo 'SELECT NOW()' | psql $DB 

echo ''
echo 'Tables loaded (sigs and files).'
echo 'Now creating indexes and tallies.'
echo 'It will take a few hours....'
echo ''

psql $DB < ../prep.sql


