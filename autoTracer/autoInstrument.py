
import os
import sys
from subprocess import Popen, PIPE
from shutil import copyfile

if __name__ == "__main__":
   if len(sys.argv) != 5:
      print "usage: python autoInstrument.py <Path to App APK> <Path to Robotium Tester APK> <Path to Output> <Path to Android Jars>"
      sys.exit(1)

   appAPKPath   = sys.argv[1]
   roboAPKPath  = sys.argv[2]
   outputPath   = sys.argv[3]
   andrJarsPath = sys.argv[4]

   appOutPath   = outputPath + "/" + appAPKPath.split('/')[-1]
   roboOutPath  = outputPath + "/" + roboAPKPath.split('/')[-1]

   print "App APK to Instrument: %s" % appAPKPath
   print "Robotium Tester APK: %s" % roboAPKPath
   print "Android Jars: %s" % andrJarsPath
   print "Outputs: "
   print "   %s" % appOutPath
   print "   %s" % roboOutPath

   print "Instrumenting and Resigning App APK: %s" % appAPKPath
   instProc = Popen(['bash', 'instrument.sh', appAPKPath, outputPath, andrJarsPath], stdout=PIPE)
   outcome,errors = instProc.communicate()
   print "Instrumentation and Resigning completed: %s" % outcome
   
   print "Copying Robotium Tester APK to %s" % roboOutPath
   copyfile(roboAPKPath, roboOutPath)
   print "Copy done!"

   print "Resigning Robotium Tester APK: %s" % roboOutPath
   roboProc = Popen(['bash', 'resign.sh', roboOutPath], stdout=PIPE)   
   outcome,errors = roboProc.communicate()
   print "Resigning completed: %s" % outcome

   print "Outputs written:"
   print "   %s" % appOutPath
   print "   %s" % roboOutPath

   print "All Done!"
