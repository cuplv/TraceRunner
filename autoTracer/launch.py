
import os
import sys
from subprocess import Popen, PIPE

from utils.getAPKInfo import getAPKInfo

if __name__ == "__main__":
   if len(sys.argv) != 3:
      print "usage: python launch.py <Path to (Resigned) Instrumented APK> <Path to Output Trace ProtoBuf File>"
      sys.exit(1)

   instrumented_apk_path = sys.argv[1]
   output_proto_path     = sys.argv[2]

   appName,activityName = getAPKInfo(instrumented_apk_path)

   print "Instrumented App Package Name: %s" % appName
   print "Instrumented App Activity Name: %s" % activityName

   print "Starting ADB+NetCat Bridge @ 5050..."
   adb_proc = Popen(['adb','reverse','tcp:5050', 'tcp:5050'], stdout=PIPE)
   adb_proc.communicate()
   nc_proc = Popen(['nc','-l','-p','5050'], stdout=PIPE)
   print "Started ADB+NetCat Bridge"

   print "Uninstalling Previous Version of Instrumented App..."
   adb_proc = Popen(['adb','uninstall',appName], stdout=PIPE)
   outcome,_ = adb_proc.communicate()
   print "Uninstall Completed: %s" % outcome

   print "Installing Current Version of Instrumented App..."
   adb_proc = Popen(['adb','install',instrumented_apk_path], stdout=PIPE)
   outcome,_ = adb_proc.communicate()
   print "Install Completed: %s" % outcome

   print "Starting the Instrumented App..."
   run_proc = Popen(['adb', 'shell', 'am', 'start', '-n', appName + '/' + activityName], stdout=PIPE)
   outcome,_ = run_proc.communicate()
   print "Started the Instrumented App: %s" % outcome

   print "Collecting trace now! Please exit (gracefully) the application once you are done..."
   trace,_ = nc_proc.communicate()

   print "Trace collected. Writing trace to %s" % output_proto_path
   with open(output_proto_path, "w") as f:
      f.write(trace)

   print "All Done!"
