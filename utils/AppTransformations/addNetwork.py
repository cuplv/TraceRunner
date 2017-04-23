import subprocess
import os
import shutil
import xml.etree.ElementTree as ET
import argparse

# apk = sys.argv[1]
# script_path= os.path.realpath(__file__)
# script_dir = os.path.sep.join(script_path.split(os.path.sep)[:-1])
#
# outfile = script_dir + os.path.sep + "out.apk"
#
# runjar = script_dir + os.path.sep + "apktool_2.2.2.jar"
# decomptemp = script_dir + os.path.sep + "tmp"

def add_network(apk, outfile,decomptemppar = None, runjarpar = None):
    script_path= os.path.realpath(__file__)
    script_dir = os.path.sep.join(script_path.split(os.path.sep)[:-1])
    if decomptemppar is None:
        decomptemp = script_dir + os.path.sep + "tmp"
    else:
        decomptemp = decomptemppar

    #check if temp file exists and throw error if it does
    if os.path.exists(decomptemp):
        raise Exception("temp directory already exists, please delete or choose another with --temp")

    if runjarpar is None:
        runjar = script_dir + os.path.sep + "apktool_2.2.2.jar"
    else:
        runjar = runjarpar
    path_ = ["/usr/bin/java", "-jar", runjar, "d", apk, "-s", "-o", decomptemp]
    print path_
    subprocess.call(path_)



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
    f.close()


    import time

    for i in xrange(10):
        time.sleep(1)

    pack = ["/usr/bin/java", "-jar", runjar, "b", decomptemp, "-o", outfile]
    subprocess.call(pack)
    shutil.rmtree(decomptemp)
    # os.rmdir(decomptemp)

if __name__ == "__main__":
    parser = argparse.ArgumentParser(description='Add network permission to APK file')
    parser.add_argument('--apk', type=str,
                        help="apk file",required=True)
    parser.add_argument('--temp', type=str,
                        help="temporary directory (must not exist)", required=False)
    parser.add_argument('--output', type=str,
                        help="location of output", required=True)
    parser.add_argument('--apk_tool', type=str,
                        help="location of apktool jar", required=False)
    args = parser.parse_args()
    add_network(args.apk, args.output,args.temp, args.apk_tool)
