.. _lms-setup-label:

Assessment Tool Setup
================================

Overview
--------

To be able to connect to a learning management system (Assessment Tool), to view and manage the courses provided by a Assessment Tool is an essential feature of the SEB Server.
To setup an exam or e-assessment for SEB on SEB Server that is based on a course from a Assessment Tool, we have to make a binding to the course on the Assessment Tool.
This is also used to always get the actual course data from Assessment Tool like start- end-time, name and others.
Another feature of SEB Server that needs a Assessment Tool communication is the SEB restriction. A SEB restriction will restrict course access on the Assessment Tool only
for connection with Safe Exam Browser and will also check if a Safe Exam Browser of trust is used and the right configuration is used by the
Safe Exam Browser that was defines for the exam on the SEB Server.

**Course API** 

This API, provided by the Assessment Tool, is used by the SEB Server to query the available courses and the needed data for each course. This API
is needed to be able to import a course from the Assessment Tool as an exam into SEB Server and configure the course as an e-assessment with SEB.
Usually this API comes as a REST or SOAP API with the core Assessment Tool implementation or a plugin.

**SEB Restriction API** 

If the automated SEB restriction functionality is available for a Assessment Tool depends on the following requirements:

- There must exist a SEB integration plugin that offers an API to put and pull SEB restrictions in the form of Config-Keys and/or Browser-Exam-Keys
  To the Assessment Tool and a specific course on the Assessment Tool to restrict the access. Such a plugin may also offer additional restriction features like restricting
  on course section or course components or only for specified user roles.
- The SEB integration plugin must be installed on the Assessment Tool that is used by the SEB Server.

For more information about known SEB integration plugins that are supported by the SEB Server see the respective Assessment Tool integration section.

Regardless if a supported Assessment Tool is missing the SEB integration plugin installation, the Assessment Tool can be used with the Course API and an exam
setup will be possible but without automated SEB restriction feature.

To be able to connect to an Assessment Tool from SEB Server, we need to create an API access account on the Assessment Tool side that can be used by the SEB Server to
access the API of the Assessment Tool. How to do this for the different supported types of Assessment Tool see :ref:`lms-api-account-label`.
After such an account was created, the account credentials, username and password, can be used by the SEB Server to connect to the Assessment Tool.
Therefore we need to create a Assessment Tool Setup on the SEB Server.

.. image:: images/lmssetup/new.png
    :align: center
    :target: https://raw.githubusercontent.com/SafeExamBrowser/seb-server/master/docs/images/lmssetup/new.png
    
A SEB Server administrator role will be able to see the institution to which the Assessment Tool Setup belongs to while an institutional administrator
is only able to see and create Assessment Tool Setup for its own institution. The name of the Assessment Tool Setup should be unique and is to identify a Assessment Tool
SEB Server internally. Use the **"Type"** selector to specify the type of the Assessment Tool to bind to the SEB Server within the Assessment Tool Setup. Currently supported are:

- **Testing**: This is for testing purposes only and can be used to mock a Assessment Tool to test exam settings. This type provides some mock-up courses within the
  Assessment Tool API of the SEB Server that can be seen in the Assessment Tool Exam Lookup once the Assessment Tool text setup is active. This mock-up courses can be imported and configured
  as exams like they would exist. But note the a SEB client that is trying to connect to such a course would not be able to connect to the Assessment Tool since it
  is not existing. But a SEB client is able to download the defined exam configuration for testing.

- **Open edX**: This type is to bind an existing `Open edX <https://open.edx.org/>`_ Assessment Tool system that is available on the Internet or intranet.
- **Moodle**: This type is to bind an existing `Moodle <https://moodle.org//>`_ Assessment Tool system that is available on the Internet or intranet.
- **Moodle with SEB Server Plugin**: This type is to bind SEB Server with an Moodle Assessment Tool that has SEB Server Plugin for Moodle <https://github.com/ethz-let/moodle-quizzaccess_sebserver>`_ installed. 
- **Ans Delft**: This type is to bind SEB Server with an `Ans Delft <https://ans.app/>`_ Assessment Tool.
- **OLAT**: This type is to bind SEB Server with an `OLAT <https://www.olat.org/>`_ Assessment Tool.

The **"Assessment Tool Server Address"** is the root URL to connect to the Assessment Tool server with HTTP over the Internet or intranet. This is usually the URL that is
also used with the Browser to connect to the main page of the Assessment Tool system. And additionally the credentials that have been created with the creation of the :ref:`lms-api-account-label` has to be set in the Assessment Tool Setup the make the SEB Server
able to securely connect to the Assessment Tool. The API credentials that consists of a client-name and a client-secret must be used with the **"Assessment Tool Server Username"**
and the **"Assessment Tool Server Password"** fields of the Assessment Tool Setup form on SEB Server.

Alternatively to **"Assessment Tool Server Username"** and **"Assessment Tool Server Password"** you can use a direct **Access Token** to connect to the Assessment Tool API if the respective Assessment Tool allows to
generate and use an access token directly.

If the SEB Server running behind a proxy server or a firewall between SEB Server den Assessment Tool, the additional proxy settings can be used to setup the proxy-connection.

.. note:: 
    To Setup a Test Assessment Tool Setup (of type "Test") only a correct URL pattern must be set like "http://test" for example. And API credentials can be anything but must be set.

After all the settings for a Assessment Tool Setup have been set, one can use either the "Save Assessment Tool Setup" action to save the Assessment Tool Setup without activation or the
"Activate Assessment Tool Setup" action to also activate the settings right after they has been successfully saved. Anyway, for both action there is an initial test
that, additionally to the usual field validation that takes place first, tries to connect to the Assessment Tool with the given API details. If the connection
wasn't successful, the SEB Server will inform the user about a possible reason of failure. Otherwise SEB Server shows a success message and the created
Assessment Tool Setup can be used.

Use the "Activate / Deactivate Assessment Tool Setup" action to activate an inactive Assessment Tool Setup or the deactivate an active Assessment Tool Setup.

.. note:: 
    On deactivation of an Assessment Tool Setup, the system checks on depending object and will show a confirmation to the user asking that all depending
    objects will also been deactivated. Depending objects of an Assessment Tool Setup are exams that has been imported from the specified Assessment Tool Setup in the past.
    
For detailed information for each Assessment Tool type and how to bind and use it with SEB Server, please refer to the respective section:

.. toctree::
   :maxdepth: 1
   
   lms_moodle
   lms_edx
   lms_olat
   lms_ans



Use Cases
---------

**Deactivate Assessment Tool Setup**

A Assessment Tool system that was running on your campus to provide e-assessment with SEB and SEB Server has been shut down and you need to also deactivate
the setup and exams on the SEB Server for this Assessment Tool.

- Sign into SEB Server with your institutional administrator role account.
- Navigate to "Exam Administration" / "Assessment Tool Setup" within the navigation on the left hand side.
- Use the Filter above the list to find the specified Assessment Tool Setup.
- Select the specified Assessment Tool Setup from the list and use the "Deactivate Assessment Tool Setup" action from the right action pane.
- Alternatively you can also double-click on the Assessment Tool Setup to fist go into the detailed view of the Assessment Tool setup and use the "Deactivate Assessment Tool Setup" action there.
- The system informs you about the number of depending exams that also will be deactivated within the deactivation of the Assessment Tool Setup.
- Confirm the deactivation and notify that the Assessment Tool Setup now is listed as "Inactive" in the list.
- Navigate to "Assessment Tool Exam Lookup" to make sure the courses form the deactivated Assessment Tool Setup are not available anymore.
- Navigate also to "Exam" and make sure that all previously imported exams from the deactivated Assessment Tool Setup are not available anymore.
