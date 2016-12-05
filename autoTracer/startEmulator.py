
import os
import sys
import subprocess
import time
import shutil

from utils.fsUtils import createPathIfEmpty

def startEmulator(deviceName, emulatorSDPath, devicePort=None, noWindow=True):
    createPathIfEmpty(emulatorSDPath)
    try:
        os.remove(emulatorSDPath + '/sdcard.img')
        os.remove(emulatorSDPath + '/sdcard.img.lock')
    except OSError:
        pass

    #create new sdcard image
    subprocess.call(['mksdcard', '-l', 'e', '512M', emulatorSDPath + '/sdcard.img'])
    sdParams = ['-sdcard', emulatorSDPath + '/sdcard.img']

    if devicePort != None:
       portParams = ['-port',devicePort, "@" + deviceName]
    else:
       portParams = ['-avd',deviceName]

    windowParams = []
    if noWindow:
       windowParams = ['-no-window']

    # emulatorPID = subprocess.Popen(['emulator', '-port', devicePort, "@" + deviceName, '-no-window', '-sdcard', emulatorSDPath + '/sdcard.img'])
    emulatorPID = subprocess.Popen(['emulator'] + portParams + windowParams + sdParams)

    #wait for shell
    subprocess.call(['adb', 'wait-for-device'])

    #wait for sdcard to mount
    
    '''
    while(True):
       res = subprocess.Popen(['bash', '-c', 'adb shell mount |grep sdcard |wc -l'], stdout=subprocess.PIPE)
       (out,something) = res.communicate()
       outi = int(out)
       print "waiting for SD card setup..." 
       if outi > 1:
          break
       time.sleep(10)
    '''

    #wait for boot animation to go away
    while(True):
       res = subprocess.Popen(['bash', '-c', 'adb shell ps |grep bootanimation |wc -l'], stdout=subprocess.PIPE)
       (out,something) = res.communicate()
       outi = int(out)
       print "waiting for boot animation to end..."
       if outi == 0:
          break
       time.sleep(10)

    time.sleep(20)

    subprocess.call(['adb', 'shell', 'input', 'keyevent', '82'])
    subprocess.call(['adb', 'shell', 'input', 'keyevent', '3'])

    print "Emulator-%s started and running" % devicePort

def killEmulator(devicePort=None):
    if devicePort == None:
       devicePort = '5554'
    print "Killing Emulator-%s" % devicePort
    proc = subprocess.Popen(['adb', '-s', 'emulator-' + devicePort, 'emu', 'kill'], stdout=subprocess.PIPE)
    outcome,error = proc.communicate()
    print "Kill emulator completed: %s" % outcome

if __name__ == "__main__":
   if len(sys.argv) != 4:
      print "usage: python startEmulator.py <Name of Device Image> <Device Port Number> <Path to Emulator SD Image>"
      sys.exit(1)

   deviceName = sys.argv[1]
   devicePort = sys.argv[2]
   emulatorSDPath = sys.argv[3]
    
   startEmulator(deviceName, emulatorSDPath, devicePort=devicePort, noWindow=False)


