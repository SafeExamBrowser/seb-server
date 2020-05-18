Exam Monitoring
===============

A key feature of the SEB Server is the ability to overview and monitor SEB clients that are connected to the SEB Server and participate in a running exam.
It is also possible to interact with individual SEB clients that are connected and send them instructions, e.g.: to quit and reload for example.
But the main focus lies on monitoring and the SEB Server don't take automated actions but always only shows incidents as they are defined by
indicators to a exam supporter. A exam supporter then can take action.

As an exam supporter you see all running exams where you have been assigned to by an exam administrator in a list. 

.. image:: images/monitoring/runningExams.png
    :align: center
    :target: https://raw.githubusercontent.com/SafeExamBrowser/seb-server/master/docs/images/monitoring/runningExams.png
    
To navigate to this list click on the "Running Exams" menu within the "Monitoring" category. And you will see the list with the name of the
exam, the type and start- and end-time of the exam. If the exam you looking for is not on the list, please check the start-time filter 
and make sure that it is set before the start-time of the exam. If not, use the filter to set the date before the exams start-date and
use the lens-symbol on the right to apply the new filter. If the exam you are looking for is still not on the list, please contact a 
exam administrator of your institution to get help and check if you are correctly assigned to the exam as an exam supporter.

To monitor an exam either double-click on the list entry of the exam or select the list entry and use the "Monitoring" action form the
right action pane to navigate into the main page of exam monitoring.

Within the the main page of exam monitoring you see a list of all SEB client connections that has been connected to the exam since the
exam is running. There are some filter switches on the right action pane that may hide some canceled and/or closed SEB connections but
you should see at least all active connections instantly.

.. image:: images/monitoring/examMonitoring.png
    :align: center
    :target: https://raw.githubusercontent.com/SafeExamBrowser/seb-server/master/docs/images/monitoring/examMonitoring.png
