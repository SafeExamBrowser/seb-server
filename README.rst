Master: 

.. image:: https://github.com/SafeExamBrowser/seb-server/actions/workflows/buildReporting.yml/badge.svg?branch=master
    :target: https://github.com/SafeExamBrowser/seb-server/actions
.. image:: https://readthedocs.org/projects/seb-server/badge/?version=latest
    :target: https://seb-server.readthedocs.io/en/latest/?badge=latest
.. image:: https://codecov.io/gh/SafeExamBrowser/seb-server/branch/master/graph/badge.svg
    :target: https://codecov.io/gh/SafeExamBrowser/seb-server
.. image:: https://img.shields.io/github/languages/code-size/SafeExamBrowser/seb-server
    :target: https://github.com/SafeExamBrowser/seb-server

Development:

.. image:: https://github.com/SafeExamBrowser/seb-server/actions/workflows/buildReporting.yml/badge.svg?branch=development
    :target: https://github.com/SafeExamBrowser/seb-server/actions
.. image:: https://codecov.io/gh/SafeExamBrowser/seb-server/branch/development/graph/badge.svg
    :target: https://codecov.io/gh/SafeExamBrowser/seb-server
.. image:: https://img.shields.io/github/last-commit/SafeExamBrowser/seb-server/development?logo=github
    :target: https://github.com/SafeExamBrowser/seb-server/tree/development


---------

**Note regarding to** `CVE-2021-44228 <https://nvd.nist.gov/vuln/detail/CVE-2021-44228>`_: SEB Server is **not affected** by the vulnerability. For more information please read `Log4J2 Vulnerability and Spring Boot <https://spring.io/blog/2021/12/10/log4j2-vulnerability-and-spring-boot>`_

However, to prevent security scanner to alert false-positives we decided to make a patch for the latest version of SEB Server (v1.2.6) including the Log4j 2.16.0 library. If you want to update please make sure your installation refer to version v1.2-stable, v1.2-latest or v1.2.6. Then simply make a update/rebuild of your seb-server docker image.

---------

About
-----
The Safe Exam Browser Server web application simplifies and centralizes the configuration of Safe Exam Browser clients for exams. It interacts with a learning management or exam system for setting up and conducting e-assessments with Safe Exam Browser. It also improves security by allowing to monitor connected Safe Exam Browser clients in real time during e-assessments. 

What is Safe Exam Browser (SEB)?
--------------------------------

`Safe Exam Browser <https://safeexambrowser.org/>`_ (SEB) is an application to carry out e-assessments safely. The freeware application is available for Windows, macOS and iOS. It turns any computer temporarily into a secure workstation. It controls access to resources like system functions, other websites and applications and prevents unauthorized resources being used during an exam. Safe Exam Browser can work with Open edX to control what a student can access during a Open edX quiz attempt. With the SEB Open edX plugin you activate the SEB support in Open edX and now only students using an approved version of SEB and the correct settings will be able to access the quiz in your Open edX course. The Safe Exam Browser is offered under a Mozilla Public License and supported by the `SEB Alliance <https://safeexambrowser.org/alliance/>`_.


What is Safe Exam Browser Server (SEB Server)?
----------------------------------------------

While the interaction with SEB is well known in Learning Management Systems (LMS) like `Open edX <https://open.edx.org/>`_, 
`Moodle <https://moodle.org/>`_ etc. the SEB Server is an entirely new component to set up secured online exams. 
It interacts with the assessments system/LMS as well as with SEB on exam clients.It supports exam scenarios on student owned devices (BYOD) 
and on managed devices.

SEB Server is a modern webservice with a REST API and a GUI service on top of it. SEB Server is written in Java and uses Docker for installation and setup.

SEB Server provides a range of basic functionalities:

- Built-in institutional multitenancy 
- Linking of multiple Learning Management Systems (LMS). Currently supported: `Open edX <https://open.edx.org/>`_
- Accessing the Course/Exam-API of a linked LMS to import a courses or exams for managing with SEB Server
- Creation and administration of SEB Client Configurations that can be used to startup a SEB and that contains SEB Server connection information for a SEB Client
- Creation and administration of SEB Exam Configurations that can be bound to an imported Exam to automatically configure a SEB Client that connects to an exam that is managed by SEB Server
- Automated SEB restriction on LMS side if the specified type of LMS supports the SEB restriction API
- Monitoring and administration of SEB Client connections within a running exam

The image below shows a very simplified diagram that locates the SEB Server in a setup with a Learning Management System (LMS) and the 
Safe Exam Browser (SEB). The SEB Server communicates with the LMS for managing and prepare exams as well as with the SEB Client to ensure 
a more automated and secure setup for high-stake exams.

.. image:: https://raw.githubusercontent.com/SafeExamBrowser/seb-server/master/docs/images/seb-sebserver-lms.png
    :align: center
    :target: https://raw.githubusercontent.com/SafeExamBrowser/seb-server/master/docs/images/seb-sebserver-lms.png
    

SEB Server Version 1.4 is out
-------------------------------

New features:

- Add possibility to archive/deactivate exams
- Added "Monitoring/Finished Exams" to be able to view already finished or archived exams within the usual monitoring view
- Improve LMS connection handling and LMS data fetch cycle
- Exam Configuration: Batch actions to change "Status" and "Update from Template" on multiple configuration at once
- Add proctoring settings to exam template
- Import of certificate-encrypted SEB configuration as Exam Configuration in SEB Server
- Added last update date and last modifying user to Connection Configuration and Exam Configuration
- Deletion possibility for all entities or documents with detailed deletion report
- Show generated client credentials of a Client Configuration. Suitable for testing purposes


Bugfixes:

- Fixed to short default expiry time for SEB access token (1 hour --> 12 hour) 
- Fixed Exam status update for running or finished exams when LMS dates changes
- Fixed Exam name and start-date filter
- Caching for SEB Exam Configuration on distributed setups
- Wrong SEB client missing ping logs generated by SEB Server
- Monitoring: Negative ping values
- Monitoring - Raise hand: Icon in SEB Server monitoring appears now also when SEB is connecting and not active yet
- Exam Configuration - List-Inputs: AutomaticallysSelect new added entry and select next entry after deleting previous entry


Changes:

- User Account: Improved description for change password form
- Monitoring: Search pop-up width to fit full navigation
- Exam Configuration: Rename option "Enable SEB with browser windows" to "Use SEB without browser window" and invert logic (same as in SEB Config Tool)
- Exam Proctoring Settings: Make pop-up larger to fit all settings
- Client Configuration - Fallback Password: Improved arrangement for better TAB handling 
- Improve form validation in add indicator form
- Change order of actions in monitoring according to user-input
- Configuration Templates: Display actual configuration values in the template's attribute list


Docker-Image:

- Exact release version: docker pull anhefti/seb-server:v1.4.0 (sha256:a2510c4bd31fa99e553bc672087f1c0276f5541452490e5320a77767bb8c1849)
- Latest stable minor version with patches: docker pull anhefti/seb-server:v1.4-latest



SEB - SEB Server Compatibility
------------------------------

The table below shows available and upcoming SEB client versions that has SEB Server integration support and are compatible with particular 
SEB Server version. There is an entry for each platform with a beta or testing release date and a official release date.

**SEB Server Version 1.4.X**

.. csv-table::
   :header: "Platform / OS", "Beta/RC Version", "Release Version"

   "SEB Client for iOS", "3.1 Beta", "3.1 (Zoom: 3.2) "
   "SEB Client for Mac", "3.1/3.2 Preview", "3.1 (Zoom: 3.2)"
   "SEB Client for Windows", "--", "Version 3.3.2 - 3.4.0"
   
**SEB Server Version 1.3.X**

.. csv-table::
   :header: "Platform / OS", "Beta/RC Version", "Release Version"

   "SEB Client for iOS", "3.1 Beta", "3.1 (Zoom: 3.2) "
   "SEB Client for Mac", "3.1/3.2 Preview", "3.1 (Zoom: 3.2)"
   "SEB Client for Windows", "--", "Version 3.3.2"

**SEB Server Version 1.2.X**

.. csv-table::
   :header: "Platform / OS", "Beta/RC Version", "Release Version"

   "SEB Client for iOS", "--", "Version 3.0.1 "
   "SEB Client for Mac", "3.1 Preview", "Version 3.1"
   "SEB Client for Windows", "--", "Version 3.2"
   
**SEB Server Version 1.1.X**

.. csv-table::
   :header: "Platform / OS", "Beta/RC Version", "Release Version"

   "SEB Client for iOS", "22. April 2020 - Version 2.1.50", "Q2 2021 - Version 2.5/3.0"
   "SEB Client for Mac", "Q2 2021 - Version 3.1", "Q2 2021 - Version 3.1"
   "SEB Client for Windows", "April 2021 - Version 3.2", "May 2021 - Version 3.2"
   
**SEB Server Version 1.0.X**

.. csv-table::
   :header: "Platform / OS", "Beta/RC Version", "Release Version"

   "SEB Client for iOS", "22. April 2020 - Version 2.1.50", "Q2 2021 - Version 2.5/3.0"
   "SEB Client for Mac", "Q2 2021 - Version 3.1", "Q2 2021 - Version 3.1"
   "SEB Client for Windows", "Q2 2020 - Version 3.1", "Q4 2020 - Version 3.1"
   

Install SEB Server
------------------

For a complete guide to install SEB Server please go to `SEB Server Installation Guide <https://seb-server-setup.readthedocs.io/en/latest/overview.html>`_

Getting started with SEB Server
-------------------------------

For a complete SEB Server user guide please go to `SEB Server User Guide <https://seb-server.readthedocs.io/en/latest/#>`_

Project Background
------------------

The SEB Server is currently build and maintained by the `Swiss MOOC Service <https://www.swissmooc.ch/>`_ that is founded by leading Swiss universities EPFL, ETH, SUPSI, USI and HES-SO. The Swiss MOOC Service was financially supported from 2018-2020 by the `Swissuniversities´ P5 program <https://www.swissuniversities.ch/themen/digitalisierung/p-5-wissenschaftliche-information>`_.

