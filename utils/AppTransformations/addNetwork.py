import subprocess
import os
import sys
import xml.etree.ElementTree as ET

apk = sys.argv[1]
script_path= os.path.realpath(__file__)
script_dir = os.path.sep.join(script_path.split(os.path.sep)[:-1])

runjar = script_dir + os.path.sep + "apktool_2.2.2.jar"
decomptemp = script_dir + os.path.sep + "tmp"

path_ = ["/usr/bin/java", "-jar", runjar, "d", apk, "-s", "-o", decomptemp]
print path_
p = subprocess.Popen(path_)
p.communicate()

manifest = ET.parse(decomptemp + os.path.sep + "AndroidManifest.xml")

root = manifest.getroot()

# for permission in root.findall('uses-permission'):
#     if()
#     print "already has network permission"
#     exit() #TODO


#{'{http://schemas.android.com/apk/res/android}name': 'android.permission.ACCESS_NETWORK_STATE'}
inetPermission = ET.SubElement(root,'uses-permission')
inetPermission.set('android:name', 'android.permission.INTERNET')

f = open(decomptemp + os.path.sep + "AndroidManifest.xml",'w')

f.write(ET.tostring(root,encoding='utf8',method='xml'))
print " "