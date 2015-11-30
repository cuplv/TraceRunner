adb shell ps |grep $1 | awk '{print $2}' | xargs adb shell kill
