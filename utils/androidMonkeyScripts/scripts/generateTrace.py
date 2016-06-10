import os
import sys
import subprocess
import shutil
import parseManifest
import signal

# Note emulator should already be started and on home screen

###hardcoded parameters####
workingDir = '/Users/shawn/working/'
if __name__ == '__main__':
	if 2 > len(sys.argv) > 3:
		raise Exception("usage: [id from buildable.txt] [optional: commit hash]")
	truncPath = sys.argv[1]
	if len(sys.argv) == 3:
		irhash = sys.argv[2]
	else:
		irhash = None


def generate_trace(truncPath, irhash):
	######delete contents of working directory#####
	folder = workingDir
	# TODO: uncomment code to clear working dir
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
	# TODO: change this over to checking out from github?
	os.chdir(workingDir)

	split = truncPath.split('/')
	sdir = '/'.join(split[0:-1])
	os.mkdir("./" + split[0])
	os.chdir(split[0])
	# fdir = "ftc@192.12.242.126:/media/data/git/" + sdir + '/*'
	fdir = "http://github.com/" + sdir
	# TODO: uncomment code to download new app
	res = subprocess.call(['git', 'clone', fdir])
	os.chdir(split[1])
	rhash = ""
	if irhash != None:
		rhash = irhash
		res = subprocess.call(['git', 'checkout', rhash])
	else:
		print os.getcwd()
		rhash = subprocess.check_output(['git', 'rev-parse', 'HEAD'])

	rhash = rhash.strip()
	###Build application###
	# os.chdir(workingDir)
	res = subprocess.call(['chmod', "+x", 'gradlew'])
	res = subprocess.call(['./gradlew', 'assembleDebug'])
	###get application info###
	(apk, pkg, mainAct) = parseManifest.getManifest(workingDir + split[0] + '/' + split[1])
	print "apk: " + apk
	print "pkg: " + pkg
	print "main activity: " + mainAct
	scriptpath = os.path.dirname(os.path.realpath(__file__))
	cmd = ['python', scriptpath + '/startAndBridge.py', apk, pkg, mainAct, rhash]
	print cmd
	# subprocess.Popen(cmd, shell=True, stdin=None, stdout=None, stderr=None, close_fds=True)
	# traceLog = open('/home/ubuntu/tracelog.txt', 'w')
	# monkey = subprocess.Popen(['python', scriptpath + '/monkey.py', pkg])
	import monkey

	pid = os.fork()
	if pid == 0:
		monkey.monkey(pkg)
		exit()

	sbres = subprocess.call(cmd)
	# print sbres
	try:

		os.kill(pid, signal.SIGTERM)
		print "----------------monkey is dead---------------------------"
	except OSError:
		pass  # don't care if already killed
	print "Done tracing app: " + truncPath

# TODO: sleep for reasonable amount of time then pulse android monkey


generate_trace(truncPath, irhash)
