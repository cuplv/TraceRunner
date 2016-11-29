
import os
import sys

from ConfigParser import ConfigParser

from utils.fsUtils import createPathIfEmpty
from startEmulator import startEmulator, killEmulator
from autoLaunch import autoLaunch


def get(conf, section, option, default=None):
     if conf.has_option(section, option):
         return conf.get(section, option)
     else:
         return default

def getConfigs(iniFilePath='tracerConfig.ini'):
    conf = ConfigParser()
    conf.read(iniFilePath)

    startEmu = get(conf, 'tracerOptions', 'startemulator', default=True)
    if startEmu in ['false', 'False', 'No', 'no']:
        startEmu = False
    else: 
        startEmu = True

    output = get(conf, 'tracerOptions', 'outputpath', default='output')

    configs = { 'startEmulator': startEmu, 'output': output }

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
           appAPK  = conf.get(section, 'app')
           tracerAPK = conf.get(section, 'tracer')
           traces = map(lambda s: s.strip(), conf.get(section, 'traces').split(','))

           apps[appName] = { 'app':appAPK, 'tracer':tracerAPK, 'traces':traces } 
    configs['apps'] = apps

    return configs

if __name__ == "__main__":

   if len(sys.argv) > 1:
       iniFilePath = sys.argv[1]
   else:
       iniFilePath = 'tracerConfig.ini'
  
   configs = getConfigs(iniFilePath)

   if configs['startEmulator']:
       startEmulator(configs['name'], configs['sdpath'], devicePort=configs['port'], noWindow=configs['noWindow'])

   createPathIfEmpty( configs['output'] )

   for appName in configs['apps']:
       print "Running Test Cases for App: %s" % appName
       
       appData = configs['apps'][appName]
       appAPK  = appData['app']
       tracerAPK = appData['tracer']
       traces = ':'.join( appData['traces'] )

       output = configs['output'] + "/" + appName
       createPathIfEmpty( output )
       autoLaunch(appAPK, tracerAPK, traces, output)
  
   if configs['startEmulator']:
       killEmulator(configs['port'])



        
