SEB Configurations
==================

There are two different kind of SEB specific configurations that must be defined and be put in place to support e-assessments with
SEB and SEB Server. The  is used to configure SEB with all needed information to securely connect to SEB Server
and an Exam Configuration is used to configure SEB to access a running exam with SEB. The  is only needed at least
once for an institution and must be deployed as start-configuration on a managed SEB setup or can be placed as a download link within
an internal trusted server, that automatically loads within the SEB application and configures the SEB to connect to the SEB Server.

An exam configuration, as the name implies, is done for an exam and contains most of the known configuration attributes
from the latest `SEB Configuration Tool <https://www.safeexambrowser.org/windows/win_usermanual_en.html#configuration>`_
Currently an exam configuration can only be applied to one Exam but can easily be copied to use with another exam. For detailed
information about exam configuration see :ref:`exam-configuration-label`

Within Configuration Templates, it is possible to create templates for exam configuration. Within the current version of SEB Server
there is the possibility to define different default value(s) for each exam configuration attribute and also to define if the attribute
is been shown in the exam configuration or not. This allows an Exam Administrator to create a exam configuration template for exams
with context defines default values and also to be able to only see change the attributes that are relevant for an exam configuration.
This feature is currently in an experimental state and may be changed and / or expanded within future releases of SEB Server. See 
:ref:`config-template-label`

An new feature since SEB Server version 1.2 is the integrated certificate store where an administator is able to upload and register
certificates. The certificates can then be used to encrypt and secure a connection configuration for example. Or as planed for another
SEB Server release, to embed into an exam configuration for SEB to allow certificate pinning on SEB - LMS communication.


.. toctree::
   :maxdepth: 1
   
   connection_config
   exam_config
   config_template
   certificates