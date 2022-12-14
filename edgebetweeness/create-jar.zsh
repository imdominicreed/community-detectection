#!/bin/bash
javac *.java src/*.java
jar cfm SerialGirvanNewman Manifest.txt *.class src/*.class