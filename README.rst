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
    

SEB Server Version 1.3.0 is out
-------------------------------

New features:

- Table Filter: Enter input on filter input field triggers filter action
- New: "Exam Templates" to predefine exam configurations that can be applied on exam import
- Exam Configuration: Copy exam configuration on exam configuration table view
- Exam Templates: Add an exam configuration template via exam template to automatically create an exam configuration on import
- Exam Templates: Add indicators for exam template that are automatically applied on exam import
- Monitoring: Add new filter to filter active connections (without any incidences)
- Monitoring: Add connection summary for each connection state filter to show how many connections are present per state
- Monitoring: Improved and extended connection information about user/login change and display client info like SEB version, OS Version...
- Monitoring: Improved distributed setup with Docker/Kubernetes
- Monitoring Notifications: Added Raise-Hand Notification and SEB Lock-Screen Notification


Bugfixes:

- Various distributed setup cache issues
- Fixed Sorting of exam in exam list
- Fixed LMS Lockup quizzes filter
- Fixed Exam proctoring settings verification
- Various proctoring issues for proctoring with Zoom


Changes:

- Overall: Extended GUI server session timeout
- User Roles: Enhanced "Exam Administrator" role to see all running exams and be able to support them as well
- LMS Lockup/Exam: Show Moodle course name together with the quiz name on LMS lockup as well as on exams
- Exam Configuration: Streamline "Exam Configuration" settings with the newest SEB versions
- Monitoring: Improved indicator and monitoring data performance for distributed setups
- Monitoring: Changed default colors for active connections and indicators (No color if no incidence)

Docker-Image:

    Exact version: docker pull anhefti/seb-server:v1.3.0 (sha256:35692e304ab8f7d198524ff948df472e1eb362f1eb7f0b0fa358d01556011e59)
    Latest stable minor version with patches: docker pull anhefti/seb-server:v1.3-latest



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

