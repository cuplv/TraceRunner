#!/usr/bin/env bash

if [ "$#" -ne 2 ]; then
    echo "Usage: bash launch.sh <Path to Instrumented APK> <Path to Output Trace File>"
    exit 1
fi

INSTR_APK_FILE=$1
OUTPUT_TRACE_FILE=$2

INSTR_APK_PATH=$(dirname "${INSTR_APK_FILE}")

# Retrieving absolute script path
CURR_PATH=$(pwd)
SCRIPT_REL_PATH=$(dirname $0)

cd $SCRIPT_REL_PATH
cd ..
TRACERUNNER_PATH=$(pwd)
cd $CURR_PATH


echo Instrumented APK File: $INSTR_APK_FILE
echo Instrumented APK Path: $INSTR_APK_PATH
echo Output Trace File: $OUTPUT_TRACE_FILE
echo TraceRunner Path: $TRACERUNNER_PATH

PKG_STR=$(python $TRACERUNNER_PATH/utils/pkgAndMainActivity.py $INSTR_APK_PATH)

readarray -t PKG_INFO <<< "$PKG_STR"

APK_PKG=${PKG_INFO[1]}
APK_MAIN=${PKG_INFO[2]}

echo App Package Name: $APK_PKG
echo App Main Class: $APK_MAIN

echo Starting ADB+NetCat Bridge ...
screen -dm bash -c "adb reverse tcp:5050 tcp:5050; nc -l -p 5050 > $OUTPUT_TRACE_FILE" 
echo Started ADB+NetCat Bridge!

echo Running $INSTR_APK_FILE ...
python $TRACERUNNER_PATH/utils/start.py $INSTR_APK_FILE $APK_PKG $APK_MAIN
echo Done!

