import os
import sys
import subprocess

if len(sys.argv) != 2:
	raise Exception("usage: [textFile]")

f = open(sys.argv[1],'r')

lines = f.readlines()
hist = {}
for l in lines:
	if l in hist:
		hist[l] = hist[l]+1
	else:
		hist[l] = 1

srt = sorted(hist.items(), key=lambda x : x[1])
srt.reverse()
print "size: " + str(len(lines))
for i in xrange(50):
	print srt[i]
f.close()
