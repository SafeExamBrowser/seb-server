Prepare Exam 
=============

This chapter is about setting up and prepare a already imported exam on SEB Server. If you don't have imported a course or quiz from LMS
as an exam into SEB Server, please see the previous chapter for detailed guidance about hot to import an exam.

To find a specific exam that has already been imported, go to "Exam Administration" / "Exam" on the navigation menu on the left hand side to
see a list of all available exams. You are able to filter and sort the list as usual to find to right course for import.

.. note::
    The "Start-Time" filter is usually set to the date one year before now or to some other default date in the past. The list shows only 
    the courses that has a start-time after that time. If you have long running courses and it may possible that a course has been stated
    a year or two ago, you habe to adapt this "Start-Time" filter to view those courses that has been started before. 
    
Double click on the list entry of the exam or select the list entry and use the "View Exam" action of the action pain on the right hand side to
open the exam in the detail view. Within the detail view of the exam you are able to edit the exam attribute, apply SEB exam configuration and
indicators for monitoring as well as defining details of the SEB restriction if this feature is available from the LMS.

.. note::
    If an exam is already running but is missing some essential setup, this is noted by the system. In the exam detail view the system
    displays a red framed message on the top of the page that points out the missing configuration parts. A running exam with missing 
    setup is also marked red in the lists to indicate that they are not ready to go and need some missing setup or preparation.

.. image:: images/exam/lmsExamLookup.png
    :align: center
    :target: https://raw.githubusercontent.com/SafeExamBrowser/seb-server/master/docs/images/exam/examNotReady.png
    
At the beginning of the page - if there is no note - you see all the details of the exam. 
    - Name: Is the name of the course, defines on the LMS
    - LMS Setup: Is the name of the LMS Setup on the SEB Server from which the course was imported
    - Start-Time: Is the date and time when the exam is starting. This is defined on the LMS
    - End-Time: Is the date and time when the exam ends. This is defined on the LMS
    - LMS Exam Identifier: Is the identity of the course on the LMS (external identifier/primary key)
    - LMS Exam URL: Is the start URL of the course/exam

To edit the SEB Server relevant attributes you can use
the "Edit Exam" action from the action pain on the right hand side to switch to the exam edit mode. In the exam edit mode you can modify
the type of the exam "Exam Type". The exam type has currently only informational character and has no implication on SEB Server side but may be 
used in the future to apply to different exam scenarios for SEB Server and SEB. Here you can also manage the accounts that shall be able to 
support and monitor the exam. To do so, use the multi combo selection within the "Exam Supporter" attribute. Click in the
input field of the attribute "Exam Supporter" to see a drop down list of all available users for selection. To filter the drop down list, start
typing characters of the name of the user-account you want to apply to automatically filter the list. Click on the drop-down list entry to select the
specified user that will be added to the list below the input field. To add another user to the selection just click again into the input field
and select another user. To remove a selected user from the selection-list, double-click on the specified entry of the selection-list.

Just below the exam details you will find the list of applied SEB exam configurations. Currently the number of SEB exam configurations that can be applied
to an exam is restricted to one since it is not possible to apply a SEB exam configuration not just to the exam but also to a specific user or a 
specific group of users. This feature may come with a future release of seb server. But for now you are able to apply just one SEB exam configuration 
the the exam and this gets delivered to all the SEB clients that connect to the SEB server and participate to the specified exam.




