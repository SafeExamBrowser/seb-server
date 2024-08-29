Exam Test-Run (since version 2.0)
--------------------------------

With the new SEB Server version 2.0 there is a new feature Test-Run for none running / upcoming exams. Since upcoming
Exams on SEB Server are not available for SEB connections and Monitoring one have to has to change the course start date
to apply testing beforehand of an Exam.Server

With new new Rest-Run feature it is now possible to change an Exam in upcoming status to a dedicated Test-Run status where
SEB clients are able to connect and SEB Server Exam Administrator or Supporter are able to Monitor the Exam as long a
the Exam stays within this Test-Run status. 

You can set an Exam into Test-Run status by using the respective action on the Exam view:

.. image:: images/exam/examWithURLView.png
    :align: center
    :target: https://raw.githubusercontent.com/SafeExamBrowser/seb-server/dev-2.0/docs/images/exam/examEnableTestrun.png

After enable Test-Run for an Exam, you can see an information at the top if the Exam view that informs you about the 
Test Run status of the Exam.

After testing is done you can disable the Test-Run status just by using the respective action on the right and the Exam
goes back to up-coming status and is not available anymore for SEB connections and Monitoring.

.. image:: images/exam/examWithURLView.png
    :align: center
    :target: https://raw.githubusercontent.com/SafeExamBrowser/seb-server/dev-2.0/docs/images/exam/examEnableTestrun.png

.. note:: 
    If an Exam changes to Running status due to the start time passing and is still in status Test-Run, SEB Sever will
    automatically change the Exam from Test-Run to Running status and all features of a running Exam are available. 