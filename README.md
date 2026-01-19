## Simple Fasting App

This Android app is a simple timer that helps with keeping track of your fasting.

The simple interface allows for instant understanding of this app's functionality.

### Nav bar: *[Screenshots](#screenshots) - [Guide](#guide) - [Setup](#setup) - [Resources](#resources)*

---
### Screenshots

![Showing app](/screenshots/screenshot%202.png)
![Changing goal time](/screenshots/screenshot%201.png)
<br>
Music buttons:<br>
![Music on](/screenshots/screenshot%203.png)
![Music off](/screenshots/screenshot%204.png)

---

### Setup

The APK to install this app is located in:
```
./app/release/app-release.apk
```

Download this to an android phone and install it.
Keep in mind that this app is not on any app-store.

---

### Guide

#### Step 1: Set a goal timer
Click on 'Change Goal Time' and pick which time to set as the goal.
The maximum is 23 hours 59 minutes.
Once picked, click 'Confirm selection'
Your goal time has now been set!

#### Step 2: Start the timer
Click on the big green button to start the timer.
Once it has reached the goal time, confetti will cover the screen with victory music!

#### Step 3: Repeat
You can stop and start the timer again by clicking the big button. The goal time will remain as set.

---

### Resources

This app uses android's [time picker example](https://developer.android.com/develop/ui/compose/components/time-pickers#input)
for the goal setting.
It also uses [Konfetti](https://github.com/DanielMartinus/Konfetti?tab=readme-ov-file) for the effect.
The victory sound is [Final Fantasy 7: Victory Fanfare](https://www.youtube.com/watch?v=rgUksX6eM0Y)