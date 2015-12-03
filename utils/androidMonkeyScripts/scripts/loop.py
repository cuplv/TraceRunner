import os
import sys
import subprocess
import time
import shutil
f = open('/home/ubuntu/toTrace.txt','r')
lines = f.readlines()

print lines
line = lines[0]

for line in lines:
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
	except OSError:
		pass
	#create new sdcard image
	subprocess.call(['mksdcard', '-l', 'e', '512M', '/home/ubuntu/emulator/sdcard.img'])
	
	
	emulatorPID = subprocess.Popen(['/home/ubuntu/android-sdk-linux/tools/emulator', '-avd', 'androidvm', '-no-audio', '-no-window', '-sdcard', '/home/ubuntu/emulator/sdcard.img'])
	
	#emulatorPID = emulatorPID.pid
	
	time.sleep(130) #it takes about 2 minutes for the emulator to come up on muse_2_16
	
	
	subprocess.call(['adb', 'shell', 'input', 'keyevent', '82'])
	subprocess.call(['adb', 'shell', 'input', 'keyevent', '3'])
	print "emulator started: " + str(emulatorPID)
	print "running on application: " + line
	subprocess.call(['python', '/home/ubuntu/scripts/generateTrace.py', line])
	
	
	#subprocess.call(['sudo','kill', str(emulatorPID)])
	
	emulatorPID.kill()


