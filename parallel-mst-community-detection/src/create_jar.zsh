#!/bin/bash
javac *.java
javac src/*.java
jar cfm ParallelST Manifest.txt *.class src/*.class