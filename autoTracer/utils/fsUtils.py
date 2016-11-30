
import os
import shutil

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
