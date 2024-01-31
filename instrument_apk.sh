dir_resolve() {
  local dir=`dirname "$1"`
  local file=`basename "$1"`
  pushd "$dir" &>/dev/null || return $? # On error, return error code
  echo "`pwd -P`/$file" # output full, link-resolved path with filename
  popd &> /dev/null
}

echo "$1"

ORIG_APK=`dir_resolve $1`
echo "Orig_APK: ${ORIG_APK}"
OUTPUT=`dir_resolve $2`
echo "Output: ${OUTPUT}"


TRACERUNNER_ROOT=/Users/$(whoami)/Documents/source/TraceRunner
cd $TRACERUNNER_ROOT
PLATFORMS=/Users/$(whoami)/Library/Android/sdk/platforms/
#instrument

TRACERUNNER_JAR="/target/scala-2.13/tracerunner-assembly-0.1.0-SNAPSHOT.jar"
#TRACERUNNER_JAR="/target/scala-2.11/tracerunner_2.11-0.1-SNAPSHOT-one-jar.jar"
#with common exclusion list for exceeding method limits
#java -jar ${TRACERUNNER_ROOT}${TRACERUNNER_JAR} -j ${PLATFORMS} -d $ORIG_APK -o $OUTPUT  -i ${TRACERUNNER_ROOT}/TraceRunnerRuntimeInstrumentation/tracerunnerinstrumentation/build/intermediates/bundles/debug/classes.jar -x "org.apache.commons*:org.jsoup*:com.squareup.okhttp*:rx*:org.shredzone.flattr4j*:okhttp3*:retrofit2*:com.bumptech.glide*:org.json*:java*"

java -jar ${TRACERUNNER_ROOT}${TRACERUNNER_JAR} -j ${PLATFORMS} -d $ORIG_APK -o $OUTPUT  -i ${TRACERUNNER_ROOT}/TraceRunnerRuntimeInstrumentation/tracerunnerinstrumentation/build/intermediates/bundles/debug/classes.jar -p "$3"

#extract apk name
APKNAME=$(basename $ORIG_APK)
echo "APK name: $APKNAME"

#add instrumentation

echo "add instrumentation to ${OUTPUT}/${APKNAME}"

python ${TRACERUNNER_ROOT}/utils/add_external_dex.py --apk ${OUTPUT}/${APKNAME} --dex ${TRACERUNNER_ROOT}/TraceRunnerRuntimeInstrumentation/app/build/intermediates/transforms/dex/debug/folders/1000/1f/main/classes.dex

echo "add network permission to ${OUTPUT}/${APKNAME}"

python ${TRACERUNNER_ROOT}/utils/AppTransformations/addNetwork.py --apk ${OUTPUT}/${APKNAME} --output ${OUTPUT}/tmp
rm ${OUTPUT}/${APKNAME}
mv ${OUTPUT}/tmp ${OUTPUT}/${APKNAME}

echo "resign apk"

#resign
bash ${TRACERUNNER_ROOT}/utils/resign.sh ${OUTPUT}/${APKNAME}

