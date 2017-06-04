
import os
import json
from datetime import datetime

from config import DEFAULT_REDIS_CONFIG
from ops import get_redis, push_job, pop_job





# Date str format: "2014-05-13 09:38:56 +0530"
def get_epochtime_from_str(dt_str):
    dt = datetime.strptime(dt_str[:-6], '%Y-%m-%d %H:%M:%S')
    return int(dt.strftime('%s'))


def retrieve_and_load_redis(jsonfilename, force_build=False, remove=True, skip_first=0, max_per_repo=5, dry_run=False, redis_store=None, config=DEFAULT_REDIS_CONFIG):
    if redis_store == None:
        redis_store = get_redis( config=config )
    num_repos   = 0
    num_commits = 0
    with open(jsonfilename, "r") as f:
        line = f.readline()
        while line != '':
            skip_count = 0
            commit_count = 0
            for data in json.loads(line):
                if skip_count >= skip_first:
                   if commit_count < max_per_repo:
                       user_name,repo_name = data['repo'].split("/")
                       dt_committed = get_epochtime_from_str( data['date'] )
                       if not dry_run:
                           push_job(user_name, repo_name, hash_id=data['hash'], dt_committed=dt_committed, force_build=force_build, remove=remove, 
                                    redis_store=redis_store, config=config)
                       print "Added: %s  %s  %s" % (user_name,repo_name,data['hash'])
                       num_commits += 1
                   else:
                       break
                   commit_count += 1
                else:
                   skip_count += 1
            line = f.readline()
            num_repos += 1
    if not dry_run:
        print "Done! %s commits (%s repos) sent to redis@%s:%s" % (num_commits, num_repos, config['host'], config['port'])
    else:
        print "Dry Run Complete! %s commits (%s repos) iterated!" % (num_commits, num_repos)


def count_repos_history_json(jsonfilename):
    with open(jsonfilename, "r") as f:
        line = f.readline()
        count = 0
        while line != '':
            count += 1
            line = f.readline()
    print "Number of Repos: %s" % count 


def watch_list_1():
     return [("googlecast","CastCompanionLibrary-android","db65bab62366e4d5aa1a6b8a90942fd419b00a06")
            ,("googlecast","CastCompanionLibrary-android","7365fda53496b985a7592637b52aa86a7da4c3d4")
            ,("googlecast","CastCompanionLibrary-android","fa6c3943f946dfe5d2b4efd66fe9dd2766d485f8")
            ,("googlecast","CastCompanionLibrary-android","2cd84abb59c3383ce26dca0a09ead6a7a432efb0")
            ,("googlecast","CastHelloText-android","90bb8814a4429955fbaf7d6f0ca68c41974823a7")
            ,("googlecast","CastHelloText-android","8a2e7c775f7da57875a7b5ddb095e68bc9ebbc38")]

def watch_list_2():
     return [("yangtzeu","yangtzeu-app","head")]

def retrieve_and_load_redis_wl(jsonfilename, watch_list, force_build=False, remove=True, skip_first=0, max_per_repo=5, 
                               redis_store=None, config=DEFAULT_REDIS_CONFIG):
    if redis_store == None:
        redis_store = get_redis( config=config )
    with open(jsonfilename, "r") as f:
        line = f.readline()
        while line != '':
            skip_count = 0
            commit_count = 0
            for data in json.loads(line):
                if skip_count >= skip_first:
                   if commit_count < max_per_repo:
                       user_name,repo_name = data['repo'].split("/")
                       dt_committed = get_epochtime_from_str( data['date'] )
                       if user_name in map(lambda x: x[0], watch_list):
                           push_job(user_name, repo_name, hash_id=data['hash'], dt_committed=dt_committed, force_build=force_build, remove=remove, 
                                    redis_store=redis_store, config=config)
                           print "Added: %s  %s  %s" % (user_name,repo_name,data['hash'])
                   else:
                       break
                   commit_count += 1
                else:
                   skip_count += 1
            line = f.readline()
    print "Done!"


def move_jobs(source, target, redis_store=None, config=DEFAULT_REDIS_CONFIG):
    if redis_store == None:
        redis_store = get_redis( config=config )
    data = redis_store.lpop( source )
    count = 0
    while data != None:
       redis_store.lpush( target, data )
       data = redis_store.lpop( source )
       count += 1
    print "Done! Moved %s entries from %s to %s" % (count, source, target)

def extract_jobs(source, target, stats, redis_store=None, config=DEFAULT_REDIS_CONFIG):
    if redis_store == None:
        redis_store = get_redis( config=config )
    data = redis_store.lpop( source )
    count = 0
    tmp = "tmp_%s" % os.getpid()
    while data != None:
       job = json.loads( data )
       if job['fail_stat'] in stats:
           redis_store.lpush( target, data )
           count += 1
       else:
           redis_store.lpush( tmp, data )
       data = redis_store.lpop( source )
    move_jobs(tmp, source, redis_store=redis_store, config=config)
    print "Done! Moved %s entries from %s to %s" % (count, source, target)



