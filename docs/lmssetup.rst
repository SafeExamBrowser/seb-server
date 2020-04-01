.. _lms-setup-label:

Leraning Management System Setup
================================

Overview
--------

To be able to connect to a learning management system (LMS) and view and use the courses provided by a LMS is an essential feature of the SEB Server.
To define an exam or e-assessment and set it up for the  use with SEB we use some data of a course that is running on an LMS like identifier, 
start- end-time, name and others. If we furthermore want to be able to automatically restrict the course for SEB access only we need to have proper
integration API's in place on both sides, the LMS and the SEB Server. This integration is separated into two main features so far called:

**Course API** 

This API, provided by the LMS, is used by the SEB Server to query the available courses and the needed data for each course. This API
is essential to be able to import a course from the LMS as an exam into SEB Server and configure the course as an e-assessment with SEB.
Usually this API comes as a REST or SOAP API with the core LMS implementation. For the Open edX system `this <https://courses.edx.org/api-docs/>`_ 
standard API is used and for the Moodle LMS `this <https://docs.moodle.org/dev/Web_service_API_functions>`_ standard API is used 
by the SEB Server to query courses

**SEB restriction API** 

Usually this are REST API's 


Use Cases
---------

**Create a new LMS Setup for Open edX**

**Create a new LMS Setup for Moodle**




Create API Account on LMS
--------------------------

**Open edX**


Install SEB restriction API plugin
----------------------------------

**Open edX**

