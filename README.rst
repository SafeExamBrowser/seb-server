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
    
SEB Server Version 1.1.0 is out
-------------------------------

New features:

- Moodle integration part 1 (Course Access)
- Live proctoring with Jitsi Meet integration (Prototype)
- Deletion of user accounts
- Deletion of exams
- User registration rate limit
- Inform user about unpublished chances in exam configuration settings
- Added "Ignore SEB Service" attributes in exam configuration settings
- Additional monitoring indicator (WIFI and battery status)
- Notification events (experimental)
- Download/Export Connection Configuration from Exam page to start an exam directly "Export Exam Connection Configuration"

Bugfixes:

- Monitoring table update, incorrect table rendering after status changed
- Remove caching for distributed setup
- Add lookup and automated master service allocation for distributed setup

Changes:

- "Client Configuration" is now named "Connection Configuration"
- "Export Exam Configuration" is now named "Export SEB Settings" and can be found in the "SEB Settings" view of an "Exam Configuration"

SEB Server Version 1.2.0 is out
-------------------------------

New features:

- Integrated X.509 certificate store to upload and use X.509 certificate for new and upcoming features
- Connection configuration encryption with X.509 certificate
- Additional exam configuration attributes of later added features of the SEB config-tool
- Default sorting and filtering for all lists
- Deleting of SEB client logs on the SEB Client Logs view
- Zoom meeting service integration for live proctoring (this is still an experimental feature)
- Ability to switch live proctoring features like town-hall, one-to-one room or broadcasting, on and off

Bugfixes:

- Exam configuration import gives more and clear information about the purpose of different imports
- Color picker is now initialized with selected color
- Fixed user-account deactivation on user-account list
- Fixed indicator list on exam shows only up to five entries
- Fixed none scrolling action pane
- Fixed exam import of Moodle LMS integration with different quizzes on same course
- Various bug-fixes and improvements for distributed setup of SEB Server (separated and scaled webservice and guiservice)

Changes:

- Updated MariaDB version for integrated setups from version 10.3 to 10.5
- Updated Spring Boot version from 2.1.0.RELEASE to 2.3.7.RELEASE
- Build pipeline automatically build the SEB Server docker image and put it to docker hub now
- New SEB Server docker setup (dockerhost) that pulls the needed images from docker-hub


SEB - SEB Server Compatibility
------------------------------

The table below shows available and upcoming SEB client versions that has SEB Server integration support and are compatible with particular 
SEB Server version. There is an entry for each platform with a beta or testing release date and a official release date.

**SEB Server Version 1.0.X**

.. csv-table::
   :header: "Platform / OS", "Beta/RC Version", "Release Version"

   "SEB Client for iOS", "22. April 2020 - Version 2.1.50", "Q2 2021 - Version 2.5/3.0"
   "SEB Client for Mac", "Q2 2021 - Version 3.1", "Q2 2021 - Version 3.1"
   "SEB Client for Windows", "Q2 2020 - Version 3.1", "Q4 2020 - Version 3.1"
   
**SEB Server Version 1.1.X**

.. csv-table::
   :header: "Platform / OS", "Beta/RC Version", "Release Version"

   "SEB Client for iOS", "22. April 2020 - Version 2.1.50", "Q2 2021 - Version 2.5/3.0"
   "SEB Client for Mac", "Q2 2021 - Version 3.1", "Q2 2021 - Version 3.1"
   "SEB Client for Windows", "April 2021 - Version 3.2", "May 2021 - Version 3.2"
   

Install SEB Server
------------------

For a complete guide to install SEB Server please go to `SEB Server Installation Guide <https://seb-server-setup.readthedocs.io/en/latest/overview.html>`_

Getting started with SEB Server
-------------------------------

For a complete SEB Server user guide please go to `SEB Server User Guide <https://seb-server.readthedocs.io/en/latest/#>`_

Project Background
------------------

The SEB Server is currently build and maintained by the `Swiss MOOC Service <https://www.swissmooc.ch/>`_ that is founded by leading Swiss universities EPFL, ETH, SUPSI, USI and HES-SO. The Swiss MOOC Service was financially supported from 2018-2020 by the `Swissuniversities´ P5 program <https://www.swissuniversities.ch/themen/digitalisierung/p-5-wissenschaftliche-information>`_.

