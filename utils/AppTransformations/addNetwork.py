import subprocess
import os
import sys

apk = sys.argv[1]
script_path= os.path.realpath(__file__)
script_dir = os.path.sep.join(script_path.split(os.path.sep)[:-1])

runjar = script_dir + os.path.sep + "apktool_2.2.2.jar"
path_ = ["/usr/bin/java", "-jar", runjar, "d", apk, "-s", "-o", script_path]
print path_
subprocess.Popen(path_)