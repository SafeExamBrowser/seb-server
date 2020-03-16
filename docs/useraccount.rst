User Accounts
=============

The user-account section within SEB Server can be used to create new or modify user-accounts for other user or to modify the own
user account and changing the password. This section differs most for the different roles in SEB Server.

A user account always belongs to one institution and has some basic attributes;

- Institution: A combo- or single-selection to choose the institution the user account belongs to. This is mandatory 
  and one user-account can only belong to one institution.
- First Name: The first name of the user
- Surname: The surname of the user
- Username: The username within SEB Server. This name is used to login.
- E-Mail: The E-Mail address for the user. This currently has only informational purpose and is neither used to confirm an account or to send automated mails.
- Time Zone: The time zone the user belongs to and to which the dates and times are converted to for display it to the user in the GUI. See also the note about time zone below.
- User Roles: A multi-selection input to define all roles a user account has. See also the note about roles below.
- Password: This password input field appears only while creating a new user-account or while self-register a user-account.
- Confirm Password: This password input field appears only while creating a new user-account or while self-register a user-account.

.. note:: 
      The date and time values within the SEB Server are always stored in universal time (UTC) and converted
      from and to the time zone a user account has defined. The time zone of the user account is usually labeled
      and the UTC time is shown below if possible.
      
.. note:: 
      The role selection is only displayed for user-accounts that has user-account modification privileges like
      SEB Server administrator and institutional administrator. Roles can be combined by selecting more then one role.
      For more information about roles and each role see the section :ref:`roles_and_usecases`

By selecting the "User Account" section on the left side menu, a SEB Server administrator will see a list of all user-accounts 
of all institution within a SEB Server instance. The filter above the list can be used to search a certain user account.

- Use the "Institution" filter to select a certain institution and show only the user-accounts that belongs to this institution.
- Use the "First Name" filter to search for user-accounts with the given occurrence of text in the First Name.
- Use the "User Name" filter to search for user-accounts with the given occurrence of text in the Username.
- Use the "Mail" filter to find an user-account by e-mail address
- 