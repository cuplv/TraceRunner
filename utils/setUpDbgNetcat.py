import os
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
res = subprocess.call(['adb', 'shell', 'am', 'start', '-D', '-n', package + '/.' + appname])
if  0 != res:
	raise Exception("starting failed")

#get pid
print "***Get PID***"
res = subprocess.Popen(['adb', 'jdwp'],stdout=subprocess.PIPE)

pid = res.stdout.readline()
res.terminate() #TODO: this will probably fail if two debuggable processes are running
pid = int(pid)

print pid


#use netcat to open bridge
#print "***Open Bridge***"
#res = subprocess.call(['/bin/bash', 'nc', '-l', '7778' ,

#open bridge
print "***Open Bridge***"
#res = subprocess.call(['adb', 'forward', 'tcp:7778', 'jdwp:' + str(pid)])
#if 0 != res:
#	raise Exception("bridge failed")




#print dir(VirtualMachine)
#vm = VirtualMachine.connect("8000")
#print vm





