#!/Users/s/anaconda/bin/python
import os
import time
import sys
import subprocess

#from com.sun.tools.attach import VirtualMachine

print "Start And Bridge"
if len(sys.argv) != 5:
	raise Exception("usage: [apk path] [package] [appname] [commit hash]")
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
jarpath = scriptPath + "/TraceRunner-all-2.0.jar"

print "jarpath: " + jarpath

if not os.path.isfile(jarpath):
	raise Exception("Tracerunner jar not found")

filt = package.split(".")
filts = ".".join(filt[0:2]) + ".*"
print "Package filter: " + filts
uniqueID = ''.join(str(time.time()).split("."))

failureLog = open('/Users/shawn/Desktop/failureLog.txt','a')

protofile = "/Users/shawn/Desktop/traces/" + package + appname + uniqueID + "_" + sys.argv[4] + ".proto"
res = subprocess.call(['/Library/Java/JavaVirtualMachines/jdk1.7.0_71.jdk/Contents/Home/bin/java','-jar'
						  ,jarpath, "7778", protofile, filts, "android.*"])
if res == -1:
	failureLog.write(package + appname + uniqueID)
	failureLog.close()
	exit(-1)


#print dir(VirtualMachine)
#vm = VirtualMachine.connect("8000")
#print vm





