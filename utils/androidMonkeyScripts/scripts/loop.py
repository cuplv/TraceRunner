import os
import sys
import subprocess
import time
f = open('/home/ubuntu/toTrace.txt','r')
lines = f.readlines()
print lines
exit()
#delete old vm
subprocess.call(['android', 'delete', 'avd', '-n', 'androidvm'])

#create new android vm
#android create avd --force -n gapi_64 -t 45 --abi google_apis/x86
#subprocess.call(['android','create','avd','--force','-n','androidvm',

#sudo /home/ubuntu/android-sdk-linux/tools/emulator -avd gapi_64 -no-audio -no-window -sdcard /home/ubuntu/emulator/sdcard.img

#subprocess.call(['sudo','/home/ubuntu/android-sdk-linux/tools/emulator', '-avd', 
