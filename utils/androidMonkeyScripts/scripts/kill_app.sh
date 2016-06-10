#adb shell ps |grep $1 | awk '{print $2}' | xargs adb shell kill #use this one for emulator

adb shell am force stop $1 #use this one for physical devices