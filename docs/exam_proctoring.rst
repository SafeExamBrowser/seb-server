.. _sebProctoringSettings-label:
Exam Proctoring
---------------

If this feature is enabled, you are able to setup a proctoring service for an specified exam that can be used in the monitoring later on while the exam is running and SEB clients are connected.

.. note::
    This feature is still in a prototype state and not all functionality meight work as expected.
    
To be able to use proctoring features within SEB Server you need a meeting service with scale. Currently supported is `Jitsi Meet <https://jitsi.org/jitsi-meet/>`_ with JWT token authentication enabled.
A `Zoom meeting service <https://zoom.us/>`_ integration is planed for a future release of SEB Server.
To setup and configure a Jitsi Meet service for testing you can refer to the `Docker installation documentation <https://jitsi.github.io/handbook/docs/devops-guide/devops-guide-docker>`_

To setup a proctoring service for an exam, go to the view page of the exam and use the "Proctoring Settings" action on the right action pain to open up the proctoring settings dialog.

.. image:: images/exam/proctoringSettings.png
    :align: center
    :target: https://raw.githubusercontent.com/SafeExamBrowser/seb-server/master/docs/images/exam/proctoringSettings.png
    
Within the proctoring settings dialog you are able switch the service on and off by using the "Proctoring enabled" checkbox. You also see if the proctoring service is enabled within 
the action icon that is either an eye when active or a slashed eye when not active.
To define and bind a service you have to chose a service type from the available service types. And you need to give the API access credentials like "Application Key" and "Secret" for
the external meeting service. SEB Server will then try to connect to the meeting service with these credentials to automatically create meetings/rooms for proctoring.
Within the "Collecting Room Size" field you can define the number of maximal participants that shall be collected within one proctoring room. SEB Server will automatically
create these collecting rooms while SEB clients are connecting to the running exam in the monitoring view.

After you have all the settings set, use "OK" to confirm the settings. SEB Server will then try to connect to the meeting service with the given settings and check the access.

Another part of proctoring settings can be found in the "Exam Configuration" "SEB Settings". There is a new tab with the name "Proctoring" where all SEB settings for proctoring are available.
These settings are directly used by a SEB client that supports the proctoring feature. 
    
.. image:: images/exam/proctoringSEBSettings.png
    :align: center
    :target: https://raw.githubusercontent.com/SafeExamBrowser/seb-server/master/docs/images/exam/proctoringSEBSettings.png


