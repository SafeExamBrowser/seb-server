-- ----------------------------------------------------------------
-- Repair SEB Settings signature to be child of prohibitedProcesses
-- ----------------------------------------------------------------

UPDATE configuration_attribute SET name="permittedProcesses.signature", parent_id=73 WHERE id=1200;

-- ----------------------------------------------------------------
-- New Settings from issue SEBSERV-501
-- ----------------------------------------------------------------

INSERT IGNORE INTO configuration_attribute VALUES
    (1580, 'allowUploads', 'CHECKBOX', null, null, null, null, 'true'),
    (1581, 'allowDownloads', 'CHECKBOX', null, null, null, null, 'true')
;

-- ----------------------------------------------------------------
-- Add SEB Settings GUI additions (SEBSERV-465)
-- ----------------------------------------------------------------

INSERT IGNORE INTO orientation (config_attribute_id, template_id, view_id, group_id, x_position, y_position, width, height, title) VALUES
    (1564, 0,  3, null, 7, 15, 5, 1, 'TOP');

-- ----------------------------------------------------------------
-- Add SEB Settings GUI additions (SEBSERV-414)
-- ----------------------------------------------------------------

INSERT IGNORE INTO configuration_attribute VALUES
    (1582, 'useTemporaryDownUploadDirectory', 'CHECKBOX', null, null, null, null, 'false')
;

UPDATE configuration_attribute SET default_value='false' WHERE id=59;
UPDATE configuration_attribute SET name='allowCustomDownUploadLocation' WHERE id=972;

UPDATE orientation SET y_position=2 WHERE config_attribute_id=60 AND template_id=0;
UPDATE orientation SET y_position=3 WHERE config_attribute_id=61 AND template_id=0;
UPDATE orientation SET y_position=4 WHERE config_attribute_id=972 AND template_id=0;
UPDATE orientation SET y_position=11 WHERE config_attribute_id=63 AND template_id=0;
UPDATE orientation SET y_position=6 WHERE config_attribute_id=64 AND template_id=0;
UPDATE orientation SET y_position=7 WHERE config_attribute_id=65 AND template_id=0;
UPDATE orientation SET y_position=13 WHERE config_attribute_id=66 AND template_id=0;

INSERT IGNORE INTO orientation (config_attribute_id, template_id, view_id, group_id, x_position, y_position, width, height, title) VALUES
    (1580, 0,  4, null, 0, 9, 8, 1, 'NONE'),
    (1581, 0,  4, null, 0, 1, 8, 1, 'NONE'),
    (1582, 0,  4, null, 0, 5, 8, 1, 'NONE')
;

-- ----------------------------------------------------------------
-- Add new SEB Settings (SEBSERV-405)
-- ----------------------------------------------------------------

INSERT IGNORE INTO configuration_attribute VALUES
    (1583, 'systemAlwaysOn', 'CHECKBOX', null, null, null, null, 'true'),
    (1584, 'displayAlwaysOn', 'CHECKBOX', null, null, null, null, 'true'),
    (1585, 'disableSessionChangeLockScreen', 'CHECKBOX', null, null, null, null, 'false')
;

-- ----------------------------------------------------------------
-- Add new SEB Settings GUI (SEBSERV-414)
-- ----------------------------------------------------------------
INSERT IGNORE INTO orientation (config_attribute_id, template_id, view_id, group_id, x_position, y_position, width, height, title) VALUES
    (1200, 0,  6, null, 0, 6, 1, 1, 'LEFT');

-- ----------------------------------------------------------------
-- Remove Setting ignoreExitKeys from GUI (SEBSERV-414)
-- ----------------------------------------------------------------
DELETE FROM orientation WHERE config_attribute_id=3 AND template_id=0;

-- ----------------------------------------------------------------
-- Remove all dictionary settings from GUI (SEBSERV-414)
-- ----------------------------------------------------------------
DELETE FROM orientation WHERE config_attribute_id=30 AND template_id=0;

-- ----------------------------------------------------------------
-- Change default value of newBrowserWindowShowURL (SEBSERV-414)
-- ----------------------------------------------------------------
UPDATE configuration_attribute SET default_value='0' WHERE id=928;

-- ----------------------------------------------------------------
-- Add allowPrint and enableFindPrinter (SEBSERV-414)
-- ----------------------------------------------------------------
INSERT IGNORE INTO configuration_attribute VALUES
    (1590, 'allowPrint', 'CHECKBOX', null, null, null, null, 'false'),
    (1591, 'enableFindPrinter', 'CHECKBOX', null, null, null, null, 'false')
;
UPDATE orientation SET x_position=5, width=2 WHERE config_attribute_id=960 AND template_id=0;
INSERT IGNORE INTO orientation (config_attribute_id, template_id, view_id, group_id, x_position, y_position, width, height, title) VALUES
    (1590, 0,  3, 'browserSecurity', 0, 5, 4, 1, 'NONE'),
    (1591, 0,  10, 'registry', 0, 11, 4, 1, 'NONE')
;
-- ----------------------------------------------------------------
-- Add enableRightMouseMac (SEBSERV-414)
-- ----------------------------------------------------------------
UPDATE orientation SET y_position=14 WHERE config_attribute_id=43 AND template_id=0;
UPDATE orientation SET y_position=15 WHERE config_attribute_id=45 AND template_id=0;
UPDATE orientation SET y_position=16 WHERE config_attribute_id=47 AND template_id=0;
UPDATE orientation SET y_position=17 WHERE config_attribute_id=928 AND template_id=0;

UPDATE orientation SET y_position=10 WHERE config_attribute_id=42 AND template_id=0;
UPDATE orientation SET y_position=11 WHERE config_attribute_id=44 AND template_id=0;
UPDATE orientation SET y_position=12 WHERE config_attribute_id=46 AND template_id=0;
UPDATE orientation SET y_position=13 WHERE config_attribute_id=919 AND template_id=0;

INSERT IGNORE INTO orientation (config_attribute_id, template_id, view_id, group_id, x_position, y_position, width, height, title) VALUES
     (1568, 0,  3, null, 0, 9, 7, 1, 'NONE')
;

-- ----------------------------------------------------------------
-- Add prohibitedProcesses.ignoreInAAC (SEBSERV-414)
-- ----------------------------------------------------------------
INSERT IGNORE INTO orientation (config_attribute_id, template_id, view_id, group_id, x_position, y_position, width, height, title) VALUES
    (1577, 0,  6, null, 0, 8, 1, 1, 'LEFT')
;

-- ----------------------------------------------------------------
-- Add Exchange "enablePrivateClipboard" with "enablePrivateClipboardMacEnforce" (SEBSERV-414)
-- ----------------------------------------------------------------
UPDATE orientation SET config_attribute_id=947 WHERE config_attribute_id=304 AND template_id=0;

-- ----------------------------------------------------------------
-- Set minMacOSVersion (SEBSERV-414)
-- ----------------------------------------------------------------
UPDATE configuration_attribute SET default_value='8', resources='0,1,2,3,4,5,6,7,8,9,10,11,12' WHERE id=308;

-- ----------------------------------------------------------------
-- Move display and version settings down (SEBSERV-414)
-- ----------------------------------------------------------------
-- move logging down
UPDATE orientation SET y_position=17 WHERE config_attribute_id=305 AND template_id=0;
UPDATE orientation SET y_position=18 WHERE config_attribute_id=306 AND template_id=0;
UPDATE orientation SET y_position=19 WHERE config_attribute_id=307 AND template_id=0;
UPDATE orientation SET y_position=20 WHERE config_attribute_id=317 AND template_id=0;
UPDATE orientation SET y_position=21 WHERE config_attribute_id=319 AND template_id=0;
UPDATE orientation SET y_position=22 WHERE config_attribute_id=320 AND template_id=0;
-- move monitors left
UPDATE orientation SET x_position = 0, y_position=14 WHERE config_attribute_id=315 AND template_id=0;
UPDATE orientation SET x_position = 0, y_position=15 WHERE config_attribute_id=1551 AND template_id=0;
UPDATE orientation SET x_position = 0, y_position=16 WHERE config_attribute_id=971 AND template_id=0;
-- apply SEB versions on the right
UPDATE orientation SET x_position = 7, y_position=18, height=9, width=5 WHERE config_attribute_id=1578 AND template_id=0;
-- move macOS settings to make space for new
UPDATE orientation SET y_position=4 WHERE config_attribute_id=309 AND template_id=0;
UPDATE orientation SET y_position=5 WHERE config_attribute_id=310 AND template_id=0;
UPDATE orientation SET y_position=6 WHERE config_attribute_id=311 AND template_id=0;
UPDATE orientation SET y_position=7 WHERE config_attribute_id=312 AND template_id=0;
UPDATE orientation SET y_position=9 WHERE config_attribute_id=313 AND template_id=0;
UPDATE orientation SET y_position=10 WHERE config_attribute_id=314 AND template_id=0;
UPDATE orientation SET y_position=11 WHERE config_attribute_id=316 AND template_id=0;
-- add new macOS settings
INSERT IGNORE INTO orientation (config_attribute_id, template_id, view_id, group_id, x_position, y_position, width, height, title) VALUES
    (1567, 0,  9, 'macSettings', 7, 2, 5, 1, 'NONE'),
    (1550, 0,  9, 'macSettings', 7, 3, 5, 1, 'NONE'),
    (909, 0,  9, 'macSettings', 7, 8, 5, 1, 'NONE'),
    (1552, 0,  9, 'macSettings', 7, 12, 5, 1, 'NONE'),
    (948, 0,  9, 'macSettings', 7, 13, 5, 1, 'NONE'),
    (1557, 0,  9, 'macSettings', 7, 14, 5, 1, 'NONE'),
    (943, 0,  9, 'macSettings', 7, 15, 5, 1, 'NONE'),
    (945, 0,  9, 'macSettings', 7, 16, 5, 1, 'NONE')
;

-- ----------------------------------------------------------------
-- Add Media Playback/Capture (SEBSERV-414)
-- ----------------------------------------------------------------

INSERT IGNORE INTO orientation (config_attribute_id, template_id, view_id, group_id, x_position, y_position, width, height, title) VALUES
    (1558, 0,  3, 'mediaPlaybackCapture', 7, 16, 2, 1, 'NONE'),
    (1559, 0,  3, 'mediaPlaybackCapture', 7, 17, 2, 1, 'NONE'),
    (1560, 0,  3, 'mediaPlaybackCapture', 7, 18, 2, 1, 'NONE'),
    (1561, 0,  3, 'mediaPlaybackCapture', 7, 19, 2, 1, 'NONE'),
    (1562, 0,  3, 'mediaPlaybackCapture', 7, 20, 3, 1, 'NONE'),

    (905, 0,  3, 'mediaPlaybackCapture', 9, 18, 3, 1, 'NONE'),
    (1569, 0,  3, 'mediaPlaybackCapture', 9, 16, 3, 1, 'NONE'),
    (1570, 0,  3, 'mediaPlaybackCapture', 9, 17, 3, 1, 'NONE'),
    (1563, 0,  3, 'mediaPlaybackCapture', 9, 19, 3, 1, 'NONE')
;

-- ----------------------------------------------------------------
-- Add Lock screen color (SEBSERV-414)
-- ----------------------------------------------------------------

INSERT IGNORE INTO configuration_attribute VALUES
    (1595, 'lockScreenBackgroundColor', 'COLOR_SELECTOR', null, null, null, null, '#FF0000')
;

INSERT IGNORE INTO orientation (config_attribute_id, template_id, view_id, group_id, x_position, y_position, width, height, title) VALUES
    (1595, 0,  2, null, 7, 8, 5, 1, 'TOP')
;

UPDATE orientation SET height=1 WHERE config_attribute_id=8 AND template_id=0;

-- ----------------------------------------------------------------
-- Correct Quantisation Screen Proctoring (SEBSERV-504)
-- ----------------------------------------------------------------

UPDATE configuration_attribute SET resources='0,1,2,3,4,5,6' WHERE id=1305;