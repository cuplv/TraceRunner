
import os
import sys
import shutil
import time

from ConfigParser import ConfigParser

from utils.fsUtils import createPathIfEmpty, recreatePath, getFilesInPath, getDirsInPath
from startEmulator import startEmulator, killEmulator
from autoLaunch import autoLaunch
from autoInstrument import autoInstrument
from autoMonkey import autoMonkey


import redisJobQueue.ops as rops

from retrieveBuilds import retrieveBuildData, latestCommit, copyBuildData


# adb shell input tap 600 150

def get(conf, section, option, default=None):
     if conf.has_option(section, option):
         return conf.get(section, option)
     else:
         return default

# Extract configurations from config file.
def getConfigs(iniFilePath='tracerConfig.ini'):
    conf = ConfigParser()
    conf.read(iniFilePath)

    startEmu = get(conf, 'tracerOptions', 'startemulator', default=True)
    if startEmu in ['false', 'False', 'No', 'no']:
        startEmu = False
    else: 
        startEmu = True

    inputPath      = get(conf, 'tracerOptions', 'input', default='/traceRunner/input')
    instrumentPath = get(conf, 'tracerOptions', 'instrument', default='/traceRunner/instrument')
    outputPath     = get(conf, 'tracerOptions', 'output', default='/traceRunner/output')
    logPath        = get(conf, 'tracerOptions', 'logs', default='/traceRunner/logs')
    androidJarPath = get(conf, 'tracerOptions', 'androidjars', default='/usr/local/android-sdk/platforms')
    usetracers     = map(lambda s: s.strip(), get(conf, 'tracerOptions', 'usetracers', default='monkey,robot').split(','))
    monkeyevents   = get(conf, 'tracerOptions', 'monkeyevents', default='40')
    monkeytraces   = get(conf, 'tracerOptions', 'monkeytraces', default='10')
    monkeytries    = get(conf, 'tracerOptions', 'monkeytries', default='5')
    onejar         = get(conf, 'tracerOptions', 'onejarinstrument', default=False)
    if onejar in ['true', 'True', 'Yes', 'yes']:
       onejar = True
    else:
       onejar = False
    permissions = map(lambda s: s.strip(), get(conf, 'tracerOptions', 'permissions', default='').split(','))
    permissions = filter(lambda p: p != '', permissions)
    inferRepos  = get(conf, 'tracerOptions', 'inferrepos', default=False)
    if inferRepos in ['true', 'True', 'Yes', 'yes']:
       inferRepos = True
    else:
       inferRepos = False
    fromRedis = get(conf, 'tracerOptions', 'fromredis', default=False)
    if fromRedis in ['true', 'True', 'Yes', 'yes']:
       fromRedis = True
    else:
       fromRedis = False

    redisHost = get(conf, 'tracerOptions', 'redishost', default='0.0.0.0')
    redisPort = int(get(conf, 'tracerOptions', 'redisport', default=6379))
    redisPass = get(conf, 'tracerOptions', 'redispass', default='phoenix-has-died')
    redisJobs = get(conf, 'tracerOptions', 'redisjobs', default='callback-jobs')

    configs = { 'startEmulator':startEmu, 'input':inputPath, 'instrument':instrumentPath, 'output':outputPath, 'logs':logPath, 'androidJars':androidJarPath, 
                'usetracers':usetracers, 'monkeyevents':monkeyevents, 'monkeytraces':monkeytraces, 'monkeytries':monkeytries, 'onejar':onejar, 'permissions':permissions }

    if startEmu:
        emuSect = 'emulatorOptions'

        deviceName  = get(conf, emuSect, 'name', default='androidavd')
        devicePort  = get(conf, emuSect, 'port', default=None)
        sdImagePath = get(conf, emuSect, 'sdpath', default='emulator')
        display     = get(conf, emuSect, 'display', default=False)
        if display in ['true', 'True', 'Yes', 'yes']:
           display = True
        else:
           display = False

        configs['name']   = deviceName
        configs['port']   = devicePort
        configs['sdpath'] = sdImagePath
        configs['noWindow'] = not display

    apps = {}
    if not inferRepos and not fromRedis:
       for section in conf.sections():
          if section.startswith("app:"):
              appName = section[4:]
              appAPK  = get(conf, section, 'app', default='app-debug.apk') 
              tracerAPK = get(conf, section, 'tracer', default='app-debug-androidTest-unaligned.apk')
              instrumentedAPK = get(conf, section, 'instrumented', default=None)
              installApp = get(conf, section, 'installapp', default=True)
              if installApp in ['False','false','No','no']:
                 installApp = False
              else:
                 installApp = True
              traces = map(lambda s: s.strip(), get(conf, section, 'traces', default='').split(','))
              appUsetracers = get(conf, section, 'usetracers', default=None)
              if appUsetracers == None:
                 appUsetracers = usetracers
              else:
                 appUsetracers = map(lambda s: s.strip(), appUsetracers.split(','))

              blackList = map(lambda s: s.strip(), get(conf, section, 'blacklist', default='').split(','))
              blackList = filter(lambda b: b != '', blackList)

              permissions = map(lambda s: s.strip(), get(conf, section, 'permissions', default='').split(','))
              permissions = filter(lambda p: p != '', permissions)
              permissions = list(set(configs['permissions'] + permissions))

              apps[appName] = { 'app':appAPK, 'tracer':tracerAPK, 'instrumented':instrumentedAPK, 'traces':traces
                              , 'usetracers':appUsetracers, 'blacklist':blackList, 'installapp':installApp, 'permissions': permissions } 
    elif not inferRepos and fromRedis:
       # redisHost, redisPort, redisPass

       redisConfig = { 'host':redisHost, 'port':redisPort, 'db':0
                     , 'jobs':redisJobs, 'name':'phoenix', 'pwd':redisPass
                     , 'started': "started-%s" % redisJobs, 'completed': "completed-%s" % redisJobs, 'noinfo': 'noinfo-%s' % redisJobs
                     , 'failed':'failed-%s' % redisJobs }

       configs['redis'] = redisConfig       

    else:
       # Infer repos from given input folder.
       repoDirs = getDirsInPath( configs['input'] )
       print "Retrieving from repos:\n%s" % ('\n'.join(repoDirs))
       for repoDir in repoDirs:
           files = getFilesInPath( repoDir )
           apks  = files
           repoName = repoDir.split('/')[-1]
           count = 0
           for apk in filter(lambda f: f.endswith(".apk"), files):
               thisRepoName = repoName + "##%s" % count
               apps[thisRepoName] = { 'app': apk.split('/')[-1], 'tracer':'some-tracer.apk', 'instrumented':None, 'traces':[]
                                    , 'usetracers':['monkey'], 'blacklist':[], 'installapp':True, 'permissions':[] }
               count += 1

    configs['apps'] = apps

    return configs

if __name__ == "__main__":

   if len(sys.argv) > 1:
       iniFilePath = sys.argv[1]
   else:
       iniFilePath = 'tracerConfig.ini'
  
   configs = getConfigs(iniFilePath)

   # Start emulator if requested
   if configs['startEmulator']:
       startEmulator(configs['name'], configs['sdpath'], devicePort=configs['port'], noWindow=configs['noWindow'])

   # recreatePath( configs['instrument'] )
   # recreatePath( configs['output'] )

   createPathIfEmpty( configs['instrument'] )
   createPathIfEmpty( configs['output'] )
   createPathIfEmpty( configs['logs'] )

   # Run Auto Tracer for each test app listed in the conf file
 
   redisConfig = configs['redis']

   appBuilderName = 'cuaws-app-builder'
   baseRemoteRepoPath = '/builder/data/staging1'

   job = rops.dequeue(redisConfig['jobs'], config=redisConfig)   
   while job != None:
      user = job['user']     
      repo = job['repo']

      rops.push_queue(redisConfig['started'], user, repo, hash_id=job['hash'], config=redisConfig)

      resp = None
      try:
         resp = retrieveBuildData(user,repo)
      except:
         print "Repo info not found."
         rops.push_queue(redisConfig['noinfo'], user, repo, hash_id=job['hash'], config=redisConfig)

      if resp != None:
          if len(resp['results']) == 0:
             print("No entries")
             rops.push_queue(redisConfig['noinfo'], user, repo, hash_id=job['hash'], config=redisConfig)
          else:
             commit = latestCommit(resp)
             _,appNames = copyBuildData(commit, configs['input'], appBuilderName, baseRemoteRepoPath)
             if len(appNames) == 0:
                rops.push_queue(redisConfig['noinfo'], user, repo, hash_id=job['hash'], config=redisConfig)
             else:
                for appName in appNames:
                    appRepo = configs['input'] + "/%s" % appName
                    files = getFilesInPath( appRepo )
                    # count = 0
                    for apk in filter(lambda f: f.endswith(".apk"), files)[:3]:
                        '''
                        thisRepoName = repoName + "##%s" % count
                        apps[thisRepoName] = { 'app': apk.split('/')[-1], 'tracer':'some-tracer.apk', 'instrumented':None, 'traces':[]
                                             , 'usetracers':['monkey'], 'blacklist':[], 'installapp':True, 'permissions':[] }
                        count += 1
                        '''
                        apkName = apk.split('/')[-1]
                        appInputPath   = configs['input'] + "/" + appName + "/" + apkName 
                        tracerInputPath = configs['input'] + "/" + appName + "/" + 'tracer-dummy.apk'
                        instrumentPath = configs['instrument'] + "/" + appName
                        loggingPath    = configs['logs'] + "/" + appName
                        outputPath     = configs['output'] + "/" + appName 
                        appInstrPath = configs['instrument'] + "/" + appName + "/" + apkName

                        recreatePath( instrumentPath )
                        createPathIfEmpty( loggingPath )

                        succ = autoInstrument(appInputPath, tracerInputPath, instrumentPath, configs['androidJars']
                                             ,oneJar=configs['onejar'], blackList=[], loggingPath=loggingPath)
                           
                        if succ:

                           createPathIfEmpty( outputPath )
 
                           print "Running Monkey Tracer..."
                           monkeyOutput = outputPath + "/" + "monkeyTraces"
                           createPathIfEmpty( monkeyOutput )
                           succ = autoMonkey(appInstrPath, monkeyOutput, configs['monkeytraces'], configs['monkeyevents'], configs['monkeytries']
                                            ,installApp=True, permissions=[], loggingPath=loggingPath)

                           if succ:
                              rops.push_queue(redisConfig['completed'], user, repo, hash_id=job['hash'], config=redisConfig)
                           else:
                              rops.push_queue(redisConfig['failed'], user, repo, hash_id=job['hash'], config=redisConfig)

                        else:
                           rops.push_queue(redisConfig['failed'], user, repo, hash_id=job['hash'], config=redisConfig)

      print "Waiting for next job. Press Ctrl^C within the next 3 seconds to quit..."      
      time.sleep(3)

      job = rops.dequeue(redisConfig['jobs'], config=redisConfig)   
 
   '''
   for appName in configs['apps']:

       print "Running Test Cases for App: %s" % appName
       
       appData = configs['apps'][appName]
       appAPK  = appData['app']
       tracerAPK = appData['tracer']
       traces = ':'.join( appData['traces'] )

       appName = appName.split("##")[0]

       # Instrument the App APK and Resign both the App and Tracer APKs
       if appData['installapp']:
          appInputPath    = configs['input'] + "/" + appName + "/" + appData['app']
          tracerInputPath = configs['input'] + "/" + appName + "/" + appData['tracer']
          instrumentPath  = configs['instrument'] + "/" + appName
          loggingPath     = configs['logs'] + "/" + appName
          recreatePath( instrumentPath )
          createPathIfEmpty( loggingPath )
          if appData['instrumented'] == None:
             autoInstrument(appInputPath, tracerInputPath, instrumentPath, configs['androidJars']
                           ,oneJar=configs['onejar'], blackList=appData['blacklist'], loggingPath=loggingPath)
          else:
             print "Instrumented APK provided... Omitting instrumentation"
             instrumentInputPath = configs['input'] + "/" + appName + "/" + appData['instrumented']
             shutil.copyfile(instrumentInputPath, instrumentPath + "/" + os.path.basename(instrumentInputPath))
       else:
          print "App installation omitted, will omit instrumentation too"

       # Launch auto tracer and write outputs to the output path
       appInstrPath = configs['instrument'] + "/" + appName + "/" + appData['app']
       tracerInstrPath = configs['instrument'] + "/" + appName + "/" + appData['tracer']
       output = configs['output'] + "/" + appName
       createPathIfEmpty( output )
       usetracers = appData['usetracers']

       # check if instrumented app path exists
       if not os.path.exists(appInstrPath):
           print "Instrumentation failed... aborting for this repo"
       else:
          if 'robot' in usetracers:
             print "Running Robotium Tracer..."
             autoLaunch(appInstrPath, tracerInstrPath, traces, output, permissions=appData['permissions'])
          if 'monkey' in usetracers:  
             print "Running Monkey Tracer..."
             monkeyOutput = output + "/" + "monkeyTraces"
             createPathIfEmpty( monkeyOutput )
             autoMonkey(appInstrPath, monkeyOutput, configs['monkeytraces'], configs['monkeyevents'], configs['monkeytries']
                       ,installApp=appData['installapp'], permissions=appData['permissions'], loggingPath=loggingPath)
          if os.path.exists( configs['input'] + "/" + appName + "/manualTraces" ):
             manualTraceOutput = output + "/manualTraces"
             createPathIfEmpty( manualTraceOutput )
             for trace in getFilesInPath(configs['input'] + "/" + appName + "/manualTraces"):
                 shutil.copyfile(trace, manualTraceOutput + "/" + os.path.basename(trace))
   '''

   # Kill emulator if it was started here
   if configs['startEmulator']:
       killEmulator(configs['port'])



        
