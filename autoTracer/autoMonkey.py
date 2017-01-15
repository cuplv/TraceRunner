
import os
import sys
import time
from subprocess import Popen, PIPE

from utils.getAPKInfo import getAPKInfo

'''
// Event percentages:
//   0: 15.0%
//   1: 10.0%
//   2: 2.0%
//   3: 15.0%
//   4: -0.0%
//   5: -0.0%
//   6: 25.0%
//   7: 15.0%
//   8: 2.0%
//   9: 2.0%
//   10: 1.0%
//   11: 13.0%

0: 15.0% touch 1: 10.0% motion
2: 15.0% traceback 3: 25.0% syskeys 4: 15.0% nav 5: 2.0% majornav 6: 2.0% appswitch 7: 1.0% flip 8: 15.0% anyevent
'''

MONKEY_EVENT_DIST = { 'throttle'      : None
                    , 'pct-touch'     : '35' 
                    , 'pct-motion'    : '15'
                    , 'pct-trackball' : '4'
                    , 'pct-nav'       : '2'
                    , 'pct-majornav'  : '2'
                    , 'pct-syskeys'   : '2'
                    , 'pct-appswitch' : '30'
                    , 'pct-anyevent'  : '10' }

def getMonkeyEvents():
   events = []
   for key,val in MONKEY_EVENT_DIST.items():
      if val != None:
         events += ["--%s" % key,val]
   return events

def runAutoMonkey(appPackageName, outputProtoPath, index, numOfMonkeyEvents):

   print "Starting ADB+NetCat Bridge @ 5050..."
   adb_proc = Popen(['adb','reverse','tcp:5050', 'tcp:5050'], stdout=PIPE)
   adb_proc.communicate()
   nc_proc = Popen(['nc','-l','-p','5050'], stdout=PIPE)
   print "Started ADB+NetCat Bridge"


   print "Running Android Monkey on Instrumented App..."
   # adb shell monkey -p your.package.name -v 500
   events = str(numOfMonkeyEvents)
   adb_monkey = ['adb','shell','monkey','-p',appPackageName] + getMonkeyEvents() + ['-v',events]
   trace_proc = Popen(adb_monkey, stdout=PIPE)
   outcome,error = trace_proc.communicate()
   print "Trace Completed: %s, %s" % (outcome,error)

   wait = 2
   print "Waiting %s seconds before stopping app..." % wait
   time.sleep(wait)

   print "Stopping the Instrumented App"
   stop_proc = Popen(['adb','shell','am','force-stop',appPackageName], stdout=PIPE)
   outcome,error = stop_proc.communicate()
   print "App Force-Stop Initiated: %s, %s" % (outcome,error)

   trace,_ = nc_proc.communicate()

   with open("%s/%s.out" % (outputProtoPath,"trace%s" % index), "w") as f:
      f.write(trace)

def autoMonkey(instrumentedAPKPath, outputProtoPath, numOfTraces, numOfMonkeyEvents):

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

   for index in range(0,int(numOfTraces)):
      runAutoMonkey(appPackageName, outputProtoPath, index, numOfMonkeyEvents)
      if index < int(numOfTraces) - 1:
         wait = 3
         print "Waiting %s seconds before restart monkey ..." % wait
         time.sleep(wait)

   print "All Done!"

if __name__ == "__main__":
   if len(sys.argv) != 5:
      print "usage: python autoMonkey.py <Path to (Resigned) Instrumented APK> <Path to Output Folder> <Num of Traces> <Num of Monkey Events>"
      sys.exit(1)

   instrumentedAPKPath = sys.argv[1]
   outputProtoPath     = sys.argv[2]
   numOfTraces         = int(sys.argv[3])
   numOfMonkeyEvents   = sys.argv[4]

   autoMonkey(instrumentedAPKPath, outputProtoPath, numOfTraces, numOfMonkeyEvents)


