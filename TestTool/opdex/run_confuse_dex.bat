call run_clean.bat

call baksmali\baksmali %1

java -jar confuse.jar

call baksmali\smali -o classes.dex out

copy %1 dex.zip

7z\7z u dex.zip classes.dex

ren dex.zip dex.apk

call dex2jar\d2j-apk-sign -o dex_sign.apk -f dex.apk

adb install dex_sign.apk

pause