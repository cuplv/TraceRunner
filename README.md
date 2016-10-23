To perform app transformation:

Import into Intellij
=======================

Select import project and choose sbt file
select download sources

Check out android platforms
===========================
git clone git@github.com:Sable/android-platforms.git

Run with arguments
=======================
```
-allow-phantom-refs -android-jars [place android-platforms was checked out]/android-platforms -process-dir [path to apk]/app-debug.apk -output-dir [directory to put apk in]
```

-allow-phantom-refs #this makes fake versions of things we cannot find, this should not affect the instrumentation since we are performing our transformation to every file, but keep an eye on it

-android-jars #non stubbed version of android framework.  The Sable/android-platforms repo is really old though so this may be problematic later, TODO: figure out how to compile recent versions.

example usage:

-j
/home/s/Documents/source/android-platforms
-d
/home/s/Documents/source/TraceRunner/testApps/TestApp/app/build/outputs/apk/app-debug.apk
-o
/home/s/Documents/source/TraceRunner/testApps/output
-p
plv.colorado.*
-i
/home/s/Documents/source/TraceRunner/TraceRunnerInstrumentation/build/libs/TraceRunnerInstrumentation-1.0.jar

-m output jimple instead of apk


Signing APK
===========
Android APK files are signed by the developer to prevent people trying to alter them (usually for mallicious purposes).  By running a code transformation we have now altered the APK meaning the old signature will no longer work.  This is how to apply a new signature.

Create a Key
------------
```
mkdir ~/.keystore
cd ~/.keystore
keytool -genkey -v -keystore recompiled.keystore -alias recompiled -keyalg RSA -keysize 2048 -validity 10000
```

Put instrumentation classes in new apk
--------------------------------------
TODO: this is a new step which avoids problems in soot with protobufs, this will eventually be automated
steps:
* unzip apk (its just a zip file)
* put classes.dex file from  TraceRunnerRuntimeInstrumentation in the unziped file
* create new zip archive with a .dex extension

Sign recompiled apk
-------------------
```
bash utils/resign.sh [path to generated apk] app-debug.apk
```


Starting the new (or any) APK on a phone
========================================

get info for all apk files in a directory:
```
python utils/pkgAndMainActivity.py [path to generated apk]
```

run a given apk:
```
python utils/start.py [apk path] [package] [appname]
```
[appname] is the name of the activity you want to start, this is listed by pkgAndMainActivity.py

Proto Converter Scripts
=======================
These scripts are used to read the traces and convert them into other data formats.

* protoPrinter.py: prints a human readable version of the data

dependencies:

sudo pip install protobuf



Design choices of TraceRunner
=============================

Library inclusion for instrumentation
-------------------------------------
This was primarily based off the presentation in Reference/ccs2013.pdf
However we found an issue with running instrumentation files involving 
protocol buffers through soot for compilation to dex.  For this reason 
the "setApplicationClass" step is ignored in favor of adding the dex
file to the apk later as android simply merges all dex files in the
project root directory.

Auto boxing of primitives
-------------------------
In java thefollowing is valid code:

```
Integer i = 2;
```

This is not the case in jimple, instead write the following:

```
Integer i = Integer.valueOf(2);
```

Failure to do so will result in runtime validation failure on the phone
the error message is not descriptive of the real problem.