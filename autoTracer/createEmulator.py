import os
import sys
import subprocess
import time
import shutil

X86 = 1
ARM = 2

def createEmulator(avdName, avdConfig="avdConfig/config.ini", apiLevel='21', abiType=ARM):

    sdkHomePath = os.environ['ANDROID_SDK_HOME']

    print "Deleting existing avd " + avdName
    subprocess.call(['android', 'delete', 'avd', '-n', avdName])

    if abiType == X86:
        abiType = "google_apis/x86"
    else:
        abiType = "google_apis/armeabi-v7a"

    #create new android vm
    #android create avd --force -n gapi_64 -t 45 --abi google_apis/x86
    print "Creating avd " + avdName + " @ API " + apiLevel + " and ABI " + abiType
    subprocess.call(['android','create','avd','--force','-n',avdName, '-t', apiLevel, '--abi', abiType])

    #increase android vm ram
    print "Replacing default config.ini with %s" % avdConfig
    shutil.copyfile(avdConfig, sdkHomePath + '/.android/avd/' + avdName + '.avd/config.ini')

    print "Emulator Creation Done!"

if __name__ == "__main__":
   if len(sys.argv) != 4:
      print "usage: python createEmulator.py <Name of AVD> <Android API Level> <ABI: arm or x86>"
      sys.exit(1)

   if os.environ['ANDROID_SDK_HOME'] == "":
      print "Please set your \'ANDROID_SDK_HOME\' environment variable"
      sys.exit(1)

   avdName  = sys.argv[1]
   apiLevel = sys.argv[2]
   abiType  = sys.argv[3]
 
   if abiType in ['1','x86', 'X86']:
       abiType = X86
   else:
       abiType = ARM

   createEmulator(avdName, apiLevel=apiLevel, abiType=abiType)


