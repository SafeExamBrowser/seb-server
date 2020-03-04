Overview
========

Safe Exam Browser Server (SEB Server) is a web-service application to help setting up and maintain e-assessments with `Safe Exam Browser <https://safeexambrowser.org/>`_ (SEB) in one place. The SEB Server application can be used by an institution or organization as a self-maintained, lightweight server application to organize and setup their e-assessments but can also run within a cloud infrastructure of a general service provider.

SEB Server consists of a web-service that provides a REST API and is able to run within one or many instances to scale up and a graphical user interface (GUI) server component that can either be integrated within the web-service or separately as a stand alone server application that connects to a defines web-service instance. SEB Server is written in Java and uses Docker for installation and setup. For more information about the architecture and installation, please go to `SEB Server Installation Guide <https://seb-server-setup.readthedocs.io/en/latest/overview.html>`_

For an organization or institution that provides safe e-assessments with `Safe Exam Browser <https://safeexambrowser.org/>`_, SEB Server can help to organize this e-assessments more effectively, safely and in one place.

There is the possibility to use the a SEB Server with built-in multi-tenancy functionality by creating institutions that are separated by each other. This is most convenient for smaller groups of organizations or institutions that want to have one self maintained SEB Server instance but need internal separation of institutions.

Since SEB Server is generally an administration tool, meaning generally used to do administration work and task to setup and maintain e-assessments, there is yet just a built-in small user-account management where users can register itself and get needed privileges assigned by already registered administrator users that has the privileges to maintain user accounts. Currently there is no possibility to register with a third party account and single sign on.

We will have a quick overview of the functionality and the roles on SEB Server in the next chapter.


Roles and Functionality
-----------------------

The SEB Server supports a simple role based privilege system where a role has defined read, modify and write privileges on certain domain entities. The privileges for a role are defined and cannot be changed by a user. Roles can be combined within one user-account.

Privileges for domain objects are categorized in read, modify and write where write includes creation and deletion grants in addition to the more restrict modify right that only allows to modify already created objects. They are also categorized in overall, institutional and owner privileges where overall means for all object, over all available institutions and institutional means only the object of the own institution and owner means the creator or assigned owner of an object.

Currently there are four roles that reflect a good separation of concerns within the SEB Server application administration itself and the setup and maintain for e-assessments with SEB.

SEB Server Administrator
^^^^^^^^^^^^^^^^^^^^^^^^

This role is primarily to administer the SEB Server application, create new and maintain institutions and user-accounts. In addition to that a SEB Server administrator also has overall read privileges for the most parts to be able to analyze problems and help others to solve them.

A typical use-case for a SEB Server administrator is to create a new institution and an institutional administrator for this institution.

Institutional Administrator
^^^^^^^^^^^^^^^^^^^^^^^^^^^^