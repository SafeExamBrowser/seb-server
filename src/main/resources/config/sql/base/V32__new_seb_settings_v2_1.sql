-- -----------------------------------------------------------------
-- SEBSERV-592 - attribute names get bigger, need larger name column
-- -----------------------------------------------------------------

ALTER TABLE `configuration_attribute`
MODIFY `name` VARCHAR(255) NOT NULL;

-- -----------------------------------------------------------------
-- SEBSERV-592 - "allowUploads" change default value
-- -----------------------------------------------------------------

UPDATE configuration_attribute SET default_value='false' WHERE name='allowUploads';

-- -----------------------------------------------------------------
-- SEBSERV-592 - batteryChargeThreshold... no GUI entry
-- -----------------------------------------------------------------

INSERT IGNORE INTO configuration_attribute VALUES
    (1610, 'batteryChargeThresholdCritical', 'DECIMAL', null, null, "DecimalTypeValidator", null, '0.1'),
    (1611, 'batteryChargeThresholdLow', 'DECIMAL', null, null, "DecimalTypeValidator", null, '0.2')
;

-- -----------------------------------------------------------------
-- SEBSERV-592 - Security entries
-- -----------------------------------------------------------------

SET @sec_view_id = (SELECT id FROM view WHERE name='security' AND template_id=0 LIMIT 1);

INSERT IGNORE INTO configuration_attribute VALUES
    (1620, 'enableSessionVerification', 'CHECKBOX', null, null, null, null, 'true'),
    (1621, 'mobileEnableModernAAC', 'CHECKBOX', null, null, null, null, 'true')
;

INSERT IGNORE INTO orientation (config_attribute_id, template_id, view_id, group_id, x_position, y_position, width, height, title) VALUES
    (1600, 0,  @sec_view_id, null, 0, 24, 3, 1, 'NONE'),
    (1620, 0,  @sec_view_id, null, 0, 25, 3, 1, 'NONE'),
    (1601, 0,  @sec_view_id, null, 0, 26, 3, 1, 'NONE'),
    (1621, 0,  @sec_view_id, null, 4, 24, 3, 1, 'NONE')
;

-- -----------------------------------------------------------------
-- SEBSERV-592 - Browser entries
-- -----------------------------------------------------------------

SET @browser_view_id = (SELECT id FROM view WHERE name='browser' AND template_id=0 LIMIT 1);

UPDATE configuration_attribute SET name='browserWindowWebViewClassicHideDeprecationNote' WHERE id=1602;

INSERT IGNORE INTO orientation (config_attribute_id, template_id, view_id, group_id, x_position, y_position, width, height, title) VALUES
    (1602, 0,  @browser_view_id, null, 0, 18, 4, 1, 'NONE'),
    (1603, 0,  @browser_view_id, null, 0, 19, 4, 1, 'NONE')
;

-- -----------------------------------------------------------------
-- SEBSERV-592 - Screen Proctoring addition
-- -----------------------------------------------------------------

INSERT IGNORE INTO configuration_attribute VALUES
    (1326, 'screenProctoringIndicateHealthAndCaching', 'CHECKBOX', null, null, null, null, 'false')
;

SET @proct_view_id = (SELECT id FROM view WHERE name='proctoring' AND template_id=0 LIMIT 1);

INSERT IGNORE INTO orientation (config_attribute_id, template_id, view_id, group_id, x_position, y_position, width, height, title) VALUES
    (1326, 0,  @proct_view_id, 'screenshot[proctoring|ScreenProctoring]', 9, 10, 3, 1, 'LEFT_SPAN')
;

UPDATE orientation SET y_position=12 WHERE config_attribute_id=1320 AND template_id=0;
UPDATE orientation SET y_position=13 WHERE config_attribute_id=1321 AND template_id=0;
UPDATE orientation SET y_position=14 WHERE config_attribute_id=1322 AND template_id=0;

-- -----------------------------------------------------------------
-- SEBSERV-592 - permittedProcesses addition
-- -----------------------------------------------------------------

SET @app_view_id = (SELECT id FROM view WHERE name='applications' AND template_id=0 LIMIT 1);

INSERT IGNORE INTO configuration_attribute VALUES
    (1630, 'permittedProcesses.allowOpenAndSavePanel', 'CHECKBOX', 73, null, null, null, 'false'),
    (1631, 'permittedProcesses.allowShareSheet', 'CHECKBOX', 73, null, null, null, 'false'),
    (1632, 'permittedProcesses.allowManualStart', 'CHECKBOX', 73, null, null, null, 'true'),
    (1633, 'permittedProcesses.allowNetworkAccess', 'CHECKBOX', 73, null, null, null, 'false'),
    (1634, 'permittedProcesses.teamIdentifier', 'TEXT_FIELD', 73, null, null, null, '')
;

INSERT IGNORE INTO orientation (config_attribute_id, template_id, view_id, group_id, x_position, y_position, width, height, title) VALUES
    (1630, 0,  @app_view_id, null, 0, 8, 1, 1, 'LEFT'),
    (1631, 0,  @app_view_id, null, 0, 9, 1, 1, 'LEFT'),
    (1632, 0,  @app_view_id, null, 0, 10, 1, 1, 'LEFT'),
    (1633, 0,  @app_view_id, null, 0, 11, 1, 1, 'LEFT'),
    (1634, 0,  @app_view_id, null, 0, 12, 1, 1, 'LEFT')
;

-- -----------------------------------------------------------------
-- SEBSERV-592 - User Interface addition
-- -----------------------------------------------------------------

SET @ui_view_id = (SELECT id FROM view WHERE name='user_interface' AND template_id=0 LIMIT 1);

INSERT IGNORE INTO configuration_attribute VALUES
    (1640, 'enableScrollLock', 'CHECKBOX', null, null, null, null, 'true')
;

INSERT IGNORE INTO orientation (config_attribute_id, template_id, view_id, group_id, x_position, y_position, width, height, title) VALUES
    (1640, 0,  @ui_view_id, null, 7, 8, 5, 1, 'NONE')
;
UPDATE orientation SET y_position=7 WHERE config_attribute_id=1595 AND template_id=0;
