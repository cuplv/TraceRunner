
Auto Tracer for Trace Runner
============================

This sub-repository contains examples on how to use Robotium (https://github.com/RobotiumTech/robotium)
an Android test automation framework, to develop trace generation scripts that can be used to automatically
re-run our Android apps and extract traces. It also contains a collection of python scripts that help drive
the process.

Note: Auto Tracer is not a solution for full automation in trace generation. What it offers is replayability
in trace generation, from UI interaction scripts written by a human being. 

For full automation, stay tuned for the sister sub-repository 'monkeyTracer'.

Quick Overview
==============

This sub-repository contains the following:

  * examples - Folder containing test apps with example auto tracer scripts
  * startEmulator.py - Script that starts and stops a given avd device image
  * launch.py - Script that runs the test app for manual trace collection
  * autoInstrument.py - Script that instruments the test app and resigns both the test app APK and auto tracer APK
  * autoLaunch.py - Script that runs the test app with the auto tracer scripts
  * autoChain.py - Top-level script that optionally starts an emulator, and runs all auto test scripts specified in tracerConfig.ini

Instructions
============

Once setup correctly, all you need to do to generate all traces is to run:

> python autoChain.py

TODO: Setup & Install instructions
