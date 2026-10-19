# Mango Home – a simple Google TV style launcher

Clean home screen for Android TV: clock, Favorites row, Apps row, Settings shortcut. No ads, no recommendations, no extra libraries.

## Controls
- D-pad to move, OK to open an app.
- **Hold OK** on an app to add it to / remove it from Favorites.
- Back does nothing on the home screen (by design).

## Build online (no PC / Android Studio needed)
1. Create a free account at github.com and make a new repository.
2. Upload ALL the files from this folder, including the hidden `.github` folder (drag the whole folder contents into the page).
3. Open the **Actions** tab, pick **Build APK**, then **Run workflow** (it also runs automatically on upload).
4. When it turns green (about 3-5 minutes), open the run and download **MangoHome-APK** from the Artifacts section. Unzip it to get `app-debug.apk`.

## Build the APK (on a PC)
1. Install Android Studio (it includes everything needed).
2. File → Open → pick this `MangoLauncher` folder and let Gradle sync finish.
3. Build → Build APK(s). The file is at
   `app/build/outputs/apk/debug/app-debug.apk`

(Or from a terminal in this folder: `./gradlew assembleDebug`.)

## Install on the TV
1. Enable Developer options and USB/Network debugging on the TV.
2. `adb connect <TV-IP>:5555`
3. `adb install app/build/outputs/apk/debug/app-debug.apk`

## Make it the default home screen
Press the Home button on the remote. If Android asks which Home app to use, choose **Mango Home** and **Always**.

If the TV keeps going back to its own launcher, find its package with
`adb shell pm list packages | grep -i launcher` and disable it for your user only:

`adb shell pm disable-user --user 0 <stock.launcher.package>`

Only do this after Mango Home is installed and works. To undo:
`adb shell pm enable <stock.launcher.package>`
