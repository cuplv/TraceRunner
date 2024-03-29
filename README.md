The process of implementing an Android application involves creating a set of callbacks which are invoked by the operating system to inform the app of various external events.  In turn these callbacks can invoke methods on the framework, which we refer to as callins, to send information to the framework about what the app wants to do or be notified of. These events have a strict protocol of when things are allowed to be called and when they are not leading to hard to understand exceptions when the developer violates this protocol. This tool is part of a broader project to address this issue by allowing us to observe this sequence of callins and callbacks as they are executed in the application.  Tracerunner functions by modifying the code of an Android application to transmit information every time a callback or a callin occurs which can be logged and analyzed later either manually or automatically.

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
sbt run -j [android platforms] -d [input apk] -o /home/s/Documents/source/TraceRunner/testApps/output -p plv.colorado.* -i /home/s/Documents/source/TraceRunner/TraceRunnerRuntimeInstrumentation/tracerunnerinstrumentation/build/intermediates/bundles/debug/classes.jar


-j #non stubbed version of android framework.  The Sable/android-platforms repo is really old though so this may be problematic later, TODO: figure out how to compile recent versions.

example usage:

Put instrumentation classes in new apk
--------------------------------------
TODO: this is a new step which avoids problems in soot with protobufs, this will eventually be automated

```
python ../../utils/add_external_dex.py --dex /home/s/Documents/source/TraceRunner/TraceRunnerRuntimeInstrumentation/app/build/intermediates/transforms/dex/debug/folders/1000/1f/main/classes.dex --apk app-debug.apk
```

steps:
* unzip apk (its just a zip file)
* put classes.dex file from  TraceRunnerRuntimeInstrumentation in the unziped file
* create new zip archive with a .dex extension

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

note: please add internet permission to android app
```
    <uses-permission android:name="android.permission.INTERNET" />
```

Receiving the trace
===================
```
adb reverse tcp:5050 tcp:5050 #bridge port to local machine through adb
nc -l -p 5050 > trace #open socket and write data to file "trace"
```
The instrumentation transmits the trace data to localhost:5050 (this can be changed in the androidruntimeinstrumentation)
currently the easiest way to read this is "nc -l -p 5050 > trace". With a rooted phone running busybox this can be run on the device itself however the port can also be forwarded as in the commands at the top of this section.
Getting the trace
=================
adb pull /sdcard/trace .

Proto Converter Scripts
=======================
These scripts are used to read the traces and convert them into other data formats.

* protoPrinter.py: prints a human readable version of the data

dependencies:

sudo pip install protobuf

usage
```
python utils/ProtoConverter/protoPrinter.py --trace trace
```



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

Application versus framework code
---------------------------------
This is decided based on a pre defined set of filters in resources/android_packages.txt.  If the package matches one of these then it is considered framework. Otherwise application.

Logging callbacks
-------------------------
All non static methods are logged as potential callbacks.  After trace generation we will filter out the ones which were not callbacks by looking at the type of the thing calling them.  Most static methods are not logged in this way as they can never be callbacks.  The one exception is \<clinit\> which can be a callback.  The effect of this is that in the trace their method calls will appear to be inlined with the callback which invoked them.

\<clinit\> is not always invoked by the framework.  It appears to be called just before the first initialization by whatever the top stack frame is.  If there are callins in the \<clinit\> and it is not logged as a callback the methods will show up in the parent transition.


Trace Version Tag
===============================
Tag of the format "traceversion0" which denotes that there have been no changes that will affect the appearance of traces until another trace version tag is added. 


Multi Dex Output
===============================
Danger: work in progress
Append the -c option to Tracerunner to output class files somewhere. (Currently there is a bug related to doing this which we are working on)

Compile class files to dex with the dx tool
```
Android/Sdk/build-tools/24.0.0/dx --dex --multi-dex --output="output.apk" .
```

Debugging
=========
Viewing exceptions
------------------
The command "adb logcat" prints the log file that uncaught exceptions are logged to.  Sometimes it can help to clear the log, run it, and then look at the log again to know that the exception came from your most recent run.  Do this with "adb logcat -c"

Known exceptions
----------------
* IOException: occurs when data cannot be sent from the app to the receiver
    * check the following
        * bridge started successfully (adb reverse tcp:5050 tcp:5050)
        * Application has the internet permission (see above)
        * nc -l -p 5050 is running only once (check with "ps aux |grep 5050")
    * Also try
        * restart adb by running adb kill-server
        * clean and rebuild app (gradle doesn't always monitor the AndroidManifest.xml file for changes properly)
        
Other debugging steps
---------------------
* clean and rebuild app (there was a developer tools update in Jan 2017 which causes issues until you clean and rebuild)
