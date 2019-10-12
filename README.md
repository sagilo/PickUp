![logo](https://github.com/sagilo/pickup/blob/master/graphics/logo_128x128.png)

# PickUp
An Android app for changing ringer volume for selected repeated callers

My phone is always muted, I hate the distraction and even more I hate to distract others.  
Here and there, my wife tries to get a hold of me with no success.  
Usually it's not that important, but sometimes it's a bit more urgent or just a matter of time relevance.  

For this reason, I've created __PickUp__.  

Now you can mute your phone and be sure no important calls will be missed, at least not more than once or twice ;)


### Current features
* Select phone number for which ringer volume will be changed
* Select how many calls are required before changing the ringer volume
* Select the anount of time in which calls are counted
* Select the ringer volume to be set once all conditions are met

### Screenshots
![screenshot 2](https://github.com/sagilo/pickup/blob/master/screenshots/screenshot_2.png) ![screenshot 1](https://github.com/sagilo/pickup/blob/master/screenshots/screenshot_1.png)


### Contribute
PR's and any other contributions are very much welcome!


### Install
If you are not into compiling and stuff, feel free to install any of the pre-compiled [releases](https://github.com/sagilo/pickup/releases)


#### Why this app is not in Play Store?
Google has restiricted the use of reading the incoming phone number (`CALL_LOG` permission) for non-dialer apps.  
Cuurently, I have no intension of fighiting or working around this so I've figured an open source project can be a nice fit here.


##### Note:  
Android [supports](https://support.google.com/android/answer/9069335?hl=en) Do not disturb exceptions but it's limited only when DND is set (and not when device is just muted or has low volume) and it's not possible to configure the time window or the number of calls for this rule.
