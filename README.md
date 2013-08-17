AHA-Android_HTTPS_Analyser
==========================
AHA, the Android HTTPS Analyser, was developted during the lecture ACN at the IAIK, TU-Graz.

This application checks other application for their correct HTTPS handling.
2 easy steps for testin:
* Set up a wifi-hotspot and start AHA.
* Connect with the testing-phone and start the apps.

AHA will show you the result of how the app handled with the certificate.

##########################
ToDo:
* Make different attackes
** Fake the keys of the original certificate
** Don't use any certificate

* Identify the app
** We only know the tested port, not the app

* Forward the Requests
** We only check the handshake, let's forward the packages to the real server.
##########################

Contact:
* Sebastian Sommer [sebastian.sommer@student.tugraz.at]
* Simon Pöllitsch
* Ralph Ankele [ralph.ankele@tugraz.at]
* Robin Ankele
