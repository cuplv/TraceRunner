import os
import time
import sys
import subprocess

#from com.sun.tools.attach import VirtualMachine


if len(sys.argv) != 4:
	raise Exception("usage: [apk path] [package] [appname]")
apk_path = sys.argv[1]
package = sys.argv[2]
appname = sys.argv[3]

#uninstall old apk
print "***Uninstalling old version***"
res = subprocess.call(['adb', 'uninstall', package])

#install new apk
print "***Install new APK***"
res = subprocess.call(['adb', 'install', apk_path])
if 0 != res:
	raise Exception("install failed")

#run apk
print "***Run APK***"
res = subprocess.call(['adb', 'shell', 'am', 'start', '-D', '-n', package + '/' + appname])
if  0 != res:
	raise Exception("starting failed")

#get pid
print "***Get PID***"
#res = subprocess.Popen(['adb', 'jdwp'],stdout=subprocess.PIPE)
res = subprocess.Popen(['adb', 'shell', 'ps'],stdout=subprocess.PIPE)
res.wait()

lines = res.stdout.readlines()
#print "-------------"
#for line in lines:
	#print line
#print "-------------"
psline = ""
for line in lines:
	if package in line:
		psline = line
		break
splitline = line.split(' ')
print splitline
#pid = res.stdout.readline()
for s in splitline:
	try:
		pid = int(s)
		break
	except ValueError:
		pass

print pid

#open bridge
print "***Open Bridge***"
res = subprocess.call(['adb', 'forward', 'tcp:7778', 'jdwp:' + str(pid)])
if 0 != res:
	raise Exception("bridge failed")





