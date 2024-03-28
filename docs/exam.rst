Prepare Exam 
=============

This chapter is about setting up and prepare a already imported exam on SEB Server. If you don't have imported a course or quiz from LMS/Assessment Tool
as an exam into SEB Server, please see the previous chapter for detailed guidance about hot to import an exam.

To find a specific exam that has already been imported, go to "Exam Administration" / "Exam" on the navigation menu on the left hand side to
see a list of all available exams. You are able to filter and sort the list as usual to find to right course for import.

.. note::
    The Date-Filter above "Start-Time" is usually set to the date one year before now or to some other default date in the past
    and is applied to the end-date of the exam or quiz. The list shows all running or up-coming exams and only hides finished or
    archived exams that has an end-date before the Date-Filter date. 
    
Double click on the list entry of the exam or select the list entry and use the "View Exam" action of the action pane on the right hand side to
open the exam in the detail view. Within the detail view of the exam you are able to edit the exam attributes, apply SEB exam configuration and
indicators for monitoring as well as defining details of the SEB restriction if this feature is available from the LMS/Assessment Tool.

.. note::
    If an exam is already running but is missing some essential setup, this is noted by the system. In the exam detail view the system
    displays a red framed message on the top of the page that points out the missing configuration parts. A running exam with missing 
    setup is also marked red in the lists to indicate that they are not ready to go and need some missing setup or preparation.

.. image:: images/exam/examNotReady.png
    :align: center
    :target: https://raw.githubusercontent.com/SafeExamBrowser/seb-server/master/docs/images/exam/examNotReady.png
    

At the beginning of the page - if there is no note - you see all the details of the exam. 

- **Name**: Is the name of the course, defines on the LMS/Assessment Tool
- **LMS/Assessment Tool Setup**: Is the name of the LMS/Assessment Tool Setup on the SEB Server from which the course was imported
- **Start-Time**: Is the date and time when the exam is starting. This is defined on the LMS/Assessment Tool
- **End-Time**: Is the date and time when the exam ends. This is defined on the LMS/Assessment Tool
- **LMS/Assessment Tool Exam Identifier**: Is the identity of the course on the LMS/Assessment Tool (external identifier/primary key)
- **LMS/Assessment Tool Exam URL**: Is the start URL of the course/exam

To edit the SEB Server relevant attributes you can use
the "Edit Exam" action from the action pane on the right hand side to switch to the exam edit mode. In the exam edit mode you can modify
the type of the exam "Exam Type". The exam type has currently only informational character and has no implication on SEB Server side but may be 
used in the future to apply to different exam scenarios for SEB Server and SEB. Here you can also manage the accounts that shall be able to 
support and monitor the exam. To do so, use the multi combo selection within the "Exam Supporter" attribute. Click in the
input field of the attribute "Exam Supporter" to see a drop down list of all available users for selection. To filter the drop down list, start
typing characters of the name of the user-account you want to apply to automatically filter the list. Click on the drop-down list entry to select the
specified user that will be added to the list below the input field. To add another user to the selection just click again into the input field
and select another user. To remove a selected user from the selection-list, double-click on the specified entry of the selection-list.


Apply SEB Exam Configuration
----------------------------

Just below the exam details you will find the list of applied SEB exam configurations. Currently the number of SEB exam configurations that can be applied
to an exam is restricted to one since it is not yet possible to apply a SEB exam configuration not just to the exam but also to a specific user or a 
specific group of users. This feature may come with a future release of seb server. But for now you are able to apply just one SEB exam configuration 
to the exam and this configuration gets delivered to all the SEB clients that connect to the SEB Server and participate to the exam.

.. image:: images/exam/examConfigAdd.png
    :align: center
    :target: https://raw.githubusercontent.com/SafeExamBrowser/seb-server/master/docs/images/exam/examConfigAdd.png

To apply a SEB exam configuration to an exam use the "Add Exam Configuration" action on the right action pane. A pop-up will prompt you to select an
SEB exam configuration by a drop-down selection. The drop-down box will present you all SEB exam configurations that are in state "Ready To Use" and
that are not already used by another exam. If there are more SEB exam configurations in the selection as fitting into the drop-down box, you can either
scroll the content of the drop-down box or start typing the name of SEB exam configuration to filter the list. After selecting a SEB exam configuration, 
the pop-up shows the description and the status of the selected configuration. 

.. note::
    If there are no SEB exam configurations available for applying, the application will note this within a pop-up message.
    In this case you can create a new one for this exam as described in :ref:`sebRestriction-label`

.. image:: images/exam/addExamConfig.png
    :align: center
    :target: https://raw.githubusercontent.com/SafeExamBrowser/seb-server/master/docs/images/exam/addExamConfig.png
    
There is also the possibility to encrypt the SEB exam configuration by a password. This is done before the SEB configuration is send to a connecting 
SEB client. Since in the most cases the SEB client connections are over HTTPS / TSL there is already a good protection in place and an
additional encryption of the SEB exam configuration is not recommended. Anyway, if you decide the use the additional password encryption, the SEB 
client that receives the encrypted SEB exam configuration will prompt the user for the password to proceed and connect to the LMS/Assessment Tool.

With SEB Server Version 1.6 it is possible to change the Exam Configuration encryption password of an applied Exam Configuration.
The new action "Edit Encryption Password" can be used to open up the original apply dialog and to change the password:

.. note::
    The Encryption Password for applied Exam Configuration can only be changes when there are no active SEB clients available for the exam.

.. image:: images/exam/editEncryptionPassword.png
    :align: center
    :target: https://raw.githubusercontent.com/SafeExamBrowser/seb-server/master/docs/images/exam/editEncryptionPassword.png

.. note::
    If you need to use the same SEB exam configuration for different exams, you can just make a copy of a SEB exam configuration that is already used
    by another exam. 
    - To do so go the the "Exam Configuration" section and find the specified SEB exam configuration by using the filter. If you have
    found the SEB exam configuration double-click on the table entry to open the SEB exam configuration. Then use the "Copy Exam Configuration" action
    from the right action pane. A pop-up will prompt you the give a new name and optionally description for the copy. Click "OK" and the system will
    generate a copy of the original SEB exam configuration with the new name and description and will lead you to the details page of the copy configuration.

Click the "OK" button on the pop-up to apply the selected SEB exam configuration. You will see the applied SEB exam configuration in the list.
If the automated SEB restriction feature is supported by the LMS/Assessment Tool of the exam, the application of a SEB exam configuration will automatically update
the SEB restriction details with the generated Config-Key. See :ref:`sebRestriction-label` for more information.

.. image:: images/exam/examConfig.png
    :align: center
    :target: https://raw.githubusercontent.com/SafeExamBrowser/seb-server/master/docs/images/exam/examConfig.png
    
To just generate the Config-Key for testing purposes or to manually apply it on an LMS/Assessment Tool without the automated SEB restriction feature you can
use the "Export Config-Key" action to generate the key. The Config-Key is presented by a pop-up and can be selected and copied to the clip-board.
For more information about the Config-Key its purpose and use, please visit the `SEB documentation <https://www.safeexambrowser.org/developer/seb-config-key.html>`_.

To remove an already applied SEB exam configuration from the exam, select the SEB exam configuration in the table and use the "Delete Exam Configuration"
action form the right action pane. If the automated SEB restriction feature is supported by the LMS/Assessment Tool of the exam, the removal of a SEB exam configuration will
automatically update the SEB restriction details and remove the Config-Key form the restriction details. See :ref:`sebRestriction-label` for more information.
Once you have removed a SEB exam configuration from the exam you are able to apply another one to the exam.

You can navigate directly to the SEB exam configuration details page by double-click on the table entry of the SEB exam configuration. You will then 
see the details of the SEB exam configuration as well as a table of exams where this SEB exam configuration is used. Since for now it is only possible
to apply one SEB exam configuration to one exam, there is only one entry and you can quickly navigate back to the exam be also double-click on the
table entry of the exam.


Use Cases
---------

**Apply or remove exam supporter**

Exam supporter "Eric" was planed to support the upcoming exam but he cancelled for reasons of illness and exam supporter "Anie" will take over.
Therefore you have to remove Eric from the exams supporter list while also adding Anie to the list. So she will be able to support the running exam.

- Login as an exam administrator and go to the "Exam" page under the "Exam Administration" section.
- Use the filter to find the exam on that you have to change the supporter assignments. 
- Double click the list entry of the exam to go to the exam details page. Check if you are on the right exam.
- Use the "Edit Exam" action form the right action pane to go into the exam edit page.
- Find Eric's user account on the list of selected exam supporter and use the minus sign icon on the entry to remove Eric from the list of exam supporter
- Click into the input field of the exam supporter selector and start typing the name of Anie's account. The drop down will present you all matching entries. Select Anie's account to add it to the list of selected exam supporter.
- Use the "Save Exam" action on the right action pane to save and confirm the task. This will lead you also back to the exam details page.
- Check again if all exam supporter are correctly assigned for the exam.

**Change the type of exam**

Although the exam type has just informative character for now and did not affect the exam in any other way, you have to change the type because
your institution use the type information of the exam to set them into context.

- Login as an exam administrator and go to the "Exam" page under the "Exam Administration" section.
- Use the filter to find the exam on that you have to change the supporter assignments. 
- Double click the list entry of the exam to go to the exam details page. Check if you are on the right exam.
- Use the "Edit Exam" action form the right action pane to go into the exam edit page.
- Use the drop down selection to change the type of the exam.
- Use the "Save Exam" action on the right action pane to save and confirm the task. This will lead you also back to the exam details page.

**Apply a SEB exam configuration**

- Login as an exam administrator and go to the "Exam" page under the "Exam Administration" section.
- Use the filter to find the exam on that you have to change the supporter assignments. 
- Double click the list entry of the exam to go to the exam details page. Check if you are on the right exam.
- If the exam has already one exam configuration attached you have first to delete this attachment before being able to attach another exam configuration. Use the "Delete Configuration" action from the right action pane to remove the attached exam configuration.
- Use the "Add Exam Configuration" action form the right action pane to open up the attachment dialog.
- If there is currently no exam configuration that can be attached to the exam, an information dialog will be shown instead of the attachment dialog.
- On the attachment dialog use the drop down selection to select the exam configuration you want to apply to the exam. The drop down selection shows the names of the available exam configurations and you can filter this names by start typing the name of the exam configuration you want to find in the input field of the selection.
- When you have selected a exam configuration the dialog shows you some additional information about the exam configuration. 
- If you want or need to put an password protected encryption to the exam configuration for this exam you can do so by give the password for the encryption also within the attachment dialog. Be aware that every SEB client that will receive an encrypted exam configuration from the SEB Server will prompt the user to give the correct password. In most cases an encryption of the exam configuration is not needed, because a secure HTTPS connection form SEB client to SEB Server is already in place.

**Archive an exam**

Since SEB Server version 1.4 it is possible to archive an exam that has been finished. An archived exam and all its data is still available
on the SEB Server but read only and the exam is not been updated from the LMS/Assessment Tool data anymore and it is not possible to run this exam again.

This is a good use-case to organize your exams since archived exam are not shown in the Exam list with the default filter anymore. They are
only shown if the status filter of the exam list is explicitly set to Archived status. An they are shown within the new "Finished Exam"
section in the monitoring view.

.. image:: images/exam/archiveExamsFilter.png
    :align: center
    :target: https://raw.githubusercontent.com/SafeExamBrowser/seb-server/master/docs/images/exam/archiveExamsFilter.png

This is also a good use-case if you want to remove an LMS/Assessment Tool and LMS/Assessment Tool Setup but still want to be able to access the exams data on the SEB Server.
In this case you can archive all exams from that LMS/Assessment Tool Setup before deactivating or deleting the respective LMS/Assessment Tool Setup.

To archive a finished exam you just have to use the "Archive Exam" action on the right action pane of the exam view:

.. image:: images/exam/archiveExam1.png
    :align: center
    :target: https://raw.githubusercontent.com/SafeExamBrowser/seb-server/master/docs/images/exam/archiveExam1.png


**Delete an exam**

If you have "Exam Administrator" privileges you are able to entirely delete an existing exam and its dependencies. 

.. note::
    Please be aware that deletion in this context means a fully removal of the data. The data will be lost and not recoverable.

- Login as an exam administrator and go to the "Exam" page under the "Exam Administration" section.
- Use the filter to find the exam on that you have to delete. 
- Double click the list entry of the exam to go to the exam details page. Check if you are on the right exam.
- Use the "Delete" action on the right action pane to open a deletion dialog.

.. image:: images/exam/deleteExam.png
    :align: center
    :target: https://raw.githubusercontent.com/SafeExamBrowser/seb-server/master/docs/images/exam/deleteExam.png
    
- Within the delete exam dialog you see a list of a dependencies that also will be deleted. Please check them carefully before deletion.
- Use the below action to either delete the exam or cancel the action and go back to the exam view.

