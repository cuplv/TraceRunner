
from ConfigParser import ConfigParser

REDIS_OPTS = 'redisOptions'

'''
host: Host name of Redis instance
port: Port of Redis instance
db:   DB argument of Redis instance
jobs: Name of the build job queue (to be shared by all builders)
builder: Prefix name of a builder (each unique instance will be used in Redis as an admin channel to that builder). 
'''
DEFAULT_REDIS_CONFIG = { 'host':'0.0.0.0', 'port':6379, 'db':0, 'jobs':'build_jobs', 'name':'phoenix', 'pwd':None, 'done':'done_jobs', 'failed':'failed_jobs' }
