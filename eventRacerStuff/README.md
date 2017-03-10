
Guide to Using Event Racer without Losing 48hrs of your Life
============================================================

This subpage contains information on how to get Event Racer running on your local system. It will also contain test apps from our corpus that are ported back to Android 4.4 (also with build configurations that work with Event Racer). We also provide Event Racer outputs from specific runs on each of these apps (follow instructions below on how to view these outputs on your local system).

First, the important links: more information of Event Racer is found at http://eventracer.org/android/ and you can download it from https://dl.dropboxusercontent.com/u/5393002/EventRacerAndroid.tar.gz . 

Viewing Event Racer output
==========================

In the folder 'testapp', you will find a repository of test apps, each containing:

* The android application source, that works with Event Racer
* A folder 'output' contain output files obtained from 'traces of interest' for each test app. These output are interesting because they are obtained by exercising the apps in a same manner in which we have done with TraceRunner. Hence, possibly serving as an initial collection of evaluation results that 'compares' Event Racer with our approach.

To view these outputs, you must first obtain the Event Racer from https://dl.dropboxusercontent.com/u/5393002/EventRacerAndroid.tar.gz . Untar it and follow the Readme instructions in it to get your system setup with the right dependencies to run Event Racer. You will also need 'dot' (e.g., apt-get install graphviz) for generating the Happens-before graph displays in the output. 

Once you have Event Racer setup, go into one of the 'output' folders here, you will find two subfolders, 'inTmp' and 'inEventRacer'. Copy the contents of 'inTmp' to your '/tmp' directory and 'inEventRacer' into the Event Racer's 'EventRacer' directory. once, you have done this, run the script 'run_eventracer.sh' and you can view the output in your browser at 'http://localhost:8000/groups_extended'. This is the only (and horrible way) to view previous outputs of 'Event Racer'.

Running Event Racer with Your Own APKs
======================================

This is a little tricky.. The two main issue are (1) getting your APK to work with Event Racer and (2) getting Event Racer scripts to work with your computer. Apparently, Event Racer was tested on Ubuntu 12.04 and 14.04. Even running on 16.04 had a few complications that require some minor mods on the script, so if you are using a Mac Os... good luck.

Let's work on (1) first. Event Racer only works on Android 4.4 (API level 19), so you need to revert your build configurations to dependencies that support that Android version. The easier way (rather than blindly punching in smaller numbers in your gradle build config) is to copy as much as possible from the build configs in the test apps here, while manually resolving whatever conflicts that specifically appear for your app. This should be doable for most of our distilled apps, but would likely be a nightmare for real apps. Remember that you will also have to revert the Android SDK used (to buildToolsVersion "21.1.2"). This is because Event Racer relies on an arcane version of APKTool (v 2.0.0) and if you use more recent Android SDKs, its Android Manifest will be unusable by Event Racer.

Once you have solved (1), you can start trying to run Event Racer. First just run './analyse_app.sh <your apk>'.. why not, you might get lucky and it works on the first run. It did not for me, but let's not solve problems that might not be problems for you. Here's a list of problems and solutions that you might run into:

* command adb not found: You need to make your Android SDK bins global.
* multiple devices detected: don't charge your phone while running Event Racer. The Event Racer scripts are written with the assumption that the only device that adb can connect to, is the stupid emulator that it creates.
* script loops forever at 'waiting for emulator': go to the script 'emulator/start_emulator.sh' and comment away the offending while loop at line 53. Replace with 'sleep 30', or 'sleep 300' if you like a coffee break everytime you run Event Racer.
* script freezes at 'Installing application': go to 'scripts/exercise_app.sh', go to line 21 and comment away 'out=`adb install /tmp/androidracer/apks/$1 2>&1 | tail -n 1`'. 'out' is never referenced and I don't know what this silly command is doing. Replace it with 'adb install -r /tmp/androidracer/apks/$1' .

These are all the issues I ran into. Do append to this if you encounter more issues. 

If all works out, Event Racer should prompt you if you want random tracing. If you do, answer 'Y' and it will initiate monkey tracer. If you say 'n', you can manually invoke the tracing: you will have to start the app yourself, do your magic, and once you are done, hit enter in the Event Racer terminal. Give it some time, and it should generate the output.







