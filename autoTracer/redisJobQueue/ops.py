
import os
import logging

from redis import StrictRedis
from json import dumps, loads

from config import DEFAULT_REDIS_CONFIG

def get_redis(config=DEFAULT_REDIS_CONFIG):
    if config['pwd'] == None:
        return StrictRedis(host=config['host'], port=config['port'], db=config['db'])
    else:
        return StrictRedis(host=config['host'], port=config['port'], db=config['db'], password=config['pwd'])

# Yes.. its currently serializing JSON. Quick and simple solution
# TODO: Satisfy Protobuf scums by changing to Protobufs perhaps? 


def push_queue(queue, user_name, repo_name, hash_id=None, redis_store=None, config=DEFAULT_REDIS_CONFIG):
    if redis_store == None:
        redis_store = get_redis(config=config)
    job = { 'user':user_name, 'repo':repo_name, 'hash':hash_id }
    redis_store.lpush( queue, dumps(job) )

def push_job(user_name, repo_name, hash_id=None, redis_store=None, config=DEFAULT_REDIS_CONFIG):
    return push_in_queue(config['jobs'], user_name, repo_name, hash_id, redis_store, config=config)

def push_done_jobs(user_name, repo_name, hash_id=None, redis_store=None, config=DEFAULT_REDIS_CONFIG):
    return push_in_queue(config['done'], user_name, repo_name, hash_id, redis_store, config=config)

def push_fail_jobs(user_name, repo_name, hash_id=None, redis_store=None, config=DEFAULT_REDIS_CONFIG):
    return push_in_queue(config['failed'], user_name, repo_name, hash_id, redis_store, config=config)

'''
def push_job(user_name, repo_name, hash_id=None, dt_committed=None, force_build=False, remove=True, redis_store=None, config=DEFAULT_REDIS_CONFIG):
    if redis_store == None:
        redis_store = get_redis(config=config)
    job = { 'user':user_name, 'repo':repo_name, 'hash':hash_id, 'dt_committed':dt_committed, 'force':force_build, 'remove':remove }
    redis_store.lpush( config['jobs'], dumps(job) )

def push_failed_job(user_name, repo_name, hash_id=None, dt_committed=None, force_build=False, remove=True, fail_stat='FL', redis_store=None, config=DEFAULT_REDIS_CONFIG):
    if redis_store == None:
        redis_store = get_redis(config=config)
    job = { 'user':user_name, 'repo':repo_name, 'hash':hash_id, 'dt_committed':dt_committed, 'force':force_build, 'remove':remove, 'fail_stat':fail_stat }
    redis_store.lpush( config['failed'], dumps(job) )
'''

def dequeue(queue, redis_store=None, config=DEFAULT_REDIS_CONFIG):
    if redis_store == None:
        redis_store = get_redis(config=config)
    data = redis_store.lpop( queue )
    return loads( data ) if data != None else None

def bdequeue(queue, timeout=0, redis_store=None, config=DEFAULT_REDIS_CONFIG):
    if redis_store == None:
        redis_store = get_redis(config=config)
    resp = redis_store.blpop( queue, timeout=timeout )
    if resp != None:
       _, data = resp
       # print data
       return loads( data )
    else:
       return None

def pop_job(redis_store=None, config=DEFAULT_REDIS_CONFIG):
    return dequeue(config['jobs'], redis_store, config)

def bpop_job(redis_store=None, config=DEFAULT_REDIS_CONFIG):
    return bdequeue(config['jobs'], redis_store, config)

def pop_done_job(redis_store=None, config=DEFAULT_REDIS_CONFIG):
    return dequeue(config['done'], redis_store, config)

def bpop_done_job(redis_store=None, config=DEFAULT_REDIS_CONFIG):
    return bdequeue(config['done'], redis_store, config)

def pop_fail_job(redis_store=None, config=DEFAULT_REDIS_CONFIG):
    return dequeue(config['failed'], redis_store, config)

def bpop_fail_job(redis_store=None, config=DEFAULT_REDIS_CONFIG):
    return bdequeue(config['failed'], redis_store, config)



def num_of_jobs(redis_store=None, config=DEFAULT_REDIS_CONFIG):
    if redis_store == None:
        redis_store = get_redis(config=config)
    return redis_store.llen( config['jobs'] )

def show_redis_connect_params(config=DEFAULT_REDIS_CONFIG, verbose=False):
    host = config['host']
    port = config['port']
    if host == None:
        host = "127.0.0.1"
    if port == None:
        port = "6379"
    msg = "Redis @ %s:%s" % (host,port)
    if config['pwd'] != None:
        msg += " with auth \'%s\'" % config['pwd']
    if verbose:
        msg += "\n   Fetching Jobs from \'%s\'" % config['jobs']
        msg += "\n   Listening for admin tasks from \'%s\'" % config['builder']
    return msg

def ping_redis(config=DEFAULT_REDIS_CONFIG):
    try:
        rd = get_redis(config=config)
        test_key = "ping_%s" % os.getpid()
        rd.lpush( test_key, -1)
        rd.lpop( test_key )
    except Exception, e:
        err_msg = 'Failed to connect to Redis server: %s' % show_redis_connect_params(config=config)
        logging.error(err_msg, exc_info=True)
        print err_msg
        return False
    return True

def load_in_queue(queue, ls, redis_store=None, config=DEFAULT_REDIS_CONFIG):
    count = 0
    for user,repo,chash in ls:
         push_queue(queue, user, repo, hash_id=chash, redis_store=redis_store, config=config)
         print "Added %s %s %s into %s" % (user,repo,chash,queue)
         count += 1
    print "Added: %s entries to %s" % (count, queue)

def load_search_data(queue, json_file = '/data/search-data/data.json', redis_store=None, config=DEFAULT_REDIS_CONFIG, unique_repo=True):
   with open(json_file, "r") as f:
      count = 0
      data = loads(f.read())['response']

      print "Number of docs found: %s" % data['numFound']
      # print data['start']

      # print data['docs'][0]
 
      ls = []
      history = {}

      for d in data['docs']:
          # user = d['user_sni']
          # repo = d['repo_sni']
          # print "%s   %s" % (user,repo)
          chash = d['hash_sni']
          user,repo = d['repo_sni'].split("/")
          if unique_repo:
             key = "%s/%s" % (user,repo)
             if key not in history:
                ls.append( (user,repo,chash) )
                history[key] = ()
          else:
             ls.append( (user,repo,chash) )
      
      load_in_queue(queue, ls, redis_store, config)


