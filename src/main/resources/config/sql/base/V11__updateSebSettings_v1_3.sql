-- -----------------------------------------------------------------
-- SEBSERV-230 Remove exit keys from GUI (and templates too)
-- -----------------------------------------------------------------

DELETE FROM `orientation` WHERE `config_attribute_id`='5';
DELETE FROM `orientation` WHERE `config_attribute_id`='6';
DELETE FROM `orientation` WHERE `config_attribute_id`='7';

-- -----------------------------------------------------------------
-- SEBSERV-201 Number 2
-- -----------------------------------------------------------------

INSERT IGNORE INTO configuration_attribute VALUES
    (972, 'allowCustomDownloadLocation', 'CHECKBOX', null, null, null, null, 'false');

UPDATE orientation SET config_attribute_id=972 WHERE config_attribute_id=62;
UPDATE orientation SET y_position=6 WHERE config_attribute_id=63 AND template_id=0;
UPDATE orientation SET y_position=9 WHERE config_attribute_id=64 AND template_id=0;
UPDATE orientation SET y_position=10 WHERE config_attribute_id=65 AND template_id=0;
UPDATE orientation SET y_position=11 WHERE config_attribute_id=66 AND template_id=0;

-- -----------------------------------------------------------------
-- SEBSERV-201 Number 3
-- -----------------------------------------------------------------

INSERT IGNORE INTO configuration_attribute VALUES
    (973, 'startURLAppendQueryParameter', 'CHECKBOX', null, null, null, null, 'false');
    
INSERT IGNORE INTO orientation (config_attribute_id, template_id, view_id, group_id, x_position, y_position, width, height, title) VALUES
    (973, 0, 5, 'queryParams', 0, 10, 8, 1, 'NONE');

-- -----------------------------------------------------------------
-- SEBSERV-201 Number 4
-- -----------------------------------------------------------------

UPDATE orientation SET width=2 WHERE config_attribute_id=53;
INSERT IGNORE INTO orientation (config_attribute_id, template_id, view_id, group_id, x_position, y_position, width, height, title) VALUES
    (904, 0, 3, 'userAgentTouch', 9, 5, 3, 1, 'NONE');

-- -----------------------------------------------------------------
-- SEBSERV-201 Number 5 (raise hand)
-- -----------------------------------------------------------------

INSERT IGNORE INTO configuration_attribute VALUES
    (974, 'raiseHandButtonShow', 'CHECKBOX', null, null, null, null, 'false'),
    (975, 'raiseHandButtonAlwaysPromptMessage', 'CHECKBOX', null, null, null, null, 'false');

INSERT IGNORE INTO orientation (config_attribute_id, template_id, view_id, group_id, x_position, y_position, width, height, title) VALUES
    (974, 0, 2, 'taskbar', 0, 11, 3, 1, 'NONE'),
    (975, 0, 2, 'taskbar', 0, 12, 3, 1, 'NONE');

-- -----------------------------------------------------------------
-- SEBSERV-232 change default value jitsi to not enabled
-- -----------------------------------------------------------------

UPDATE configuration_attribute SET default_value='false' WHERE id=1102;
