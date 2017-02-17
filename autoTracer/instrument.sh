#!/usr/bin/env bash

if [ "$#" -ne 3 ]; then
    echo "Usage: bash instrument.sh <Path to APK> <Path to Output> <Path to Android Jars> <':' separated Instr Blacklist (Optional)>"
    exit 1
fi

APP_APK_FILE=$1
OUTPUT_PATH=$2
# ANDROID_JARS=/home/edmund/workshops/git_workshop/external/android-platforms
ANDROID_JARS=$3
BLACK_LIST=$4

# Retrieving absolute script path
CURR_PATH=$(pwd)
SCRIPT_REL_PATH=$(dirname $0)

cd $SCRIPT_REL_PATH
cd ..
TRACERUNNER_PATH=$(pwd)
cd $CURR_PATH

INST_JAR_FILE=$TRACERUNNER_PATH/TraceRunnerRuntimeInstrumentation/tracerunnerinstrumentation/build/intermediates/bundles/debug/classes.jar
INST_DEX_FILE=$TRACERUNNER_PATH/TraceRunnerRuntimeInstrumentation/app/build/intermediates/transforms/dex/debug/folders/1000/1f/main/classes.dex

if [ "${OUTPUT_PATH:0:1}" = "/" ]
then
   echo ""
else
   OUTPUT_PATH=$CURR_PATH/$OUTPUT_PATH
fi

OUTPUT_APK_FILE=$OUTPUT_PATH/${APP_APK_FILE##*/}

echo Application APK file: $APP_APK_FILE
echo Output Path: $OUTPUT_PATH
echo Output APK: $OUTPUT_APK_FILE
echo TraceRunner Path: $TRACERUNNER_PATH
echo Instrumentation Jar Path: $INST_JAR_FILE
echo Instrumentation Dex Path: $INST_DEX_FILE

cd $TRACERUNNER_PATH
if [ "${BLACK_LIST}" = "" ]
then
  sbt "run -d $APP_APK_FILE -j $ANDROID_JARS -o $OUTPUT_PATH -i $INST_JAR_FILE"
else
  sbt "run -d $APP_APK_FILE -j $ANDROID_JARS -o $OUTPUT_PATH -i $INST_JAR_FILE -x $BLACK_LIST"
fi
echo "App ${APP_APK_FILE##*/} instrumented and written to $OUTPUT_PATH"
cd $CURR_PATH

python $TRACERUNNER_PATH/utils/add_external_dex.py --apk $OUTPUT_APK_FILE --dex $INST_DEX_FILE
echo "Added instrumentation Dex into $OUTPUT_APK_FILE"

bash $TRACERUNNER_PATH/utils/resign.sh $OUTPUT_APK_FILE
echo "Resigned Instrumented APK $OUTPUT_APK_FILE"

echo "Done!"


