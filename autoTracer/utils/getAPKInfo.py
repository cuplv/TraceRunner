

import os
import sys
from subprocess import Popen, PIPE
import re

def getNameFromLine(line):
    spline = line.split(" ")
    for l in spline:
        if re.match("^name=", l):
            return l[6:-1]

def getAPKInfo(apk_path):
    androidHome = os.environ.get('ANDROID_HOME')
    if androidHome == "":
        raise Exception("please set ANDROID_HOME")
    aapt = androidHome + "/build-tools/24.0.3/aapt"

    process = Popen([aapt, "dump", "badging", apk_path], stdout=PIPE)
    
    (output, err) = process.communicate()
    exit_code = process.wait()
    soutput = output.split("\n")
    package = ""
    activity = ""
    for line in soutput:
       if "package" in line:
          #print line
          package = getNameFromLine(line)
       if "launchable" in line:
          #print line
          activity = getNameFromLine(line)
    return (package, activity)


if __name__ == "__main__":
   if len(sys.argv) != 2:
      print "usage: python getAPKInfo.py <Path to APK>"
      sys.exit(1)

   apk_path = sys.argv[1]

   print getAPKInfo(apk_path)


