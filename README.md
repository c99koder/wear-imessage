# iMessage on Android Wear

This project uses an AppleScript handler to push iMessages from Mac OS X to Android using Google Cloud Messaging.
It also includes a Node.js server that can send voice replies back to iMessage.

![Hi Dave](https://lh3.googleusercontent.com/-o-CubQw11YU/VS68ZF4q7DI/AAAAAAAAuJ0/FDYfU41NTWE/w346-h425/IMG_20150415_104533700_HDR.jpg)

## Building and Configuration

1. Create a Google API project on the Google Developers Console and enable the GCM service.  For more info, see the Getting Started on Android document: https://developer.android.com/google/gcm/gs.html
2. Open app/src/main/java/org/c99/wear_imessage/GCMIntentService.java and enter your GCM project ID number on line 46.  If you want to use the voice replies feature, set ENABLE_REPLIES on line 47 to true.
3. run "./gradlew :assembleDebug" or build the app through Android Studio
4. Install app/build/outputs/apk/app-debug.apk on your device and launch the app
5. Wait for the app to register for push notifications, then tap the "Copy GCM ID" button to copy your device ID to the clipboard
6. Open GCM.applescript and enter your Google API key and GCM device ID on lines 2 and 3
7. Copy GCM.applescript to the iMessage scripts folder (~/Library/Application Scripts/com.apple.iChat on Yosemite)
8. Open iMessage's preferences and select "GCM" as the Applescript handler

*NOTE: Messages will only be forwarded in the background, if you have iMessage focused then the active conversation will not be forwarded*

## Voice replies

Sending voice replies from Wear to iMessage requires running a Node.js server on your Mac.

1. Install Node.js
2. Start the server.js service by typing "node server.js" in a terminal inside the server folder
3. Launch the app on your phone and enter your server's IP and port (the default is 1337)

## TODO

* Store the received messages in a sqlite database and use a single Wear notification with a 2nd page to show the conversation history instead of grouping multiple notifications
* Add an interface for replying from the Android phone too
* Notify if a voice reply fails to send

# License

Copyright (c) 2015 Sam Steele

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.

