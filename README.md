# YARC
YARC is a project with the sole objective of allowing you to remotely control your Linux box with typical pointing device features. It is basically composed of two parts: a server-side script written in C and a remote control app for Android. Here is a screenshot:

![](./screenshot.png)

## Capabilities
The YARC Remote Control has the following features:
* two-finger scroll up / down
* relative pointing navigation
* tap selection
* left-button hold / selection
* right-button hold / selection
* "go back" button for browsing
* key input from Android keyboard
* home button (for [StreamCenter](https://github.com/elmadjian/StreamCenter))
* volume control based on pulseaudio-ctl
* PC power on/off (using [WoL](https://en.wikipedia.org/wiki/Wake-on-LAN))
* keepalive to monitor connectivity

## Installing
Make sure you have the following in your system:
* libxtst
* xdotool
* Android SDK
* Android platform and build tools
* PulseAudio

Now compile the `mouse_server_udp.c` file:
```
gcc server_udp.c -lX11 -lXtst -lxdo -o server
```
Install the pre-built [apk](https://github.com/elmadjian/YARC/releases) package (or build one yourself) on you connected Android device:
```
adb install -t yarc.apk
```

## Running
First of all, you need the server running on your Linux box:
```
./server
```
Now, just start the YARC app on your Android device and configure it for the first time, inserting your local Linux box IP and the communication port (the default one is 11111). In order to make the power button functional, you also need to provide the MAC address of your WoL-compliant network adapter. The next time, it will remember the previous configuration and try to establish a connection automatically (_be sure that both your app and the server are in the **same** local network_).

If you intend to use the power button, your motherboard must support [Wake-on-Lan](https://en.wikipedia.org/wiki/Wake-on-LAN). Since it is basically a harware matter, I won't provide any instructions here regarding how to make Wake-on-Lan work, but I recommend this [guide](https://wiki.archlinux.org/index.php/Wake-on-LAN). Also, the power button feature is completely optional, YARC doesn't depend on it to work properly. 

**IMPORTANT:** I don't encourage you to use this app with public IP hosts. This was developed for local networks only and there is no security measures (e.g., encryption) against MiM attacks or any other sort of attack. Doing so exposes your machine in a foolish way. You've been warned.

## FAQ
* **What does YARC stand for?**
**Y**et **A**nother **R**emote **C**ontrol. Just plain and shocking honesty.

* **Could you please add more features or fix a particular bug?**
Short answer: no. Long answer: I only started this project because I couldn't find a decent app in Google Play Store to attend my needs. I don't intend to provide long term support or add more features. But you are free to tinker with it.

* **Can I improve or add some features to your project?**
Sure, why not?

