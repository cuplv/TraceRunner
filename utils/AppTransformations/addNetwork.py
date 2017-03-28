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

ET.register_namespace("android","http://schemas.android.com/apk/res/android")

manifest_file = decomptemp + os.path.sep + "AndroidManifest.xml"
manifest = ET.parse(manifest_file)

root = manifest.getroot()

# for permission in root.findall('uses-permission'):
#     if()
#     print "already has network permission"
#     exit() #TODO


#{'{http://schemas.android.com/apk/res/android}name': 'android.permission.ACCESS_NETWORK_STATE'}
inetPermission = ET.SubElement(root,'uses-permission')
inetPermission.set('android:name', 'android.permission.INTERNET')

os.remove(manifest_file)

f = open(manifest_file,'w')
manifest.write(f, xml_declaration=True, encoding="utf-8")

outfile = script_dir + os.path.sep + "out.apk"

print "---------"
pack = ["/usr/bin/java", "-jar", runjar, "b", decomptemp, "-o", outfile]
p = subprocess.Popen(pack)
p.communicate()
