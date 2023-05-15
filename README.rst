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


The Safe Exam Browser Server web application simplifies and centralizes the configuration of Safe Exam Browser clients for exams. It interacts with a learning management or exam system for setting up and conducting e-assessments with Safe Exam Browser. It also improves security by allowing to monitor connected Safe Exam Browser clients in real time during e-assessments. 

What is Safe Exam Browser (SEB)?
--------------------------------

`Safe Exam Browser <https://safeexambrowser.org/>`_ (SEB) is an application to carry out e-assessments safely. The free-ware application is available for Windows, macOS and iOS. It turns any computer temporarily into a secure workstation. It controls access to resources like system functions, other websites and applications and prevents unauthorized resources being used during an exam. Safe Exam Browser can work with Open edX to control what a student can access during a Open edX quiz attempt. With the SEB Open edX plugin you activate the SEB support in Open edX and now only students using an approved version of SEB and the correct settings will be able to access the quiz in your Open edX course. The Safe Exam Browser is offered under a Mozilla Public License and supported by the `SEB Alliance <https://safeexambrowser.org/alliance/>`_.


What is Safe Exam Browser Server (SEB Server)?
----------------------------------------------

While the interaction with SEB is well known in Learning Management Systems (LMS) like `Open edX <https://open.edx.org/>`_, 
`Moodle <https://moodle.org/>`_ etc. the SEB Server is an entirely new component to set up secured online exams. 
It interacts with the assessments system/LMS as well as with SEB on exam clients.It supports exam scenarios on student owned devices (BYOD) 
and on managed devices.

SEB Server is a modern webservice with a REST API and a GUI service on top of it. SEB Server is written in Java and uses Docker for installation and setup.

SEB Server provides a range of basic functionalities:

- Built-in institutional multitenancy
- Linking of multiple Learning Management Systems (LMS). Currently supported LMS: `Open edX <https://open.edx.org/>`_, `Moodle <https://moodle.org/>`_, `Open Olat <https://www.openolat.com/>`_, `ANS <https://ans.app/>`_
- Accessing the Course/Exam-API of a linked LMS to import a courses or exams for managing with SEB Server
- Creation and administration of SEB Client Configurations that can be used to startup a SEB and that contains SEB Server connection information for a SEB Client
- Creation and administration of SEB Exam Configurations that can be bound to an imported Exam to automatically configure a SEB Client that connects to an exam that is managed by SEB Server
- Automated SEB restriction on LMS side if the specified type of LMS supports the SEB restriction API
- Monitoring and administration of SEB Client connections within a running exam

The image below shows a very simplified diagram that locates the SEB Server in a setup with a Learning Management System (LMS) and the 
Safe Exam Browser (SEB). The SEB Server communicates with the LMS for managing and prepare exams as well as with the SEB Client to ensure 
a more automated and secure setup for high-stake exams.

.. image:: https://raw.githubusercontent.com/SafeExamBrowser/seb-server/dev-1.5/docs/images/setup_1.png
    :align: center
    :target: https://raw.githubusercontent.com/SafeExamBrowser/seb-server/dev-1.5/docs/images/setup_1.png
    
SEB Server Version 1.5 is out
-------------------------------

New Features:

- Security: New Application Signature Key (ASK) integration within SEB Server exams and monitoring
- Security: Minimum SEB Client version tracking within SEB Server monitoring
- LMS Integration: Better Moodle integration with new `SEB Server Moodle Plugin <https://github.com/ethz-let/moodle-quizaccess_sebserver>`_
- Exam Maintenance: Added new SEB grouping functionality for Exam (and Exam Template) and Monitoring to be able to view/manage SEB Clients within defined groups (IP range, SEB client OS, ...) 
- Exam Maintenance: Batch actions for archive and delete exams
- Exam Maintenance: Added SEB log export for finished and archived exams
- Exam List: Filter for unavailable exams
- Exam Monitoring: Added force SEB Lock Screen feature to be able to send lock screen instruction to SEB client as well as release such from SEB Server
- User Account: Added "Change Password" function also in User Account edit page
    
Improvements:

- LMS Setup Lookup: Improved parallel data fetch of course and quit data from LMS and added notifications to the UI if background job is still fetching data from LMS in the background
- Zoom Proctoring: Adapted to new Zoom API's, SDK's and Apps
- Zoom Proctoring: Gallery view works now also in the proctoring web-client of the SEB Server
- Open Olat Integration: Added propagation of quit-link and quit-password for exam to Open Olat within the SEB restriction
- Monitoring: Improved performance for active monitoring
- Migration: Improved migration and added database table-char-set check
- SEB Settings: Added various new SEB Settings within the SEB Server database and Configuration Template (not yet in Exam Configuration UI)
- Added Tool-Tips also for list filters / various text and minor UI improvements
    
Bugfixes: 

- Exam Configuration status change to "Archived" is possible for up-coming exams
- Fix handling of invalid SEB Server monitoring UI sessions
- Open Olat LMS Setup access deadlock (serialized token request for LMS Template)
- Fixed exam update background process to update sometimes exams from LMS where nothing changed on LMS side
- Zoom proctoring multiplied participants on room change
- SEB Restriction warning on Exam seems to be not present when restriction fails
- Certificate cannot be imported
- Configuration Template: Filtering column "View": Paging in attribute list shows only one page
- Exam Configuration export SEB Settings should export current settings
- Exam: Name and Date filter does not work correctly
- Export Exam Connection Configuration, special characters in exam name cut of file name
- Zoom proctoring gallery view seems not to work because of cross-origin settings

Docker-Image:

- Exact release version: docker pull anhefti/seb-server:v1.5.0 (sha256:21d62e24dd5cf697ab5f2b437dc458e6c7492ea294f77a424d39d05164d6c8cc)
- Latest stable minor version with patches: docker pull anhefti/seb-server:v1.5-stable


SEB - SEB Server Compatibility
------------------------------

The table below shows available and upcoming SEB client versions that has SEB Server integration support and are compatible with particular 
SEB Server version. There is an entry for each platform with a beta or testing release date and a official release date.

**SEB Server Version 1.5.X**

.. csv-table::
   :header: "Platform / OS", "Beta/RC Version", "Release Version"

   "SEB Client for iOS", "ASK: 3.3", "3.1 (ASK: 3.3)"
   "SEB Client for Mac", "ASK: 3.3pre", "3.1 (Zoom: 3.2/ASK: 3.3)"
   "SEB Client for Windows", "--", "3.5.0 "

**SEB Server Version 1.4.X**

.. csv-table::
   :header: "Platform / OS", "Beta/RC Version", "Release Version"

   "SEB Client for iOS", "3.1 Beta", "3.1 (Zoom: 3.2) "
   "SEB Client for Mac", "3.1/3.2 Preview", "3.1 (Zoom: 3.2)"
   "SEB Client for Windows", "--", "Version 3.3.2 - 3.4.0"
   

Install SEB Server
------------------

For a complete guide to install SEB Server please go to `SEB Server Installation Guide <https://seb-server-setup.readthedocs.io/en/latest/overview.html>`_

Getting started with SEB Server
-------------------------------

For a complete SEB Server user guide please go to `SEB Server User Guide <https://seb-server.readthedocs.io/en/latest/#>`_

Project Background
------------------

The SEB Server is currently build and maintained by `ETH Zürich <https://ethz.ch/en.html>`_ and by the `Swiss MOOC Service <https://www.swissmooc.ch/>`_ that is founded by leading Swiss universities EPFL, ETH, SUPSI, USI and HES-SO. The Swiss MOOC Service was financially supported from 2018-2020 by the `Swissuniversities´ P5 program <https://www.swissuniversities.ch/themen/digitalisierung/p-5-wissenschaftliche-information>`_.
