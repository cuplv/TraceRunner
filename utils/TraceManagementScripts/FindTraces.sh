#!/usr/bin/env bash
#takes a directory and finds all the traces which contain a given string when you print the protobuf

echo $1

for f in `find $1 -name "*.proto"`;
do
	contains=$(java -jar  /Users/s/Documents/source/TraceRunner/build/libs/TraceRunner-all-2.0.jar read $f |grep AsyncTask)
	if [ -n "$contains" ]
	then
		echo $f
	fi

done