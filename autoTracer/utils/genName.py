
import os
import time
import datetime
import random as r

def generateName(path, prefix="tmp",postfix=""):
   ts = time.time()
   st = datetime.datetime.fromtimestamp(ts).strftime('%Y-%m-%d_%H:%M:%S')
   name = "%s/%s_%s%s" % (path,prefix,st,postfix)
   index = 1
   while os.path.exists(name):
     name = "%s/%s_%s-%s%s" % (path,prefix,st,index,postfix)
     index += 1
   return name
