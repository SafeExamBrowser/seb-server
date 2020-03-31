.. _exam-configuration-label:

Exam Configuration
==================

Overview
--------

An exam configuration defines all the SEB settings for an exam. A SEB client that is connecting to SEB Server
and an exam defined by the SEB Server is downloading this exam configuration from SEB Server and reconfigure itself before accessing the
exam on the learning management system (LMS) and present it to the user.

.. note::
    For more information and detailed description of the SEB exam settings, see `SEB Configuration <https://www.safeexambrowser.org/windows/win_usermanual_en.html#configuration>`_.
    Currently not all settings are available and some has different uses. For details about differences see :ref:`setting-dif-label` 

An exam administrator is able to create, modify and maintain exam configurations while the SEB Server administrator and the institutional administrator 
role have only read access either for all exam configurations or for the exam configurations of the institution. 
A exam supporter role is able to see and modify only the exam configurations to which the user-account is assigned as supporter.

To view the list of available exam configuration go the the sub-menu "Exam Configuration" within the menu "SEB Configurations" on the left
hand side. The list shows the name, the description and the status of the exam configurations in a column. With SEB Server administrator role
also an institution column is shown because a SEB Server administrator is able to see all exam configurations of all institutions.
As usual, to filter the list use the filter inputs above and click on the lens symbol on the right to apply the filter and to clear the 
filter use the clear symbol right to the lens symbol. See :ref:`gui-label` for more information about the list navigation. 

.. image:: images/exam_config/list.png
    :align: center
    :target: https://raw.githubusercontent.com/SafeExamBrowser/seb-server/master/docsexam_config/list.png
    
To view details of a specific exam configuration either double-click on a list entry or select a list entry and use the "View Exam Configuration"
action from the right action pane. In the detail view all general settings are shown and also a list of exams that uses this exam configuration.
In the current version of SEB Server, an exam configuration can only be assigned to one exam.

.. note:: 
    For the actual SEB Server version it is not possible to assign the same exam configuration to more then one exam. This because it shall be
    possible to change configuration settings for running exams when no active SEB clients are connected within the specified exam. This is 
    manageable for one exam but is going to become confusing if more exams are involved.
    But there is the possibility to copy an existing exam configuration to use the same for another exam.
    
The status of an exam configuration defines its visibility and assignment possibilities. If an exam configuration is still in the creation process 
and shall not be to an exam yet, it should stay in the "Under Construction" status.
This is the default status while creating a new exam configuration. Once a exam configuration is done and ready for assignment, one can change this
status to "Ready To Use". Exam configurations wihtin this status are available for selection and assignment on exams. When a exam configuration
is assigned to an exam the status changes automatically to "In Use" and the SEB settings of the exam configuration will appear only in read mode for default.

.. image:: images/exam_config/view.png
    :align: center
    :target: https://raw.githubusercontent.com/SafeExamBrowser/seb-server/master/docsexam_config/view.png

An exam configuration has a general settings part (like other domain objects has within SEB Server) that defines the name, description and status
of the exam configuration that are used to maintain the exam configurations SEB Server internally. And a exam configuration has, separated from 
the general settings, the SEB settings that contains most of the SEB setting attributes as they are provided by the SEB.

.. note::
    For more information and detailed description of the SEB setting attributes, see `SEB Configuration <https://www.safeexambrowser.org/windows/win_usermanual_en.html#configuration>`_.
    Currently not all settings are available and some has different uses. For details about differences see :ref:`setting-dif-label` 

The SEB settings differ from the general form-settings also in how they are managed and stored on server-side. Unlike the form-settings,
the SEB settings are arranged like they are in the SEB Configuration Tool for Windows and they are stored while modifying. This means, 
a entered value for an attribute is immediately sent to and stored by the SEB Server. There is no additional save action needed and 
no entered data will be lost. Instead the "Save / Publish Settings" can be used to store the current setting in a new revision in the history 
while also publish them to exams that uses this exam configuration.

.. image:: images/exam_config/settings.png
    :align: center
    :target: https://raw.githubusercontent.com/SafeExamBrowser/seb-server/master/docsexam_config/settings.png

.. note:: 
    Changes in SEB settings must be published to be available on exams they use this exam configuration. Before publishing they are not
    available for exams and SEB clients that connect to the SEB Server will still receive the last published version of the SEB settings.
    
.. note:: 
    Currently there is an "Undo" function to revert the changed made to the last published state. But there is no possibility yet to 
    maintain the publishing history of a Exam Configuration but may be available in a future release of the SEB Server.


Use Cases
---------

**Create new Exam Configuration**

For the upcoming semester you have to create several new exam configurations for the different kind of exams your institution is going to provide
during the semester.

- Sign into SEB Server with your exam administrator role account
- Navigate to the "Exam Configuration" menu within the SEB Configuration section on the left hand side.
- You see a list of all available exam configuration.
- Use the "Add Exam Configuration" action on the action pane on the right hand side to create a new exam configuration
- The creation form at least needs a unique name for the exam configuration. You can also add a description or hint to recognize and find this configuration more easily later on.
- In the "Status" field you are able to choose if the configuration is still under construction and cannot be added to exams yet, or if it is ready to use.
- Save the form with the "Save Exam Configuration" action at the right action pane to create the configuration.

**Edit SEB Settings and prepare for Use**

**Export an Exam Configuration**

**Import an Exam Configuration**

**Copy an Exam Configuration**

**Save an Exam Configuration as Template**

**Generate and Export the Config-Key of an Exam Configuration**

**Edit SEB Settings of an Exam Configuration in Use**


.. _setting-dif-label:

SEB Setting Differences
-----------------------

