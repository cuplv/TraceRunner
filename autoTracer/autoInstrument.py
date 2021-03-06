
import os
import sys
from subprocess import Popen, PIPE
from shutil import copyfile

from utils.genName import generateName

def autoInstrument(appAPKPath, roboAPKPath, outputPath, andrJarsPath, oneJar=None, blackList=[], loggingPath=None):

   appOutPath   = outputPath + "/" + appAPKPath.split('/')[-1]
   roboOutPath  = outputPath + "/" + roboAPKPath.split('/')[-1]

   print "App APK to Instrument: %s" % appAPKPath
   print "Robotium Tester APK: %s" % roboAPKPath
   print "Android Jars: %s" % andrJarsPath
   print "Outputs: "
   print "   %s" % appOutPath
   print "   %s" % roboOutPath

   appAPKName = appAPKPath.split('/')[-1][:-4]

   print "Instrumenting and Resigning App APK: %s" % appAPKPath
   if len(blackList) > 0:
      blackListParam = [':'.join(blackList)]
      print "Instrumentation blacklist provided: %s" % (':'.join(blackList))
   else:
      blackListParam = []
      print "No instrumentation blacklist provided"
   if not oneJar:
      print "Running with sbt..."
      instProc = Popen(['bash', 'instrument.sh', appAPKPath, outputPath, andrJarsPath] + blackListParam
                      ,stdout=PIPE, stderr=PIPE)
   else:
      print "Running with oneJar..."
      instProc = Popen(['bash', 'instrumentOneJar.sh', appAPKPath, outputPath, andrJarsPath] + blackListParam
                       ,stdout=PIPE, stderr=PIPE)

   outcome,errors = instProc.communicate()
   print "Instrumentation and Resigning completed: %s" % outcome
   if loggingPath != None:
      output = "######## STDOUT ########\n" + outcome + (("\n######## STDERR ########\n" + errors) if (errors != None) else "")
      with open(generateName(loggingPath, prefix='instrument-%s' % appAPKName,postfix=".log"), "w") as f:
         f.write(output)
         f.flush()   

   if os.path.exists( roboAPKPath ):
      print "Copying Robotium Tester APK to %s" % roboOutPath
      copyfile(roboAPKPath, roboOutPath)
      print "Copy done!"

      print "Resigning Robotium Tester APK: %s" % roboOutPath
      roboProc = Popen(['bash', 'resign.sh', roboOutPath], stdout=PIPE)   
      outcome,errors = roboProc.communicate()
      print "Resigning completed: %s" % outcome

   print "Outputs written:"
   print "   %s" % appOutPath
   if os.path.exists( roboAPKPath ):
      print "   %s" % roboOutPath

   print "Instrumentation and Resigning Done!"

   return True

if __name__ == "__main__":
   if len(sys.argv) != 5:
      print "usage: python autoInstrument.py <Path to App APK> <Path to Robotium Tester APK> <Path to Output> <Path to Android Jars>"
      sys.exit(1)

   appAPKPath   = sys.argv[1]
   roboAPKPath  = sys.argv[2]
   outputPath   = sys.argv[3]
   andrJarsPath = sys.argv[4]

   autoInstrument(appAPKPath, roboAPKPath, outputPath, andrJarsPath)


