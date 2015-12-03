import os
import sys
import subprocess
import shutil
import parseManifest

#Note emulator should already be started and on home screen

###hardcoded parameters####
workingDir = '/home/ubuntu/working/'

if len(sys.argv) != 2:
	raise Exception("usage: [id from buildable.txt]")

######delete contents of working directory#####
folder = '/home/ubuntu/working/'
for the_file in os.listdir(folder):
	file_path = os.path.join(folder, the_file)
	try:
		if os.path.isfile(file_path):
			os.unlink(file_path)
		elif os.path.isdir(file_path):
			shutil.rmtree(file_path)
	except Exception, e:
		print e

#########Copy Android Project to Working Directory #######
truncPath = sys.argv[1]
sdir = '/'.join(truncPath.split('/')[0:-1])
fdir = "ftc@192.12.242.126:/media/data/git/" + sdir + '/*'
res = subprocess.call(['scp', '-r', fdir, workingDir])

###Build application###
os.chdir(workingDir)

res = subprocess.call(['chmod', "+x", 'gradlew'])
res = subprocess.call(['./gradlew', 'assembleDebug'])

###get application info###
(apk, pkg, mainAct) = parseManifest.getManifest('/home/ubuntu/working')
print "apk: " + apk
print "pkg: " + pkg
print "main activity: " + mainAct

cmd = ['python', '/home/ubuntu/scripts/startAndBridge.py', apk, pkg, mainAct]
print cmd
#subprocess.Popen(cmd, shell=True, stdin=None, stdout=None, stderr=None, close_fds=True)
traceLog = open('/home/ubuntu/tracelog.txt', 'w')
monkey = subprocess.Popen(['python','/home/ubuntu/scripts/monkey.py',pkg])
sbres = subprocess.call(cmd, stdout=traceLog)
print sbres
try:
	monkey.kill()
except OSError:
        pass #don't care if already killed

print "Done tracing app: " + sys.argv[1]
#TODO: sleep for reasonable amount of time then pulse android monkey
