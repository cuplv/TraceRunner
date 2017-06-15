
import os
import sys
import shutil
import random

from ConfigParser import ConfigParser

from utils.fsUtils import createPathIfEmpty, recreatePath, getFilesInPath, getDirsInPath
from startEmulator import startEmulator, killEmulator
from autoLaunch import autoLaunch
from autoInstrument import autoInstrument
from autoMonkey import autoMonkey
from multiprocessing import Process
import time


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
    if not inferRepos:
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

                    apps[appName] = { 'app':appAPK, 'tracer':tracerAPK, 'instrumented':instrumentedAPK, 'traces':traces, 'usetracers':appUsetracers, 'blacklist':blackList, 'installapp':installApp, 'permissions': permissions }
    else:
        # Infer repos from given input folder.
	print "Inferring repos"

        def recSearch(pdir):
            out = set()
            for dirpath, dirnames, filenames in os.walk(pdir):
                for file in filenames:
                    for dirname in dirnames:
                        out.union(recSearch(dirname))
                    if file.endswith(".apk"):
                        out.add(dirpath)
            return out

        # getDirsInPath( configs['input'] )
        repoDirs = recSearch(configs['input'])
        print "Retrieving from repos:\n%s" % ('\n'.join(repoDirs))
        for repoDir in repoDirs:
            files = getFilesInPath( repoDir )
            apks  = files
            pathsplit = repoDir.split('/')
            repoName = pathsplit[-3] + "_" + pathsplit[-2] + "_" + pathsplit[-1]
            count = 0
            for apk in filter(lambda f: f.endswith(".apk"), files):
                thisRepoName = repoName + "##%s" % count
                apps[thisRepoName] = { 'app': apk.split('/')[-1], 'tracer':'some-tracer.apk', 'instrumented':None, 'traces':[]
                   , 'usetracers':['monkey'], 'blacklist':[], 'installapp':True, 'permissions':[]
                   , 'repodir': repoDir }
                count += 1

    configs['apps'] = apps

    return configs


def trace_app_on_emulator(appName, configs):
    print "Running Test Cases for App: %s" % appName
    appData = configs['apps'][appName]
    appAPK = appData['app']
    tracerAPK = appData['tracer']
    traces = ':'.join(appData['traces'])
    appName = appName.split("##")[0]
    # Instrument the App APK and Resign both the App and Tracer APKs
    if appData['installapp']:
        appInputPath = appData['repodir'] + "/" + appData['app']
        dirExtension = appData['repodir'].split(configs['input'])[1]  # strip off prefix
        appData['dirExtension'] = dirExtension
        tracerInputPath = appData['repodir'] + "/" + appData['tracer']
        instrumentPath = configs['instrument'] + "/" + dirExtension
        loggingPath = configs['logs'] + "/" + dirExtension
        recreatePath(instrumentPath)
        createPathIfEmpty(loggingPath)
        if appData['instrumented'] == None:
            autoInstrument(appInputPath, tracerInputPath, instrumentPath, configs['androidJars']
                           , oneJar=configs['onejar'], blackList=appData['blacklist'], loggingPath=loggingPath)
        else:
            print "Instrumented APK provided... Omitting instrumentation"
            instrumentInputPath = configs['input'] + "/" + appName + "/" + appData['instrumented']
            shutil.copyfile(instrumentInputPath, instrumentPath + "/" + os.path.basename(instrumentInputPath))
    else:
        print "App installation omitted, will omit instrumentation too"

    # Launch auto tracer and write outputs to the output path
    appInstrPath = configs['instrument'] + "/" + appData['dirExtension'] + "/" + appData['app']
    tracerInstrPath = configs['instrument'] + "/" + appData['dirExtension'] + "/" + appData['tracer']
    output = configs['output'] + "/" + appData['dirExtension']
    createPathIfEmpty(output)
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
            createPathIfEmpty(monkeyOutput)
            autoMonkey(appInstrPath, monkeyOutput, configs['monkeytraces'], configs['monkeyevents'],
                       configs['monkeytries']
                       , installApp=appData['installapp'], permissions=appData['permissions'], loggingPath=loggingPath)
        if os.path.exists(configs['input'] + "/" + appName + "/manualTraces"):
            manualTraceOutput = output + "/manualTraces"
            createPathIfEmpty(manualTraceOutput)
            for trace in getFilesInPath(configs['input'] + "/" + appName + "/manualTraces"):
                shutil.copyfile(trace, manualTraceOutput + "/" + os.path.basename(trace))


if __name__ == "__main__":
 
    if len(sys.argv) > 1:
        iniFilePath = sys.argv[1]
    else:
        iniFilePath = 'tracerConfig.ini'
   
    configs = getConfigs(iniFilePath)
 
    # recreatePath( configs['instrument'] )
    # recreatePath( configs['output'] )
 
    createPathIfEmpty( configs['instrument'] )
    createPathIfEmpty( configs['output'] )
    createPathIfEmpty( configs['logs'] )
 
    # Run Auto Tracer for each test app listed in the conf file
    apps_to_process = configs['apps'].keys()
    print "processing %i apps" % len(configs['apps'])
    random.shuffle(apps_to_process) # shuffle so if restarted we don't keep redoing the same apps
    for appName in apps_to_process:
        # Start emulator if requested
        if configs['startEmulator']:
            killEmulator(configs['port'])
            startEmulator(configs['name'], configs['sdpath'], devicePort=configs['port'], noWindow=configs['noWindow'])

        #trace_app_on_emulator(appName, configs)

        p = Process(target=trace_app_on_emulator, args=(appName,configs))
        p.start()
        time.sleep(10*60) #TODO: figure out if this is enough
        if p.is_alive():
            print "Terminating trace thread after timeout"
            p.terminate()
        p.join()

        # Kill emulator if it was started here
        if configs['startEmulator']:
            killEmulator(configs['port'])
