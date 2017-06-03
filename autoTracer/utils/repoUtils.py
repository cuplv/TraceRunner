
import os
import shutil
import time

from fsUtils import getFilesInPath, getDirsInPath

def createBuildMap(buildPath):
    buildMap = {}
    for userPath in getDirsInPath(buildPath):
        userName = os.path.basename(userPath)
        userMap = {}
        buildMap[userName] = userMap
        for repoPath in getDirsInPath(userPath):
            repoName = os.path.basename(repoPath)
            repoMap = {}
            userMap[repoName] = repoMap
            for hashPath in getDirsInPath(repoPath):
                hashName = os.path.basename(hashPath)
                repoMap[hashName] = hashPath
    return buildMap

def remove_prefix(text, prefix):
    if text.startswith(prefix):
        return text[len(prefix):]
    return text

def migrateBuildRepos(refRepos, srcRepos, destRepos):
    refBMap = createBuildMap(refRepos)
    srcBMap = createBuildMap(srcRepos)

    missing_users = []
    missing_repos = []
    for user in refBMap:
       if user not in srcBMap:
          missing_users.append( user )
          print "User %s is missing!" % user
       else:
          refUMap = refBMap[user]
          srcUMap = srcBMap[user]
          for repo in refUMap:
              if repo not in srcUMap:
                 missing_repos.append( "%s/%s" % (user,repo) )
                 print "Repo %s/%s is missing!" % (user,repo)
              else:
                  refRMap = refUMap[repo]
                  srcRMap = srcUMap[repo]
                  for hashId in refRMap:
                      if hashId not in srcRMap:
                          missing_hash.append( "%s/%s/%s" % (user,repo,hashId) )
                          print "Hash %s/%s/%s is missing!" % (user,repo,hashId)
                      else:
                          srcPath  = srcRMap[hashId]
                          destPath = destRepos + remove_prefix(srcPath, srcRepos)
                          shutil.copytree(srcPath, destPath)
