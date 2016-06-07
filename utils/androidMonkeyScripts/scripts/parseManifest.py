import os
import sys
import xml.etree.ElementTree as ET

def parseManifest(srcdir):
	manifests = []
	apk = []
	pkgAndMain = []
	for root, dirs, files in os.walk(srcdir):
		for f in files:
			if f.endswith(".apk"):
				apk.append(root + "/" + f)
			if f == "AndroidManifest.xml":
				manifests.append(root + '/' + f)
	
	for manifest in manifests:
		try:
			xmlroot = ET.parse(manifest).getroot()
			
			package = xmlroot.attrib['package']
			applications = xmlroot.findall('application')
			if len(applications) != 1:
				raise AttributeError("too many application tags")
			application = applications[0]
			
			activities = application.findall('activity')
			mainActivity = ""
			for activity in activities:
				intentfilters = activity.findall('intent-filter')
				if len(intentfilters) > 1:
					raise AttributeError("too many intent filters in activity")
				if len(intentfilters) == 1:
					intentfilter = intentfilters[0]
					actions = intentfilter.findall('action')
					if len(actions) != 1:
						raise AttributeError("wrong number of actions")
					action = actions[0]
					if(action.attrib['{http://schemas.android.com/apk/res/android}name'] == 'android.intent.action.MAIN'):
						mainActivity = activity
			#print mainActivity.attrib.keys
			mainActivityName = mainActivity.attrib['{http://schemas.android.com/apk/res/android}name']
			#pkg = package
			#mainActivity = mainActivityName
			pkgAndMain.append((package,mainActivityName))
		except AttributeError as e:
			pass
	return (apk, pkgAndMain)
#@Noneable
def getManifest(srcdir):
	(apkList, pmList) = parseManifest(srcdir)
	#choose apk
	apkk = ""
	if len(apkList) ==0:
		return None
	for apk in apkList:
		if "unaligned" not in apk:
			apkk = apk
	if(apkk == ""):
		apkk = apkList[0]
	
	pmListSorted = sorted(pmList,key=lambda a: len(a[0]+a[1]))	
	#pm = pmListSorted[-1]
	#return (apkk,pm[0],pm[1])
	if(len(pmListSorted) > 0):
		pm = pmListSorted[-1]
		return (apkk,pm[0],pm[1])
	else:
		return None
