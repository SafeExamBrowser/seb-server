Screen Proctoring
===========

Overview
---------

SEB Screen Proctoring is an integral component of the SEB Ecosystem, designed to monitor student screens during digital examinations.
This tool captures and displays screenshots taken by SEB, ensuring a secure and controlled testing environment.

Enable Screen Proctoring after an exam is created in the exam view.

.. image:: images/screen_proctoring/enable_screen_proctoring.png
    :align: center
    :target: https://raw.githubusercontent.com/SafeExamBrowser/seb-server/docu/docs/images/screen_proctoring/enable_screen_proctoring.png

**Enable Screen Proctoring**


SEB Server needs to send an instructions to SEB to capture the screen and send the screenshot back to the server.
To do this enable screen proctoring in the SEB Settings.

.. image:: images/screen_proctoring/enable_screen_proctoring_seb_settings.png
    :align: center
    :target: https://raw.githubusercontent.com/SafeExamBrowser/seb-server/docu/docs/images/screen_proctoring/enable_screen_proctoring_seb_settings.png

**Enable Screen Proctoring in SEB Settings**


SEB-Server and SEB are now ready to capture and to display screenshots.
Navigate to exam monitoring and click the button in the Screen Proctoring section. A new tab will be opened.

.. image:: images/screen_proctoring/open_screen_proctoring.png
    :align: center
    :target: https://raw.githubusercontent.com/SafeExamBrowser/seb-server/docu/docs/images/screen_proctoring/open_screen_proctoring.png


Gallery View
---------

- When a SEB is connected to SEB-Server and the user logged in to LMS a tile with the latest screenshot will appear.
- Press the "Grid-Size"-Drop-Down to change the displayable sessions per screen to 4, 9 or 16.

- Use the arrow buttons and the left and right on the screen to change windows. The current page and the amount of live sessions / total sessions is display next to the grid selection.


.. image:: images/screen_proctoring/gallery_view_live_grid_selection.png
    :align: center
    :target: https://raw.githubusercontent.com/SafeExamBrowser/seb-server/docu/docs/images/screen_proctoring/gallery_view_live_grid_selection.png


- Use your mouse to hover over a tile or press the tab key. A "selected" tile reveals information and actions for the session.
- To change view preferences press the settings icon in the top right corner.
- Toggling "Show Name" and "Show IP" reveals the the Name and IP of the selected session.
- Per default all the sessions are sorted by lastname in ascending order. To change this press the "Sort by Name"-button.
- The camera icon opens the Proctoring View in a new tab. See chapter "Proctoring View" for more infos.
- Click on the "expand"-icon to enlarge the screenshot.

.. image:: images/screen_proctoring/gallery_view_settings.png
    :align: center
    :target: https://raw.githubusercontent.com/SafeExamBrowser/seb-server/docu/docs/images/screen_proctoring/gallery_view_settings.png

- The SEB-Client additionally sends metadata about the screenshot
- Metadata changes according to the content displayed on the screenshot
- Refer to chapter "Metadata" for more details
- The camera icon opens the Proctoring View in a new tab. See chapter "Proctoring View" for more infos.
- Close the expanded view either by clicking on the collapse button or somewhere outside of the screenshot


.. image:: images/screen_proctoring/gallery_view_expanded.png
    :align: center
    :target: https://raw.githubusercontent.com/SafeExamBrowser/seb-server/docu/docs/images/screen_proctoring/gallery_view_expanded.png

(kann man evtl. weg lassen)
- A message box indicates that there are no live sessions available
- As soon as a sessions are connected the message box will disappear


Running Exams
---------

To get an overview of all running exams which your user has access to click on the "Running Exams" item in the navigation bar on the left hand side.

- By Default the exams are sorted by "Exam Start-Time" in ascending order
- click on any table header to change the sorting according to your needs
- click on the link the "Group" column to get to the gallery view of the group

.. image:: images/screen_proctoring/running_exam_no_selection.png
    :align: center
    :target: https://raw.githubusercontent.com/SafeExamBrowser/seb-server/docu/docs/images/screen_proctoring/running_exam_no_selection.png

By clicking on the settings icon in the menu bar two options are displayed.

- select "Show past exams" to display all finished exams (red)
- select "Show upcoming exams" to display all exams which are planed for the future


.. image:: images/screen_proctoring/running_exam_selection.png
    :align: center
    :target: https://raw.githubusercontent.com/SafeExamBrowser/seb-server/docu/docs/images/screen_proctoring/running_exam_selection.png



Proctoring View
---------

The proctoring view shows a recorded or live session in more detail.






Search
---------

Metadata
---------

