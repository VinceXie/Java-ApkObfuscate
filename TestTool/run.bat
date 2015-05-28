call ant clean

call ant release
pause

call signapk bin/MergerTest-release-unsigned.apk

call adb install signed.apk