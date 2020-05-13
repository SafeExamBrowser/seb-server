
.. image:: https://travis-ci.com/SafeExamBrowser/seb-server.svg?branch=master
    :target: https://travis-ci.com/SafeExamBrowser/seb-server
.. image:: https://codecov.io/gh/SafeExamBrowser/seb-server/branch/master/graph/badge.svg
    :target: https://codecov.io/gh/SafeExamBrowser/seb-server
.. image:: https://readthedocs.org/projects/seb-server/badge/?version=latest
    :target: https://seb-server.readthedocs.io/en/latest/?badge=latest
    :alt: Documentation Status

What is Safe Exam Browser (SEB)?
--------------------------------

`Safe Exam Browser <https://safeexambrowser.org/>`_ (SEB) is an application to carry out e-assessments safely. The freeware application is available for Windows, macOS and iOS. It turns any computer temporarily into a secure workstation. It controls access to resources like system functions, other websites and applications and prevents unauthorized resources being used during an exam. Safe Exam Browser can work with Open edX to control what a student can access during a Open edX quiz attempt. With the SEB Open edX plugin you activate the SEB support in Open edX and now only students using an approved version of SEB and the correct settings will be able to access the quiz in your Open edX course. The Safe Exam Browser is offered under a Mozilla Public License and supported by the `SEB Consortium <https://safeexambrowser.org/consortium/>`_.


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

SEB - SEB Server Compatibility
------------------------------

The table below shows available and upcoming SEB client versions that has SEB Server integration support and are compatible with particular 
SEB Server version. There is an entry for each platform with a beta or testing release date and a official release date.

**SEB Server Version 1.0.X**

.. csv-table::
   :header: "Platform / OS", "Beta/RC Version", "Release Version"

   "SEB Client for iOS", "22. April 2020 - Version 2.1.50", "Q2 2020 - Version 2.2"
   "SEB Client for Mac", "Q2 2020 - Version 2.2", "Q2 2020 - Version 2.2"
   "SEB Client for iOS", "Q2 2020 - Version 3.1", "Q3 2020 - Version 3.1"


Install SEB Server
------------------

For a complete guide to install SEB Server please go to `SEB Server Installation Guide <https://seb-server-setup.readthedocs.io/en/latest/overview.html>`_

Getting started with SEB Server
-------------------------------

For a complete SEB Server user guide please go to `SEB Server User Guide <https://seb-server.readthedocs.io/en/latest/#>`_

Project Background
------------------

The SEB Server is currently build and maintained by the `Swiss MOOC Service <https://www.swissmooc.ch/>`_ that is founded by leading Swiss universities EPFL, ETH, SUPSI, USI and HES-SO and is financially supported by the `Swissuniversities’ P5 program <https://www.swissuniversities.ch/themen/digitalisierung/p-5-wissenschaftliche-information>`_.
