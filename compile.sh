#!/bin/sh

find -name '*.java' > sources.txt
javac @sources.txt
rm -rf sources.txt
