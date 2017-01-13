
import os
import sys
from subprocess import Popen, PIPE

from utils.getAPKInfo import getAPKInfo

def runAutoMonkey(appPackageName, outputProtoPath):

   print "Starting ADB+NetCat Bridge @ 5050..."
   adb_proc = Popen(['adb','reverse','tcp:5050', 'tcp:5050'], stdout=PIPE)
   adb_proc.communicate()
   nc_proc = Popen(['nc','-l','-p','5050'], stdout=PIPE)
   print "Started ADB+NetCat Bridge"


   print "Running Android Monkey on Instrumented App..."
   # adb shell monkey -p your.package.name -v 500
   events = '20'
   trace_proc = Popen(['adb','shell','monkey','-p',appPackageName,'-v',events], stdout=PIPE)
   outcome,error = trace_proc.communicate()
   print "Trace Completed: %s, %s" % (outcome,error)

   print "Stopping the Instrumented App"
   stop_proc = Popen(['adb','shell','am','force-stop',appPackageName], stdout=PIPE)
   outcome,error = stop_proc.communicate()
   print "App Force-Stop Initiated: %s, %s" % (outcome,error)

   trace,_ = nc_proc.communicate()

   with open("%s/%s.out" % (outputProtoPath,"trace"), "w") as f:
      f.write(trace)

def autoMonkey(instrumentedAPKPath, outputProtoPath):

   appPackageName,activityName = getAPKInfo(instrumentedAPKPath)
   print "Instrumented App Package Name: %s" % appPackageName

   print "Uninstalling Previous Version of Instrumented App..."
   adb_proc = Popen(['adb','uninstall',appPackageName], stdout=PIPE)
   outcome,_ = adb_proc.communicate()
   print "Uninstall Completed: %s" % outcome

   print "Installing Current Version of Instrumented App..."
   adb_proc = Popen(['adb','install',instrumentedAPKPath], stdout=PIPE)
   outcome,_ = adb_proc.communicate()
   print "Install Completed: %s" % outcome

   runAutoMonkey(appPackageName, outputProtoPath)

   print "All Done!"

if __name__ == "__main__":
   if len(sys.argv) != 3:
      print "usage: python autoMonkey.py <Path to (Resigned) Instrumented APK> <Path to Output Folder>"
      sys.exit(1)

   instrumentedAPKPath = sys.argv[1]
   outputProtoPath     = sys.argv[2]

   autoMonkey(instrumentedAPKPath, outputProtoPath)


