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

echo 'DROP TABLE IF EXISTS files' | psql maven 

echo 'SELECT NOW()' | psql maven

unxz -c $f | cut -b 3- | cat files-definition.sql - end-data | psql maven  

echo 'DROP TABLE IF EXISTS sigs' | psql maven

echo 'SELECT NOW()' | psql maven

unxz -c $s | cut -b 3- | cat sigs-definition.sql - end-data | psql maven

echo 'SELECT NOW()' | psql maven

echo ''
echo 'Tables loaded (sigs and files).'
echo 'Now creating indexes and tallies.'
echo 'It will take a few hours....'
echo ''

psql maven < ../prep.sql


