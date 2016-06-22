adb logcat -c
LOGNAME=$1_`date +"%M_%H_%m_%d_%Y"`.traceperflog
(sleep 3; python $1.py /Users/s/Documents/source/computerperformancemodeling/finalProject/AndroidBenchmarkCallins/app/build/outputs/apk/app-debug.apk plv.colorado.edu.callinsbenchmark .CallinsBenchmark)&

adb logcat

