#!/usr/bin/python
import os
import sys
import xml.etree.ElementTree as ET

if len(sys.argv) != 2:
	raise Exception("usage [source path]")

srcdir = sys.argv[1]
manifests = []
for root, dirs, files in os.walk(srcdir):
	for f in files:
		if f.endswith(".apk"):
			print "apk: " + root + "/" + f
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
		mainActivityName = mainActivity.attrib['{http://schemas.android.com/apk/res/android}name']
		print "==================="
		print manifest
		print "--"
		print "package: " + package
		print "main activity name: " + mainActivityName
	except AttributeError as e:
		if False:
			print "--------------------"
			print manifest
			print "bad manifest: " + str(e)
