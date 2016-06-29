#!/usr/bin/env bash
#takes a list of .proto files and converts them into json for BMC checking
#usage: [cmd] [list] [output dir]

for f in `cat $1`;
do
	newf=$(echo $f |rev |cut -d"/" -f 1 |rev)
	echo $f
	java -jar /Users/s/Documents/source/TraceRunner/build/libs/TraceRunner-all-2.0.jar verifTrace $f "$2/$newf.json"
done
