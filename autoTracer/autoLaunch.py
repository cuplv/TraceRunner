
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

if __name__ == "__main__":
   if len(sys.argv) != 4:
      print "usage: python autoLaunch.py <Path to (Resigned) Instrumented APK> <Path to (Resigned) Auto Tracer APK> <Path to Output Trace ProtoBuf File>"
      sys.exit(1)

   instrumented_apk_path = sys.argv[1]
   auto_tracer_apk_path  = sys.argv[2]
   output_proto_path     = sys.argv[3]

   appName,activityName = getAPKInfo(instrumented_apk_path)
   tracerName,_ = getAPKInfo(auto_tracer_apk_path)

   print "Instrumented App Package Name: %s" % appName
   print "Instrumented App Activity Name: %s" % activityName
   print "Auto Tracer Name: %s" % tracerName

   print "Starting ADB+NetCat Bridge @ 5050..."
   adb_proc = Popen(['adb','reverse','tcp:5050', 'tcp:5050'], stdout=PIPE)
   adb_proc.communicate()
   nc_proc = Popen(['nc','-l','-p','5050'], stdout=PIPE)
   print "Started ADB+NetCat Bridge"

   print "Uninstalling Previous Version of Instrumented App..."
   adb_proc = Popen(['adb','uninstall',appName], stdout=PIPE)
   outcome,_ = adb_proc.communicate()
   print "Uninstall Completed: %s" % outcome

   print "Uninstalling Previous Version of Auto Tracer..."
   adb_proc = Popen(['adb','uninstall',tracerName], stdout=PIPE)
   outcome,_ = adb_proc.communicate()
   print "Uninstall Completed: %s" % outcome

   print "Installing Current Version of Instrumented App..."
   adb_proc = Popen(['adb','install',instrumented_apk_path], stdout=PIPE)
   outcome,_ = adb_proc.communicate()
   print "Install Completed: %s" % outcome

   print "Installing Current Version of Auto Tracer..."
   adb_proc = Popen(['adb','install',auto_tracer_apk_path], stdout=PIPE)
   outcome,_ = adb_proc.communicate()
   print "Install Completed: %s" % outcome


   print "Running Auto Tracer on Instrumented App..."
   trace_proc = Popen(['adb','shell','am','instrument','-w','-r','-e','debug','false','-e','class','plv.colorado.edu.testapp0.RobotTest','%s/android.support.test.runner.AndroidJUnitRunner' % tracerName], stdout=PIPE)
   outcome,error = trace_proc.communicate()
   print "Trace Completed: %s, %s" % (outcome,error)

   trace,_ = nc_proc.communicate()

   with open(output_proto_path, "w") as f:
      f.write(trace)

   print "All Done!"

