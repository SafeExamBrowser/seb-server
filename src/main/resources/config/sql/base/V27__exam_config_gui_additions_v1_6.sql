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

UPDATE orientation SET y_position=2 WHERE config_attribute_id=60;
UPDATE orientation SET y_position=3 WHERE config_attribute_id=61;
UPDATE orientation SET y_position=4 WHERE config_attribute_id=972;
UPDATE orientation SET y_position=11 WHERE config_attribute_id=63;
UPDATE orientation SET y_position=6 WHERE config_attribute_id=64;
UPDATE orientation SET y_position=7 WHERE config_attribute_id=65;
UPDATE orientation SET y_position=13 WHERE config_attribute_id=66;

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
-- Add new SEB Settings GUI (SEBSERV-405)
-- ----------------------------------------------------------------

INSERT IGNORE INTO orientation (config_attribute_id, template_id, view_id, group_id, x_position, y_position, width, height, title) VALUES
    (1200, 0,  6, null, 0, 6, 1, 1, 'LEFT');