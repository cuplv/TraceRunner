
TRACERUNNER_ROOT=/Users/s/Documents/source/TraceRunner
cd $TRACERUNNER_ROOT
PLATFORMS=/Users/s/Library/Android/sdk/platforms/
ORIG_APK=$1
OUTPUT=$2

#instrument

java -jar ${TRACERUNNER_ROOT}/target/scala-2.11/tracerunner_2.11-0.1-SNAPSHOT-one-jar.jar -j ${PLATFORMS} -d $ORIG_APK -o $OUTPUT  -i ${TRACERUNNER_ROOT}/TraceRunnerRuntimeInstrumentation/tracerunnerinstrumentation/build/intermediates/bundles/debug/classes.jar

#extract apk name
APKNAME=$(basename $ORIG_APK)
echo "APK name: $APKNAME"

#add instrumentation

echo "add instrumentation to ${OUTPUT}/${APKNAME}"

python ${TRACERUNNER_ROOT}/utils/add_external_dex.py --apk ${OUTPUT}/${APKNAME} --dex ${TRACERUNNER_ROOT}/TraceRunnerRuntimeInstrumentation/app/build/intermediates/transforms/dex/debug/folders/1000/1f/main/classes.dex

echo "resign apk"

#resign
bash ${TRACERUNNER_ROOT}/utils/resign.sh ${OUTPUT}/${APKNAME}

