# Anonymous Peer-to-Peer Map App for Android

This is a proof of concept peer-to-peer map application for sharing geolocation alerts – similar to Waze's or Google Maps' reporting feature. Users can connect to a peer-to-peer network and place alerts on a map, such as road hazards, speed traps and other warnings. 

All conections are routed through the TOR network to provide privacy. 

Initial messaging framework was forked from the [ourbook](https://github.com/onionApps/ourbook) project.

## Features

* Native Android Java App
* Google Maps API integration
* User can share geolocation alerts with timestamp and street address
* Included TOR binary built from source with updated OpenSSL
* Peer-to-peer mesh network, allowing for functionality independent of dedicated server
* SQLite for local storage

## Screenshots

![Map screen](/screenshots/sc1.png) ![Map screen](/screenshots/sc2.png) ![Map screen](/screenshots/sc3.png)
