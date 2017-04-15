
import os
import shutil

from subprocess import Popen, PIPE
from threading import Thread

from os.path import isfile, isdir, join

def createPathIfEmpty(path):
    if not os.path.exists(path):
       print "Creating directory: %s" % path
       os.makedirs(path)
    
def removePathIfExists(path):
    if os.path.exists(path):
       print "Deleting directory: %s" % path
       shutil.rmtree(path)

def recreatePath(path):
    removePathIfExists(path)
    createPathIfEmpty(path)


def getFilesInPath(path):
    return [join(path, f) for f in os.listdir(path) if isfile(join(path, f))]

def getDirsInPath(path):
    return [join(path, f) for f in os.listdir(path) if isdir(join(path, f))]

class Command(object):
    def __init__(self, cmd):
        self.cmd = cmd
        self.process = None
        self.stdout = None
        self.stderr = None

    def run(self, timeout):
        def target():
            print 'Thread started with timeout at %s seconds' % timeout
            self.process = Popen(self.cmd, stdout=PIPE, stderr=PIPE)
            (stdout,stderr) = self.process.communicate()
            self.stdout = stdout
            self.stderr = stderr
            print 'Thread finished'

        thread = Thread(target=target)
        thread.start()

        # process = self.process
        def Future():
	    thread.join(timeout)
            timedout = False
            if thread.is_alive():
               print 'Terminating process'
               try:
                  self.process.terminate()
               except e:
                  print e
               # thread.join()
               timedout = True
               self.stdout = (self.stdout + "\n *** Timedout ***") if self.stdout != None else "\n *** Timedout ***"   
            return (self.stdout, self.stderr, timedout, self.process.returncode)
 
        return Future

