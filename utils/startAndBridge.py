#!/Users/s/anaconda/bin/python
import os
import time
import sys
import subprocess

#from com.sun.tools.attach import VirtualMachine


if len(sys.argv) != 4:
	print "run with jython -J-cp /Library/Java/JavaVirtualMachines/jdk1.8.0_45.jdk/Contents/Home/lib/tools.jar"
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


#get path of tracerunner jar
scriptPath = os.path.dirname(os.path.realpath(__file__))
if(scriptPath[-5:] != "utils"):
	raise Exception("launch from utils directory")
jarpath = scriptPath[0:-5] + "build/libs/TraceRunner-all-2.0.jar"

if not os.path.isfile(jarpath):
	raise Exception("Tracerunner jar not found")

filt = package.split(".")
filts = ".".join(filt[0:2]) + ".*"
print "Package filter: " + filts
res = subprocess.call(['java','-jar',jarpath, "7778", "/Users/s/Desktop/android_star_filter_data/" + package + appname + ".proto", filts, "android.*"])


#print dir(VirtualMachine)
#vm = VirtualMachine.connect("8000")
#print vm





