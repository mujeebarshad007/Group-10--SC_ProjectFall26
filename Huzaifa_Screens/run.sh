#!/bin/sh
# Compile and run PULSE (needs a JDK: javac + java)
cd "$(dirname "$0")"
mkdir -p out
javac -d out *.java && java -cp out Main
