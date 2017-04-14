
import os
import sys
import time
import datetime
import random as r

from utils.genName import generateName

from subprocess import Popen, PIPE

from utils.getAPKInfo import getAPKInfo

from utils.fsUtils import Command

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

MONKEY_EVENT_DIST = { 'throttle'      : '300'
                    , 'pct-touch'     : '54' 
                    , 'pct-motion'    : '2'
                    , 'pct-trackball' : '2'
                    , 'pct-nav'       : '0'
                    , 'pct-majornav'  : '0'
                    , 'pct-syskeys'   : '0'
                    , 'pct-appswitch' : '40'
                    , 'pct-anyevent'  : '2' }

def getMonkeyEvents():
   events = []
   for key,val in MONKEY_EVENT_DIST.items():
      if val != None:
         events += ["--%s" % key,val]
   return events

def monkeySprint(appPackageName, numOfMonkeyEvents, monkeyLog):

   adb_monkey = ['adb','shell','monkey','-p',appPackageName] + getMonkeyEvents() + ['-v',str(numOfMonkeyEvents)]
   # trace_proc = Popen(adb_monkey, stdout=PIPE, stderr=PIPE)
   # outcome,error = trace_proc.communicate()
   monkey_fut = Command(adb_monkey).run(120)
   time.sleep(2)
   (outcome,error,timedout,_) = monkey_fut()

   print "%s Monkey Steps Completed: %s, %s" % (numOfMonkeyEvents,outcome,error)
   if monkeyLog != None:
      output = "\n%s Monkey Steps Completed: %s, %s\n" % (numOfMonkeyEvents,outcome,error)
      if timedout:
         output += "Monkey timed out! Aborting further tracing."
      with open(monkeyLog, "a") as f:
         f.write(output)
         f.flush()

   return timedout

def runAutoMonkey(appAPKName, appPackageName, activityName, outputProtoPath, index, numOfMonkeyEvents, numOfMonkeyTries, loggingPath):

   print "Starting ADB+NetCat Bridge @ 5050..."
   adb_proc = Popen(['adb','reverse','tcp:5050', 'tcp:5050'], stdout=PIPE)
   adb_proc.communicate()
   # nc_proc = Popen(['nc','-l','-p','5050'], stdout=PIPE)
   
   timeout = 600
   nc_fut = Command(['nc','-l','-p','5050']).run(timeout)
   print "Started ADB+NetCat Bridge"

   # reachMonkeyDropZone(appPackageName, activityName)

   print "Running Android Monkey on Instrumented App..."
   # adb shell monkey -p your.package.name -v 500

   if loggingPath != None:
      monkeyLog = generateName(loggingPath, prefix="monkey-run-%s" % appAPKName, postfix=".log")
   else:
      monkeyLog = None

   ranNumOfMonkeyTries = numOfMonkeyTries + r.randint(-2,2)
   if ranNumOfMonkeyTries < 1:
      ranNumOfMonkeyTries = 1
   print "%s Monkeys will jump on your Android" % ranNumOfMonkeyTries
   for i in range(0, ranNumOfMonkeyTries):
        ranNumOfMonkeyEvents = numOfMonkeyEvents + r.randint(-20,20)
        if ranNumOfMonkeyEvents < 1:
           ranNumOfMonkeyEvents = 10
        timedout = monkeySprint(appPackageName, ranNumOfMonkeyEvents, monkeyLog)
        if timedout:
           break
        if i < ranNumOfMonkeyTries - 1:
           wait = 2 ## + r.randint(0,10)
           print "Waiting %s seconds before restarting the monkey..." % wait
           time.sleep(wait)
   # events = str(numOfMonkeyEvents)
   # adb_monkey = ['adb','shell','monkey','-p',appPackageName] + getMonkeyEvents() + ['-v',events]
   # trace_proc = Popen(adb_monkey, stdout=PIPE)
   # outcome,error = trace_proc.communicate()
   # print "Trace Completed: %s, %s" % (outcome,error)

   wait = 4
   print "Waiting %s seconds before stopping app..." % wait
   time.sleep(wait)

   print "Stopping the Instrumented App"

   stop_proc = Popen(['adb','shell','am','force-stop',appPackageName], stdout=PIPE, stderr=PIPE)
   outcome,error = stop_proc.communicate()
   print "App Force-Stop Initiated: %s, %s" % (outcome,error)

   '''
   appswitch_proc = Popen(['adb','shell','input','keyevent','KEYCODE_APP_SWITCH'], stdout=PIPE, stderr=PIPE)
   outcome,error = appswitch_proc.communicate()
   print "App Switch: %s, %s" % (outcome,error)
   time.sleep(5)
   kill_proc = Popen(['adb','shell','input','tap','590','475'], stdout=PIPE, stderr=PIPE)
   outcome,error = kill_proc.communicate()
   print "App kill: %s, %s" % (outcome,error)
   '''

   wait = 2
   print "Waiting %s seconds before collecting trace..." % wait
   time.sleep(wait)

   # trace,_ = nc_proc.communicate()

   (trace,nc_stderr,timedout,_) = nc_fut()

   if timedout:
      output = "NetCat bridge timedout... no news from instrumenter for %s secs" % timeout
      print output
      if monkeyLog != None:
          with open(monkeyLog, "a") as f:
            f.write(output)
            f.flush()
      return

   wait = 2
   print "Waiting %s seconds before writing trace..." % wait
   time.sleep(wait)

   # with open("%s/%s" % (outputProtoPath,"trace%s" % index), "w") as f:
   protoTraceFile = generateTraceName(outputProtoPath, prefix="trace-%s" % appAPKName)
   with open(protoTraceFile, "w") as f:
      f.write(trace)
      f.flush()

   if monkeyLog != None:
      output = "\nOutput trace written to: %s\n" % protoTraceFile
      with open(monkeyLog, "a") as f:
         f.write(output)
         f.flush()

   '''
   print "Clearing the Instrumented App from task list"
   clear_app = Popen(['adb','shell','pm','clear',appPackageName], stdout=PIPE, stderr=PIPE)
   outcome,error = clear_app.communicate()
   print "App Clear Initiated: %s, %s" % (outcome,error)
   '''

def generateTraceName(outputProtoPath, prefix="trace"):
   ts = time.time()
   st = datetime.datetime.fromtimestamp(ts).strftime('%Y-%m-%d_%H:%M:%S')
   traceName = "%s/%s_%s" % (outputProtoPath,prefix,st)
   index = 1
   while os.path.exists(traceName):
     traceName = "%s/%s_%s_%s" % (outputProtoPath,prefix,st,index)
     index += 1
   return traceName

def reachMonkeyDropZone(appPackageName,activityName):
      print "Starting App %s/%s" % (appPackageName,activityName)
      start_proc = Popen(['adb','shell','am','start','-n', "%s/%s" % (appPackageName,activityName)], stdout=PIPE, stderr=PIPE)
      outcome,stderr = start_proc.communicate()
      print "App started: %s \n %s" % (outcome,stderr)

      wait = 20
      print "Sleeping for %s secs..." % wait
      time.sleep(wait)

      print "Tap Menu button"
      hit_proc = Popen(['adb','shell','input','tap','600', "150"], stdout=PIPE, stderr=PIPE)
      outcome,stderr = hit_proc.communicate()
      print "Tapped: %s \n %s" % (outcome,stderr)

      wait = 10
      print "Sleeping for %s secs..." % wait
      time.sleep(wait)

      print "Tap Settings button"
      hit_proc = Popen(['adb','shell','input','tap','600', "150"], stdout=PIPE, stderr=PIPE)
      outcome,stderr = hit_proc.communicate()
      print "Tapped: %s \n %s" % (outcome,stderr)

      wait = 15
      print "Sleeping for %s secs..." % wait
      time.sleep(wait)

def autoMonkey(instrumentedAPKPath, outputProtoPath, numOfTraces, numOfMonkeyEvents, numOfMonkeyTries, installApp=True, permissions=[], loggingPath=None):

   appPackageName,activityName = getAPKInfo(instrumentedAPKPath)
   print "Instrumented App Package Name: %s" % appPackageName

   if appPackageName == None:
      if loggingPath != None:
         monkeyLog = generateName(loggingPath, prefix="monkey-run-%s" % appAPKName, postfix=".log")
         output = "Unable to retrieve package name.. monkey tracing aborted."
         print output
         with open(monkeyLog, "a") as f:
            f.write(output)
            f.flush()
         return

   appAPKName = instrumentedAPKPath.split('/')[-1][:-4]

   if installApp:
      print "Uninstalling Previous Version of Instrumented App..."
      adb_proc = Popen(['adb','uninstall',appPackageName], stdout=PIPE, stderr=PIPE)
      outcome,_ = adb_proc.communicate()
      print "Uninstall Completed: %s" % outcome

      print "Installing Current Version of Instrumented App..."
      adb_proc = Popen(['adb','install',instrumentedAPKPath], stdout=PIPE, stderr=PIPE)
      outcome,_ = adb_proc.communicate()
      print "Install Completed: %s" % outcome
   else:
      print "App installation omitted. Assuming that app already exist on the device..."

   for index in range(0,int(numOfTraces)):

      print "Granting permissions..."
      for permission in permissions:
         perm_proc = Popen(['adb','shell','pm','grant',appPackageName,permission], stdout=PIPE, stderr=PIPE)
         outcome,err = perm_proc.communicate()
         print "Request permission %s: \n %s \n %s" % (permission,outcome,err)

      runAutoMonkey(appAPKName, appPackageName, activityName, outputProtoPath, index, int(numOfMonkeyEvents), int(numOfMonkeyTries), loggingPath=loggingPath)
      if index < int(numOfTraces) - 1:
         wait = 1
         print "Waiting %s seconds before next trace ..." % wait
         time.sleep(wait)

   print "All Done!"

if __name__ == "__main__":
   if len(sys.argv) != 6:
      print "usage: python autoMonkey.py <Path to (Resigned) Instrumented APK> <Path to Output Folder> <Num of Traces> <Num of Monkey Events> <Num of Monkey Tries>"
      sys.exit(1)

   instrumentedAPKPath = sys.argv[1]
   outputProtoPath     = sys.argv[2]
   numOfTraces         = int(sys.argv[3])
   numOfMonkeyEvents   = sys.argv[4]
   numOfMonkeyTries    = sys.argv[5]

   autoMonkey(instrumentedAPKPath, outputProtoPath, numOfTraces, numOfMonkeyEvents, numOfMonkeyTries)


