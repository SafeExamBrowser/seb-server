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


Apply SEB Exam Configuration
----------------------------

Just below the exam details you will find the list of applied SEB exam configurations. Currently the number of SEB exam configurations that can be applied
to an exam is restricted to one since it is not yet possible to apply a SEB exam configuration not just to the exam but also to a specific user or a 
specific group of users. This feature may come with a future release of seb server. But for now you are able to apply just one SEB exam configuration 
to the exam and this configuration gets delivered to all the SEB clients that connect to the SEB server and participate to the exam.

To apply a SEB exam configuration to an exam use the "Add Exam Configuration" action on the right action pane. A pop-up will prompt you to select an
SEB exam configuration by a drop-down selection. The drop-down box will present you all SEB exam configurations that are in state "Ready To Use" and
that are not already used by another exam. If there are more SEB exam configurations in the selection as fitting into the drop-down box, you can either
scroll the content of the drop-down box or start typing the name of SEB exam configuration to filter the list. After selecting a SEB exam configuration, 
the pop-up shows the description and the status of the selected configuration. 

.. note::
    If there are no SEB exam configurations available for applying, the application will note this within a pop-up message.
    In this case you can create a new one for this exam as described in :ref:`seb_restriction-label`

.. image:: images/exam/lmsExamLookup.png
    :align: center
    :target: https://raw.githubusercontent.com/SafeExamBrowser/seb-server/master/docs/images/exam/addExamConfig.png
    
There is also the possibility to encrypt the SEB exam configuration by a password. This is done before the SEB configuration is send to a connecting 
SEB client. Since in the moste cases the SEB client connections are over HTTPS / TSL there is alrady a good protection in place and an 
additional encryption of the SEB exam configuration is not recommended. Anyways, if you decide the use the additional password encryption, the SEB 
client that receives the encrypted SEB exam configuration will prompt the user for the password to proceed and connect to the LMS.

.. note::
    If you need to use the same SEB exam configuration for different exams, you can just make a copy of a SEB exam configuration that is already used
    by another exam. 
    - To do so go the the "Exam Configuration" section and find the specified SEB exam configuration by using the filter. If you have
    found the SEB exam configuration double-click on the table entry to open the SEB exam configuration. Then use the "Copy Exam Configuration" action
    from the right action pane. A pop-up will prompt you the give a new name and optionally description for the copy. Click "OK" and the system will
    generate a copy of the original SEB exam configuration with the new name and description and will lead you to the details page of the copy configuration.

Click the "OK" button on the pop-up to apply the selected SEB exam configuration. You will see the applied SEB exam configuration in the list.
If the automated SEB restriction feature is supported by the LMS of the exam, the application of a SEB exam configuration will automatically update
the SEB restriction details with the generated Config-Key. See :ref:`seb_restriction-label` for more information.

.. image:: images/exam/lmsExamLookup.png
    :align: center
    :target: https://raw.githubusercontent.com/SafeExamBrowser/seb-server/master/docs/images/exam/examWithConfig.png
    
When a SEB exam configuration is applied to the exam you are able to export the SEB exam configuration XML in plain text or the generated Config-Key for
testing purposes. Just select the SEB exam configuration in the list to activate the action on the action pain on the right side and use the
appropriate action. The SEB exam configuration export action will instruct the browser to open a download dialog. How the download is applied is up the 
the browser of use. Usually you are able to either save the file or open it up with a application. If you have already installed the Safe Exam Browser on
your device, the browser probably associates the download file already with the SEB client application.

To just generate the Config-Key for testing purposes or to manually apply it on an LMS without the automated SEB restriction feature you can
use the "Export Config-Key" action to generate the key. The Config-Key is presented by a pop-up and can be selected and copied to the clip-board.
For more information about the Config-Key its purpose and use, please visit the `SEB documentation <https://www.safeexambrowser.org/developer/seb-config-key.html>`_.

To remove an already applied SEB exam configuration from the exam, select the SEB exam configuration in the table and use the "Delete Exam Configuration"
action form the right action pane. If the automated SEB restriction feature is supported by the LMS of the exam, the removal of a SEB exam configuration will 
automatically update the SEB restriction details and remove the Config-Key form the restriction details. See :ref:`seb_restriction-label` for more information.
Once you have removed a SEB exam configuration from the exam you are able to apply another one to the exam.

You can navigate directly to the SEB exam configuration details page by double-click on the table entry of the SEB exam configuration. You will then 
see the details of the SEB exam configuration as well as a table of exams where this SEB exam configuration is used. Since for now it is only possible
to apply one SEB exam configuration to one exam, there is only one entry and you can quickly navigate back to the exam be also double-click on the
table entry of the exam.


Apply Indicators
-----------------

For monitoring connected SEB clients, SEB server supports some indicators that can be defines within an exam and that are shown and measured during an
exam. When you import, the application automatically creates a default ping-indicator for the exam. You are able to modify this default indicator and also
add some other indicators to the exam to be able to get notified while monitoring a exam session.

The type of indicators are pre-defined within the SEB Server and restricts the number of available indicators that can be used. Currently following
indicators are supported:

 - Last Ping Time: This indicator measures the time that has passed since the last ping from a specified SEB client was received by the SEB Server in milliseconds.
   This can be used to monitor constant SEB client connections and get notified when a SEB client connection gets lost for some defined 
   time or when a SEB client connection that has been list is back again.
   This indicator is used as default indicator and automatically applied to the exam on import. You are able to modify this indicator 
   and also delete it but we recommend to have this basic indicator in place for every exam.
                   
 - Errors: This indicator measures the number of error-logs a specified SEB client is sending to the SEB Server.
 - Warnings: This indicator measures the number of warning-logs a specified SEB client is sending to the SEB Server.
 
You can define thresholds for each indicator. A threshold is defined by an indication-color and by a threshold-value. On the monitoring side, the 
indicator for each SEB client connection with change to the threshold color when the measured indicator value has reached the threshold-value.

To add a new indicator to the exam you can use the "Add Indicator" action from the right action pane. In the indicator edit page you are able to give the
new indicator a name. This name will be displayed in the SEB client connection table on monitoring section as column name for the column of this
indicator. Then you are able to choose a "Type" that specifies the type of indicator. Choose this from a drop-down selection of supported indicators.
If you have selected one indicator type, a description will be shown just below the "Type" attribute. 

You can define a "Default Color" for the indicator. An indicator which measured value has not reached any defines threshold will be shown in this 
color on the SEB client connection table of the monitoring section.

.. note::
    To select a color click on the brush-palette icon of the color input field to open up a color chooser pop-up window. Within the color chooser
    you can select one of the basic colors provided by the chooser or by defining the red, green and blue part of the color.

.. image:: images/exam/lmsExamLookup.png
    :align: center
    :target: https://raw.githubusercontent.com/SafeExamBrowser/seb-server/master/docs/images/exam/editIndicator.png
    

Below the default color you see a list of thresholds. Use the plus sign icon to add a new threshold to the list and on an existing threshold use
the minus sign icon to remove a particular threshold form the list. A threshold has a threshold-value and a threshold-color. The threshold value
must be set in the format of the measured indicator value that is described within the type description. This is usually an integer or floating-point
number. The color for each threshold can be set using the color chooser tool by clicking on the brush-palette icon on the right side of each threshold.
The color chooser pop-up is the same as for the default color.

.. note::
    In the monitoring section when the exam is running, an indicator will be shown within a column with given name of the indicator in the SEB connection 
    table. For each connection the measured indicator value will be displayed in the cell of the individual SEB client connection rows. If the measured 
    indicator value for a particular SEB client reaches a defined threshold, the cell will be displayed in the defined color of the threshold.


.. _seb_restriction-label:
Automated SEB restriction
--------------------------

If the LMS and the LMS integration of an exam supports the automated SEB restriction feature, the SEB restriction 

Use Cases
---------

**Apply or remove exam supporter**

**Change the type of exam**

**Apply a SEB exam configuration**

**Remove a SEB exam configuration**

**Add an indicator**

**Modify indicators**

**Apply automated SEB restriction**










