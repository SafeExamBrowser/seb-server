Safe Exam Browser (SEB) Server
--------------------------------

.. image:: https://github.com/SafeExamBrowser/seb-server/actions/workflows/buildReporting.yml/badge.svg?branch=master
    :target: https://github.com/SafeExamBrowser/seb-server/actions
.. image:: https://readthedocs.org/projects/seb-server/badge/?version=latest
    :target: https://seb-server.readthedocs.io/en/latest/?badge=latest
.. image:: https://img.shields.io/github/languages/code-size/SafeExamBrowser/seb-server
    :target: https://github.com/SafeExamBrowser/seb-server



The Safe Exam Browser Server web application simplifies and centralizes the configuration of Safe Exam Browser clients for exams. It interacts with a learning management or exam system for setting up and conducting e-assessments with Safe Exam Browser. It also improves security by allowing to monitor connected Safe Exam Browser clients in real time during e-assessments. 

`SEB Server On Github <https://github.com/SafeExamBrowser/seb-server>`_

What is Safe Exam Browser (SEB)?
--------------------------------

`Safe Exam Browser <https://safeexambrowser.org/>`_ (SEB) is an application to carry out e-assessments safely. The free-ware application is available for Windows, macOS and iOS. It turns any computer temporarily into a secure workstation. It controls access to resources like system functions, other websites and applications and prevents unauthorized resources being used during an exam. Safe Exam Browser can work with Open edX to control what a student can access during a Open edX quiz attempt. With the SEB Open edX plugin you activate the SEB support in Open edX and now only students using an approved version of SEB and the correct settings will be able to access the quiz in your Open edX course. The Safe Exam Browser is offered under a Mozilla Public License and supported by the `SEB Alliance <https://safeexambrowser.org/alliance/>`_.


What is Safe Exam Browser Server (SEB Server)?
----------------------------------------------

While the interaction with SEB is well known in Learning Management Systems (LMS) like `Open edX <https://open.edx.org/>`_, 
`Moodle <https://moodle.org/>`_ etc. the SEB Server is an entirely new component to set up secured online exams. 
It interacts with the assessments system/LMS as well as with SEB on exam clients. It supports exam scenarios on student owned devices (BYOD) 
and on managed devices.

SEB Server is a modern webservice with a REST API and a GUI service on top of it. SEB Server is written in Java and uses Docker for installation and setup.

SEB Server provides a range of basic functionalities:

- Built-in institutional multitenancy
- Linking of multiple Learning Management Systems (LMS). Currently supported LMS: `Open edX <https://open.edx.org/>`_, `Moodle <https://moodle.org/>`_, `OLAT <https://www.olat.org/>`_, `ANS <https://ans.app/>`_
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
    
SEB Server Version 2.1 is out
-------------------------------

Within this new version 2.1 we implemented mostly new features for Screen Proctoring and make SEB Server and Screen Proctoring 
more stable. We also stabilized the SEB Server Moodle Plugin integration for SEB Server Moodle Plugin Version 2.

For a migration from a running SEB Server setup with version 1.x to new major version 2.0, please also read the `migration guide <https://seb-server-setup.readthedocs.io/en/latest/major-migration.html>`_.

New Features:
    - SEB Server - Screen Proctoring: Client group application for screen proctoring groups. Client groups applied in an exam can now be applied to screen proctoring to have the same groups.
    - Screen Proctoring: Deletion of screen proctoring sessions for admin users
    - Screen Proctoring: Application View search   

Improvements:
   - SEB Server - SEB Settings: All new SEB Settings from SEB Win, SEB Mac and SEB iOS now in SEB Settings GUI
   - SEB Server - Screen Proctoring: Apply Screen Proctoring to SEB Settings from Exam. When screen proctoring is enabled/disables on Exam, it is also enabled/disabled in the applied SEB Settings for SEB
   - SEB Server - Screen Proctoring: Stabilise Screen Proctoring Feature and make SPS API Connection more stable
   - SEB Server - Setup: Simplify Spring profiles
   - SEB Server - Moodle Integration: Stabilise Moodle Plugin 2 Integration
   - SEB Server - Moodle Integration: Make Teacher Accounts not selectable as Exam supporter
   - SEB Server - Error Handling: Improve error message and logs in SEB Server
   - SEB Server - ASK: ASK Monitoring: Make clearer note when SEB do not send an ASK 
   - Screen Proctoring: Update to latest Spring Boot version 3.3.2
   - Screen Proctoring: Add limit to Screenshot Queue to prevent out of memory
   - Screen Proctoring: Make sure Exam Admin and Institution Admin see only supported Exams on SPS

Bugfixes:
   - SEB Server - Moodle Integration: LMS Moodle external_id should not be greater than 255 chars
   - SEB Server - Moodle Integration: Delete Teacher users when exam is deleted or assessment tool setup is deactivated
   - SEB Server - Moodle Integration: using twice the same quiz name in a Moodle Course seems not working
   - SEB Server - Monitoring: Notification symbol does not appear when locking a client from the list view
   - Screen Proctoring: Prevent queue overflow on metadata store error (due to to long meta data for example)
   - Screen Proctoring: Fix RDBMS and S3 store strategy to avoid multiple entries in screenshot_data table when retrying to store screenshot
   - Screen Proctoring: S3 Storage: HTTP Connection leak on abandoned image requests (fast time line shift on recorder for example)
   - Screen Proctoring: URLEncoded metadata in Header has problem with spaces replaces by '+'
   - Screen Proctoring: Can't use screen proctoring with OLAT due to OLAT exam identifier


SEB - SEB Server Compatibility
------------------------------

The table below shows available and upcoming SEB client versions that has SEB Server integration support and are compatible with particular 
SEB Server version. There is an entry for each platform with a beta or testing release date and a official release date.

**SEB Server Version 2.1.x**

.. csv-table::
   :header: "Platform / OS", "Release Version"

   "SEB Client for iOS", "3.5"
   "SEB Client for Mac", "3.5"
   "SEB Client for Windows", "3.9"

Install SEB Server
------------------

For a complete guide to install SEB Server please go to `SEB Server Installation Guide <https://seb-server-setup.readthedocs.io/en/latest/overview.html>`_

Getting started with SEB Server
-------------------------------

For a complete SEB Server user guide please go to `SEB Server User Guide <https://seb-server.readthedocs.io/en/latest/#>`_


Project Background
------------------

The SEB Server is currently build and maintained by `ETH Zürich <https://ethz.ch/en.html>`_ and by the `Swiss MOOC Service <https://www.swissmooc.ch/>`_ that is founded by leading Swiss universities EPFL, ETH, SUPSI, USI and HES-SO. The Swiss MOOC Service was financially supported from 2018-2020 by the `Swissuniversities´ P5 program <https://www.swissuniversities.ch/themen/digitalisierung/p-5-wissenschaftliche-information>`_.

Contributing to SEB Server
---------------------------

We want to make contributing to this project as easy and transparent as possible, whether it's:

- Give us a star
- Reporting a bug
- Submitting a fix
- Proposing new features
- Becoming a SEB Alliance member

We use github to host code, to track issues and feature requests, as well as accept pull requests.
And we use `Github issues <https://github.com/SafeExamBrowser/seb-server/issues>`_ to track public bugs.
Report a bug by [opening a new issue]();

**Before enter a new bug-report, ensure the bug was not already reported**

Please fill and provide all the information suggested by the bug-report template
Great Bug Reports tend to have:

- A quick summary and/or background
- Steps to reproduce
- Be specific and give sample code if you can. Can also be Pseudocode.
- What you expected would happen
- What actually happens
- Notes (possibly including why you think this might be happening, or stuff you tried that didn't work)

**We Use Git-Flow for Code Contributions**

Pull requests are the best way to propose changes to the codebase. We use `Github Flow <https://www.atlassian.com/git/tutorials/comparing-workflows/gitflow-workflow>`_. We actively welcome your pull requests:

1. Fork the repo and create your branch from `development`. The development branch always has the newest changes.
2. If you've added code that should be tested, add tests.
3. If you introduce new API also add clear documentation.
4. Ensure the test suite passes.
5. Make sure your code lints.
6. Issue that pull request!

**Use a Consistent Coding Style**

Have a close look to the existing code stile that is used within SEB Server and adapt to it as close as possible.
We reserve the right to adapt contributed code to the code style matching SEB Server code style before or after a pull request.

**Any contributions you make will be under the Mozilla Public License Version 2.0**

In short, when you submit code changes, your submissions are understood to be under the same `Mozilla Public License <https://github.com/SafeExamBrowser/seb-server?tab=MPL-2.0-1-ov-file>`_ that covers the project. Feel free to contact the maintainers if that's a concern.

**Becoming a SEB Alliance member**

The `SEB Alliance <https://www.safeexambrowser.org/alliance/members.html>`_ is the body which sustains ongoing funding of the Safe Exam Browser open source project to continue its maintenance, development and support activities. ETH Zurich provides the infrastructure for the management and the software engineering of the SEB project and appoints an alliance manager who will provide administrative support to the SEB Alliance, and ensure the day-to-day running of the SEB Alliance. ETH Zurich leads the Alliance and offers different contribution levels to parties interested in the evolution of the SEB open source project.

More information about `joining <https://www.safeexambrowser.org/alliance/join.html>`_ the Alliance is available in our `benefits <https://www.safeexambrowser.org/alliance/benefits.html>`_ and `documents <https://www.safeexambrowser.org/alliance/documents.html>`_ section.

