#!/bin/bash

echo ''
echo -ne '      Total:\t'
echo     ` find -type f -iname \*.jar.txt | wc --lines `
echo ''
echo -e  '            \tPerfect\tCorrect\tWrong'
echo ''
echo -ne '   1  Single:\t'
echo -n ` if [ -d ./SINGLE/PERFECT/1.000/ ]; then find ./SINGLE/PERFECT/1.000/ -type f -iname \*.jar.txt | wc --lines; else echo -n 0 ; fi `
echo -ne '\t'
echo -n ` if [ -d ./SINGLE/CORRECT/1.000/ ]; then find ./SINGLE/CORRECT/1.000/ -type f -iname \*.jar.txt | wc --lines; else echo -n 0 ; fi `
echo -ne '\t'
echo    ` if [ -d ./SINGLE/WRONG/1.000/ ];   then find ./SINGLE/WRONG/1.000/   -type f -iname \*.jar.txt | wc --lines; else echo -n 0 ; fi `

echo -ne '   1  Multi:\t'
echo -n ` if [ -d ./MULTI/PERFECT/1.000/ ];  then find ./MULTI/PERFECT/1.000/  -type f -iname \*.jar.txt | wc --lines; else echo -n 0 ; fi `
echo -ne '\t'
echo -n ` if [ -d ./MULTI/CORRECT/1.000/ ];  then find ./MULTI/CORRECT/1.000/  -type f -iname \*.jar.txt | wc --lines; else echo -n 0 ; fi `
echo -ne '\t'
echo    ` if [ -d ./MULTI/WRONG/1.000/ ];    then find ./MULTI/WRONG/1.000/    -type f -iname \*.jar.txt | wc --lines; else echo -n 0 ; fi `

echo ''

echo -ne '(0-1) Single:\t'
echo -n ` if [ -d ./SINGLE/PERFECT/0.999/ ]; then find ./SINGLE/PERFECT/0.999/ -type f -iname \*.jar.txt | wc --lines; else echo -n 0 ; fi `
echo -ne '\t'
echo -n ` if [ -d ./SINGLE/CORRECT/0.999/ ]; then find ./SINGLE/CORRECT/0.999/ -type f -iname \*.jar.txt | wc --lines; else echo -n 0 ; fi `
echo -ne '\t'
echo    ` if [ -d ./SINGLE/WRONG/0.999/ ];   then find ./SINGLE/WRONG/0.999/   -type f -iname \*.jar.txt | wc --lines; else echo -n 0 ; fi `

echo -ne '(0-1) Multi:\t'
echo -n ` if [ -d ./MULTI/PERFECT/0.999/ ];  then find ./MULTI/PERFECT/0.999/  -type f -iname \*.jar.txt | wc --lines; else echo -n 0 ; fi `
echo -ne '\t'
echo -n ` if [ -d ./MULTI/CORRECT/0.999/ ];  then find ./MULTI/CORRECT/0.999/  -type f -iname \*.jar.txt | wc --lines; else echo -n 0 ; fi `
echo -ne '\t'
echo    ` if [ -d ./MULTI/WRONG/0.999/ ];    then find ./MULTI/WRONG/0.999/    -type f -iname \*.jar.txt | wc --lines; else echo -n 0 ; fi `

echo ''
echo -ne '   No Match:\t'
echo -n ` if [ -d ./NO_MATCH/ ];             then find ./NO_MATCH/             -type f -iname \*.jar.txt | wc --lines; else echo -n 0 ; fi `
echo ''
echo ''

