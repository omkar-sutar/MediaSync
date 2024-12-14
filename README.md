# MediaSync

### MediaSync is an android client to backup your media to MediaHaven.


<div align="center">
  <img src="./docs/assets/mediasync-main.png" alt="mediasync-img" width="25%">
</div>
<br>

**Note**: You need MediaHaven API Server up and running to use this application.

## Installation 🚀
Head to [release](https://github.com/omkar-sutar/MediaSync/releases) section to get the desired release. Download and install the APK file of the corresponding release.
Make sure to provide the permissions requested when the application is started for the first time.

## Configuration ⚙️
To configure the app, set the values of following parameters by clicking `Edit` button beside the value.
<ul>
  <li>Server IP Address</li>
  <li>Server IP Port</li>
  <li>Username</li>
  <li>Password</li>
</ul>
<br>To test the configuration, click on Ping button. The client will attempt to connect and log in with the provided credentials.

#### User preferences (optional)
<ul>
  <li>Date threshold: The media files created or modified before this date will be skipped for backup. This is useful when old media is already backed up to a different place, or does not need to be backed up.</li>
  <li>Folders: A comma seperated list of folders to be considered for backup. By default, <code>DCIM/Camera</code> is included. Use <code>%</code> for wildcard path matching.
  Example value: <code>%DCIM/Camera%, %Pictures/Screenshots%</code></li>
</ul>

## Planned for next releases 🗓️
<ul>
  <li>Automatic backup</li>
</ul>
