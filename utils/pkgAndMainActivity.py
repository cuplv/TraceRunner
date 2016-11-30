#!/usr/bin/python
import os
import sys
from subprocess import Popen, PIPE
import re

if len(sys.argv) != 2:
    raise Exception("usage [source path]")

androidHome = os.environ.get('ANDROID_HOME')
if androidHome == "":
    raise Exception("please set ANDROID_HOME")

# print "android home: %s" % androidHome
aapt = androidHome + "/build-tools/24.0.3/aapt"

def getNameFromLine(line):
    spline = line.split(" ")
    for l in spline:
        if re.match("^name=", l):
            return l[6:-1]


srcdir = sys.argv[1]
manifests = []
for root, dirs, files in os.walk(srcdir):
    for f in files:
        if f.endswith(".apk"):
            print "apk: " + root + "/" + f
            process = Popen([aapt, "dump", "badging", root + "/" + f], stdout=PIPE)
            (output, err) = process.communicate()
            exit_code = process.wait()
            soutput = output.split("\n")
            for line in soutput:
                package = ""
                activity = ""
                if "package" in line:
                    #print line
                    package = getNameFromLine(line)
                if "launchable" in line:
                    #print line
                    activity = getNameFromLine(line)
                if package != "":
                    print package
                if activity != "":
                    print activity

            print "----------------------------------"






