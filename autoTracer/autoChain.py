
import os
import sys

from ConfigParser import ConfigParser

from utils.fsUtils import createPathIfEmpty, recreatePath
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
    monkeytries    = get(conf, 'tracerOptions', 'monkeytries', default='10')

    configs = { 'startEmulator':startEmu, 'input':inputPath, 'instrument':instrumentPath, 'output':outputPath, 'androidJars':androidJarPath, 'usetracers':usetracers, 'monkeyevents':monkeyevents, 'monkeytries':monkeytries }

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
           traces = map(lambda s: s.strip(), conf.get(section, 'traces').split(','))
           appUsetracers = get(conf, section, 'usetracers', default=None)
           if appUsetracers == None:
              appUsetracers = usetracers
           else:
              appUsetracers = map(lambda s: s.strip(), appUsetracers.split(','))
           apps[appName] = { 'app':appAPK, 'tracer':tracerAPK, 'traces':traces, 'usetracers':appUsetracers } 
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

   recreatePath( configs['instrument'] )
   recreatePath( configs['output'] )

   # Run Auto Tracer for each test app listed in the conf file
   for appName in configs['apps']:
       print "Running Test Cases for App: %s" % appName
       
       appData = configs['apps'][appName]
       appAPK  = appData['app']
       tracerAPK = appData['tracer']
       traces = ':'.join( appData['traces'] )

       # Instrument the App APK and Resign both the App and Tracer APKs
       appInputPath    = configs['input'] + "/" + appName + "/" + appData['app']
       tracerInputPath = configs['input'] + "/" + appName + "/" + appData['tracer']
       instrumentPath  = configs['instrument'] + "/" + appName
       createPathIfEmpty( instrumentPath )
       autoInstrument(appInputPath, tracerInputPath, instrumentPath, configs['androidJars'])

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
           autoMonkey(appInstrPath, monkeyOutput, configs['monkeytries'], configs['monkeyevents'])

   # Kill emulator if it was started here
   if configs['startEmulator']:
       killEmulator(configs['port'])



        
