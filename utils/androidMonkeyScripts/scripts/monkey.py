import os
import sys
import subprocess
import time

if len(sys.argv) != 2:
	raise Exception("package needed")

pkg = sys.argv[1]
monkeyLog = open('/Users/shawn/Desktop/monkeylog.txt', 'w')
time.sleep(180)
#adb shell monkey -p com.flatsoft.base -v 500
for i in xrange(3): #TODO: change this back to 50
	subprocess.call(['adb', 'shell', 'monkey', '-p', pkg,'--pct-touch', '70', '-v', '5'], stdout=monkeyLog)
	time.sleep(15)



#/home/ubuntu/scripts/kill_app.sh

print "App package killing: " + pkg
appsdir = os.path.dirname(os.path.realpath(__file__))
subprocess.call(['sh', appsdir + '/kill_app.sh', pkg])

monkeyLog.close()
