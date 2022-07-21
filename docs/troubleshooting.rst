Troubleshooting
===============

This page's purpose is to collect frequently ask questions, problems and workarounds that are reported and known. 
There shall be at least a problem description, an optional explanation if needed, a solution description and the SEB Server version and domain information. For Example:

Please also have a look at `Open Issues <https://https://github.com/SafeExamBrowser/seb-server/issues>`_ and/or `Ongoing Discussions <https://github.com/SafeExamBrowser/seb-server/discussions>`_ on the Git-Hub page.

--------------------------------

- **Version**     : 1.3.x

- **Domain**      : Exam Monitoring

- **Problem**     : SEB connections get lost and ping-times go up for already connected SEB clients

- **Explanation** : This issue is due to a access token used by SEB client to authenticate on SEB Server that lasts not longer the one hour since SEB Server version 1.3 and since SEB client has no new access token request implements yet.

- **Solution**    : A workaround for SEB Server version 1.3.x is to make the access token expiry-date last long enough to minimize the possibility that the access token became invalid during a exam. We recommend to set it to 12 hours = 43200 seconds. Therefore please set the following  SEB Server setup properties in the respective application-prod.properties configuration file of your SEB Server setup:

    sebserver.webservice.api.admin.accessTokenValiditySeconds=43200
    sebserver.webservice.api.exam.accessTokenValiditySeconds=43200

In SEB Server version 1.4 this is already set as default again and we are currently working on new SEB client versions that also 
handle SEB Server communication token expiry by automatically requesting a new access token (with new lifetime) from SEB Server
when an old access token is not valid any longer.

--------------------------------


**Template**
--------------------------------

- **Version**     : 1.0.0
- **Domain**      : Exam Administration / Exam / Indicator
- **Problem**     : This is an example problem description.
- **Explanation** : An explanation is not mandatory but shall be provided if you want to give some background information to the problem.
- **Solution**    : A description of the solution, workaround or upcoming fixes.

--------------------------------
