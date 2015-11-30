import os
import sys
import subprocess

pid = int(sys.argv[1])


print pid

#open bridge
print "***Open Bridge***"
res = subprocess.call(['adb', 'forward', 'tcp:7778', 'jdwp:' + str(pid)])
if 0 != res:
	raise Exception("bridge failed")




#print dir(VirtualMachine)
#vm = VirtualMachine.connect("8000")
#print vm





