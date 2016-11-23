
import os
import sys
from subprocess import Popen, PIPE

from utils.getAPKInfo import getAPKInfo

def pwd():
   p = Popen(['pwd'], stdout=PIPE)
   (stdout, error) = p.communicate()
   return stdout.replace('\n','')

def dirName(path):
   p = Popen(['dirname',path], stdout=PIPE)
   (stdout, error) = p.communicate()
   return stdout.replace('\n','')

def runAutoTracer(appPackageName, atracerPackageName, atracerClassName, outputProtoPath):

   print "Starting ADB+NetCat Bridge @ 5050..."
   adb_proc = Popen(['adb','reverse','tcp:5050', 'tcp:5050'], stdout=PIPE)
   adb_proc.communicate()
   nc_proc = Popen(['nc','-l','-p','5050'], stdout=PIPE)
   print "Started ADB+NetCat Bridge"

   print "Running Auto Tracer on Instrumented App..."
   trace_proc = Popen(['adb','shell','am','instrument','-w','-r','-e','debug','false','-e','class','%s.%s' % (appPackageName,atracerClassName),'%s/android.support.test.runner.AndroidJUnitRunner' % atracerPackageName], stdout=PIPE)
   outcome,error = trace_proc.communicate()
   print "Trace Completed: %s, %s" % (outcome,error)

   trace,_ = nc_proc.communicate()

   with open("%s/%s.out" % (outputProtoPath,atracerClassName), "w") as f:
      f.write(trace)

if __name__ == "__main__":
   if len(sys.argv) != 5:
      print "usage: python autoLaunch.py <Path to (Resigned) Instrumented APK> <Path to (Resigned) Auto Tracer APK> <Auto Tracer Class Names (Colon Separated)> <Path to Output Folder>"
      sys.exit(1)

   instrumentedAPKPath = sys.argv[1]
   atracerAPKPath      = sys.argv[2]
   atracerClassNames   = sys.argv[3]
   outputProtoPath     = sys.argv[4]

   appPackageName,activityName = getAPKInfo(instrumentedAPKPath)
   atracerPackageName,_ = getAPKInfo(atracerAPKPath)

   print "Instrumented App Package Name: %s" % appPackageName
   print "Instrumented App Activity Name: %s" % activityName
   print "Auto Tracer Package Name: %s" % atracerPackageName

   print "Uninstalling Previous Version of Instrumented App..."
   adb_proc = Popen(['adb','uninstall',appPackageName], stdout=PIPE)
   outcome,_ = adb_proc.communicate()
   print "Uninstall Completed: %s" % outcome

   print "Uninstalling Previous Version of Auto Tracer..."
   adb_proc = Popen(['adb','uninstall',atracerPackageName], stdout=PIPE)
   outcome,_ = adb_proc.communicate()
   print "Uninstall Completed: %s" % outcome

   print "Installing Current Version of Instrumented App..."
   adb_proc = Popen(['adb','install',instrumentedAPKPath], stdout=PIPE)
   outcome,_ = adb_proc.communicate()
   print "Install Completed: %s" % outcome

   print "Installing Current Version of Auto Tracer..."
   adb_proc = Popen(['adb','install',atracerAPKPath], stdout=PIPE)
   outcome,_ = adb_proc.communicate()
   print "Install Completed: %s" % outcome

   for atracerClassName in atracerClassNames.split(":"):
       print "Running Auto Tracing for %s" % atracerClassName
       runAutoTracer(appPackageName, atracerPackageName, atracerClassName, outputProtoPath)

   print "All Done!"

