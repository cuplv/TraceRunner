
import os
import sys
import shutil

from ConfigParser import ConfigParser

from utils.fsUtils import createPathIfEmpty, recreatePath, getFilesInPath
from startEmulator import startEmulator, killEmulator
from autoLaunch import autoLaunch
from autoInstrument import autoInstrument
from autoMonkey import autoMonkey

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

    configs = { 'startEmulator':startEmu, 'input':inputPath, 'instrument':instrumentPath, 'output':outputPath, 'androidJars':androidJarPath, 'usetracers':usetracers, 'monkeyevents':monkeyevents, 'monkeytraces':monkeytraces, 'monkeytries':monkeytries, 'onejar':onejar }

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

           apps[appName] = { 'app':appAPK, 'tracer':tracerAPK, 'instrumented':instrumentedAPK, 'traces':traces
                           , 'usetracers':appUsetracers, 'blacklist':blackList, 'installapp':installApp } 
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

   # Run Auto Tracer for each test app listed in the conf file
   for appName in configs['apps']:
       print "Running Test Cases for App: %s" % appName
       
       appData = configs['apps'][appName]
       appAPK  = appData['app']
       tracerAPK = appData['tracer']
       traces = ':'.join( appData['traces'] )

       # Instrument the App APK and Resign both the App and Tracer APKs
       if appData['installapp']:
          appInputPath    = configs['input'] + "/" + appName + "/" + appData['app']
          tracerInputPath = configs['input'] + "/" + appName + "/" + appData['tracer']
          instrumentPath  = configs['instrument'] + "/" + appName
          recreatePath( instrumentPath )
          if appData['instrumented'] == None:
             autoInstrument(appInputPath, tracerInputPath, instrumentPath, configs['androidJars']
                           ,oneJar=configs['onejar'], blackList=appData['blacklist'])
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
       if 'robot' in usetracers:
           print "Running Robotium Tracer..."
           autoLaunch(appInstrPath, tracerInstrPath, traces, output)
       if 'monkey' in usetracers:  
           print "Running Monkey Tracer..."
           monkeyOutput = output + "/" + "monkeyTraces"
           createPathIfEmpty( monkeyOutput )
           autoMonkey(appInstrPath, monkeyOutput, configs['monkeytraces'], configs['monkeyevents'], configs['monkeytries'], installApp=appData['installapp'])
       if os.path.exists( configs['input'] + "/" + appName + "/manualTraces" ):
           manualTraceOutput = output + "/manualTraces"
           createPathIfEmpty( manualTraceOutput )
           for trace in getFilesInPath(configs['input'] + "/" + appName + "/manualTraces"):
               shutil.copyfile(trace, manualTraceOutput + "/" + os.path.basename(trace))

   # Kill emulator if it was started here
   if configs['startEmulator']:
       killEmulator(configs['port'])



        
