import os
import sys
import subprocess
import time
import shutil
f = open('/home/ubuntu/toTrace.txt','r')
lines = f.readlines()

print lines
#delete old vm
subprocess.call(['android', 'delete', 'avd', '-n', 'androidvm'])

#create new android vm
#android create avd --force -n gapi_64 -t 45 --abi google_apis/x86
subprocess.call(['android','create','avd','--force','-n','androidvm', '-t', '45', '--abi','google_apis/x86'])

#increase android vm ram
shutil.copyfile('/home/ubuntu/scripts/config.ini','/home/ubuntu/.android/avd/androidvm.avd/config.ini')

#delete old sdcard
try:
	os.remove('/home/ubuntu/emulator/sdcard.img')
	os.remove('/home/ubuntu/emulator/sdcard.img.lock')
except OSError:
	pass
#create new sdcard image
subprocess.call(['mksdcard', '-l', 'e', '512M', '/home/ubuntu/emulator/sdcard.img'])


emulatorPID = subprocess.Popen(['/home/ubuntu/android-sdk-linux/tools/emulator', '-avd', 'androidvm', '-no-audio', '-no-window', '-sdcard', '/home/ubuntu/emulator/sdcard.img'])

#emulatorPID = emulatorPID.pid

#wait for shell
subprocess.call(['adb', 'wait-for-device'])

#wait for sdcard to mount
while(True):
	res = subprocess.Popen(['bash', '-c', 'adb shell mount |grep sdcard |wc -l'], stdout=subprocess.PIPE)
	(out,something) = res.communicate()
	outi = int(out)
	print "sdcard: " + out
	if outi > 1:
		break
	time.sleep(10)
#wait for boot animation to go away
while(True):
	res = subprocess.Popen(['bash', '-c', 'adb shell ps |grep bootanimation |wc -l'], stdout=subprocess.PIPE)
	(out,something) = res.communicate()
	outi = int(out)
	print "bootanimation: " + out
	if outi == 0:
		break
	time.sleep(10)

time.sleep(20)

subprocess.call(['adb', 'shell', 'input', 'keyevent', '82'])
subprocess.call(['adb', 'shell', 'input', 'keyevent', '3'])
print "emulator started: " + str(emulatorPID)
