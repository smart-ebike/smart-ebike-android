Smart EBike Android app

== How to build APK installer file ==
 1. Install Maven 3
 2. "cd /<path>/<to>/<this>/<project>"
 3. "mvn clean package"
 4. You'll find the signed APK file in "dashboard/target" directory.
 5. ...
 6. Profit

== How to install onto your device
 1. Make sure your device is connectect and ADB lists it (adb devices)
 2. mvn clean package android:deploy
 3. ...
