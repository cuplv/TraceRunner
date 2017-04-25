import os
import json
import pycurl
from StringIO import StringIO
from subprocess import Popen, PIPE

from utils.fsUtils import createPathIfEmpty, recreatePath, getFilesInPath

# yDelouis/selfoss-android/a562d234f1d76c5f9e8d2fcf1b5bd3da22d785b5

# 47deg/translate-bubble-android/e39aa2282510e0acd91b2aa84903de8dd9afdda3

'''
No entries retrieved on:
TheLester/mytaskmanager, 
OneBusAway/onebusaway-android, 
thenewcircle/class-3090, 
zeroows/Wordpress-Android-App, 
Paku-/MavLinkHUB, 
scala-android/sbt-android, 
imhotep/MapsPluginExample, 
warmshowers/wsandroid, 
poliva/WifiStaticArp

## Failed to copy from Builder: 
47deg/translate-bubble-android, 
yeokm1/nus-soc-print, 
smarek/Simple-Dilbert, 
googlesamples/android-SynchronizedNotifications, 
casimir/simpleDeadlines, 
wada811/Android-Material-Design-Colors, 
gonjay/rubychina4android, 
xiaopansky/AndroidToolbox, 
woalk/TintedTranslucentStatusbar, 
RaghavSood/CompilingAndroidMail, 
smarek/Simple-Dilbert, 
nowlauncher/now-launcher, 
china-ece/Gaia, 
altchen/mopoo-android, 
FoamyGuy/StackSites
'''

troubles = [('TheLester','mytaskmanager'), 
           ('OneBusAway','onebusaway-android'), 
           ('thenewcircle','class-3090'), 
           ('zeroows','Wordpress-Android-App'), 
           ('Paku-','MavLinkHUB'), 
           ('scala-android','sbt-android'), 
           ('imhotep','MapsPluginExample'), 
           ('warmshowers','wsandroid'), 
           ('poliva','WifiStaticArp'),
           ('47deg','translate-bubble-android'), 
           ('yeokm1','nus-soc-print'), 
           ('smarek','Simple-Dilbert'),
           ('googlesamples','android-SynchronizedNotifications'), 
           ('casimir','simpleDeadlines'), 
           ('wada811','Android-Material-Design-Colors'), 
           ('gonjay','rubychina4android'), 
           ('xiaopansky','AndroidToolbox'), 
           ('woalk','TintedTranslucentStatusbar'), 
           ('RaghavSood','CompilingAndroidMail'), 
           ('smarek','Simple-Dilbert'), 
           ('nowlauncher','now-launcher'), 
           ('china-ece','Gaia'), 
           ('altchen','mopoo-android'),
           ('FoamyGuy','StackSites')]

apps = [('yDelouis','selfoss-android')
       ,('47deg','translate-bubble-android')
       ,('TheLester','mytaskmanager') 
       ,('yeokm1','nus-soc-print') 
       ,('smarek','Simple-Dilbert') 
       ,('googlesamples','android-SynchronizedNotifications') 
       ,('monakhv','samlib-Info') 
       ,('casimir','simpleDeadlines')
       ,('OneBusAway','onebusaway-android')
       ,('wada811','Android-Material-Design-Colors')
       ,('gonjay','rubychina4android')
       ,('xiaopansky','AndroidToolbox')
       ,('thenewcircle','class-3090')
       ,('wikimedia','apps-android-wikipedia')
       ,('woalk','TintedTranslucentStatusbar')
       ,('zeroows','Wordpress-Android-App')
       ,('RaghavSood','CompilingAndroidMail')
       ,('Paku-','MavLinkHUB')
       ,('smarek','Simple-Dilbert')
       ,('scala-android','sbt-android')
       ,('OceanLabs','Android-Print-SDK')
       ,('imhotep','MapsPluginExample')
       ,('PingPlusPlus','pingpp-android')
       ,('warmshowers','wsandroid')
       ,('nowlauncher','now-launcher')
       ,('china-ece','Gaia')
       ,('altchen','mopoo-android')
       ,('FoamyGuy','StackSites')
       ,('poliva','WifiStaticArp')]

newlist1 = [('vinsol','expense-tracker')
           ,('yangtzeu','yangtzeu-app')
           ,('thehung111','ContactListView')
           ,('qylk','AudioPlayer')
           ,('life0fun','wifi-direct-chat')
           ,('blundell','FaceDetectionTutorial')
           ,('blint','SSLDroid')
           ,('WizTeam','WizAndroidMiNotes')
           ,('swapagarwal','BlackJack')
           ,('qylk','AudioPlayer')
           ,('joaobmonteiro','livro-android')
           ,('yhcting','netmbuddy')
           ,('smblott-github','intent_radio')
           ,('jianfengye','Android_Works')
           ,('macdidi5','AndroidTutorial')
           ,('wangym','zxing-client-android')
           ,('NandoVelazquez','Android-Video-Player-Example')
           ,('dozingcat','CamTimer')
           ,('pfn','android-sdk-plugin')
           ,('cgutman','USBIPServerForAndroid')
           ,('ssnhitfkgd','webRTC')
           ,('zielmicha','emacs-android-app')
           ,('dogtim','VideoEditor')
           ,('qylk','AudioPlayer')
           ,('yxl','DownloadProvider')
           ,('zielmicha','emacs-android-app')
           ,('xiaogegexiao','vipmediaplayer')
           ,('billthefarmer','sig-gen')
           ,('phishman3579','android-augment-reality-framework')]

def retrieveBuildData(user, repo):
   print "Calling http://52.15.135.195:8080/builds/?user=%s&repo=%s&stat=ps" % (user,repo)
   buffer = StringIO()
   c = pycurl.Curl()
   c.setopt(c.URL, 'http://52.15.135.195:8080/builds/?user=%s&repo=%s&stat=ps' % (user,repo))
   c.setopt(c.WRITEDATA, buffer) 
   c.perform()
   c.close()

   body = buffer.getvalue()
   resp = json.loads(body)

   print("Response received: %s results" % len(resp['results']))

   return resp
   
def latestCommit(resp):
   newest = None
   newest_dtstamp = 0
   for result in resp['results']:
      print(result['hash'])
      if result["dt_committed"] > newest_dtstamp:
          newest = result
          result_dtstamp = result['dt_committed']
   return newest

def copyBuildData(commit, baseLocalRepoPath, appBuilderName, baseRemoteRepoPath):

   failed_copy = 0

   appNames = []
   for name,info in commit['apps'].items():
       appName = "%s-%s-%s" % (commit['user'],commit['repo'],name)
       currRepoPath = baseLocalRepoPath + "/%s" % appName
       recreatePath( currRepoPath )
       for apkPath in info['apk']:
          # relativeApkPath = newest['apps'].values()[0]['apk'][0]
          remoteApkPath = '%s:%s/%s/%s/%s/%s' % (appBuilderName,baseRemoteRepoPath,commit['user'],commit['repo'],commit['hash'],apkPath)
          localApkPath  = currRepoPath + "/" + os.path.basename(remoteApkPath)
          print "Copying %s to %s" % (remoteApkPath,localApkPath)
          res = None
          try:
             res = os.system("scp %s %s" % (remoteApkPath,localApkPath))
          except:
             print("Failed to copy %s to %s" % (remoteApkPath,localApkPath))
             failed_copy += 1 

          if res != 0 and res != None:
             print("Failed to copy %s to %s" % (remoteApkPath,localApkPath))
             failed_copy += 1

          if res == 0:
             appNames.append(appName)

   return failed_copy,appNames

def extractSearchOutput(json_file = '/data/search-data/data.json'):
   with open(json_file, "r") as f:
      count = 0
      data = json.loads(f.read())['response']

      print "Number of docs found: %s" % data['numFound']
      print data['start']

      print data['docs'][0]
 
      ls = []
      for d in data['docs']:
          # user = d['user_sni']
          # repo = d['repo_sni']
          # print "%s   %s" % (user,repo)
          user,repo = d['repo_sni'].split("/")
          ls.append( (user,repo) )
      return ls

if __name__ == "__main__":
   
   baseRepoPath = "/data/callback-v4/repo"
   appBuilderName = 'muse-behemoth'
   baseRemoteRepoPath = '/eval/data/production1'

   failedRetData = []
   noEntries = []
   failedcopy = []

   history = {}

   for user,repo in extractSearchOutput() + apps + newlist1: # troubles: # apps:
       resp = None
       key = "%s/%s" % (user,repo)
       if key not in history:
         history[key] = ()
         try:
            resp = retrieveBuildData(user,repo)
         except e:
            print("Failed to retrieve info of %s/%s from App Builder" % (user,repo))
            failedRetData.append( (user,repo) )
 
         if resp != None:
  
            if len(resp['results']) == 0:
               print("No entries")
               noEntries.append( (user,repo) )
            else:
               commit = latestCommit(resp)
               succ = copyBuildData(commit, baseRepoPath, appBuilderName, baseRemoteRepoPath)
               if not succ:
                   failedcopy.append( (user,repo) )
         

   print("## Failed on Builder Data Retrieve: %s" % ', '.join(map(lambda x: "%s/%s" % x,failedRetData)) ) 
   print("## No entries retrieved on: %s" % ', '.join(map(lambda x: "%s/%s" % x,noEntries)) )
   print("## Failed to copy from Builder: %s" % ', '.join(map(lambda x: "%s/%s" % x,failedcopy)) )

