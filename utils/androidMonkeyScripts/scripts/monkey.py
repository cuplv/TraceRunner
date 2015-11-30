import os
import sys
import subprocess
import time

if len(sys.argv) != 2:
	raise Exception("package needed")

pkg = sys.argv[1]
monkeyLog = open('/home/ubuntu/monkeylog.txt', 'w')
time.sleep(60)
#adb shell monkey -p com.flatsoft.base -v 500
for i in xrange(20):
	subprocess.call(['adb', 'shell', 'monkey', '-p', pkg, '-v', '50'], stdout=monkeyLog)
	time.sleep(30)



#/home/ubuntu/scripts/kill_app.sh

subprocess.call(['sh', '/home/ubuntu/scripts/kill_app.sh', pkg])
monkeyLog.close()
