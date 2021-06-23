-- -----------------------------------------------------
-- Add missing (new) SEB settings attributes
-- -----------------------------------------------------
INSERT IGNORE INTO configuration_attribute VALUES 
    (950, 'showSideMenu', 'CHECKBOX', null, null, null, null, 'true'),
    (951, 'browserWindowAllowAddressBar', 'CHECKBOX', null, null, null, null, 'false'),
    (952, 'newBrowserWindowAllowAddressBar', 'CHECKBOX', null, null, null, null, 'false'),
    (953, 'allowDeveloperConsole', 'CHECKBOX', null, null, null, null, 'false'),

    (960, 'allowFind', 'CHECKBOX', null, null, null, null, 'true'),
    (961, 'allowPDFReaderToolbar', 'CHECKBOX', null, null, null, null, 'false'),

    (970, 'setVmwareConfiguration', 'CHECKBOX', null, null, null, null, 'false')
    ;

-- -----------------------------------------------------
-- Correct default value for newBrowserWindowShowURL
-- -----------------------------------------------------
UPDATE configuration_attribute SET default_value='2' WHERE id=928;

-- -----------------------------------------------------
-- Remove unused orientations
-- -----------------------------------------------------

-- remove enableTouchExit setting from GUI
DELETE FROM `orientation` WHERE `config_attribute_id`=9;

-- remove taskBarHeight from GUI
DELETE FROM `orientation` WHERE `config_attribute_id`=17;

-- remove Browser security
DELETE FROM `orientation` WHERE `config_attribute_id`=36;
DELETE FROM `orientation` WHERE `config_attribute_id`=37;
DELETE FROM `orientation` WHERE `config_attribute_id`=38;
DELETE FROM `orientation` WHERE `config_attribute_id`=39;
DELETE FROM `orientation` WHERE `config_attribute_id`=40;
DELETE FROM `orientation` WHERE `config_attribute_id`=41;
DELETE FROM `orientation` WHERE `config_attribute_id`=49;

-- -----------------------------------------------------
-- Add new orientations
-- -----------------------------------------------------

-- insert Browser window toolbar
INSERT IGNORE INTO orientation (config_attribute_id, template_id, view_id, group_id, x_position, y_position, width, height, title) VALUES
    (951, 0, 2, 'wintoolbar', 0, 7, 5, 1, 'NONE'),
    (952, 0, 2, 'wintoolbar', 0, 8, 5, 1, 'NONE'),
    (953, 0, 2, 'wintoolbar', 0, 9, 5, 1, 'NONE')
    ;

-- insert Taskbar / Dock
INSERT IGNORE INTO orientation (config_attribute_id, template_id, view_id, group_id, x_position, y_position, width, height, title) VALUES
    (950, 0, 2, 'taskbar', 0, 10, 3, 1, 'NONE');

-- insert Browser security
INSERT IGNORE INTO orientation (config_attribute_id, template_id, view_id, group_id, x_position, y_position, width, height, title) VALUES
    (960, 0, 3, 'browserSecurity', 0, 5, 7, 1, 'NONE'),
    (961, 0, 3, 'browserSecurity', 0, 6, 7, 1, 'NONE')
    ;

-- insert Restrictions in Exam Window
INSERT IGNORE INTO orientation (config_attribute_id, template_id, view_id, group_id, x_position, y_position, width, height, title) VALUES
    (919, 0, 3, 'examWindow', 2, 12, 5, 1, 'LEFT_SPAN'),
    (928, 0, 3, 'additionalWindow', 2, 15, 5, 1, 'LEFT_SPAN');

-- insert Set VMWare Configuration
INSERT IGNORE INTO orientation (config_attribute_id, template_id, view_id, group_id, x_position, y_position, width, height, title) VALUES
    (970, 0, 10, 'registry', 0, 7, 4, 1, 'NONE');

-- -----------------------------------------------------
-- Update old orientations
-- -----------------------------------------------------
-- update stretch the orientation for Browser view mode group
UPDATE orientation SET width=7 WHERE config_attribute_id=8;

-- update Browser window toolbar
UPDATE orientation SET width=4 WHERE config_attribute_id=13;
UPDATE orientation SET x_position=0, y_position=10, width=5 WHERE config_attribute_id=14;
UPDATE orientation SET x_position=4, y_position=6, width=3 WHERE config_attribute_id=15;

-- update Taskbar / Dock
UPDATE orientation SET x_position=4, y_position=9, width=3 WHERE config_attribute_id=812;
UPDATE orientation SET x_position=4, y_position=10, width=3 WHERE config_attribute_id=18;
UPDATE orientation SET x_position=4, y_position=11, width=3 WHERE config_attribute_id=19;
UPDATE orientation SET x_position=4, y_position=12, width=3 WHERE config_attribute_id=20;

-- update zoom and zoom mode
UPDATE orientation SET width=4 WHERE config_attribute_id=21;
UPDATE orientation SET x_position=4, width=3 WHERE config_attribute_id=23;

-- update Browser security
UPDATE orientation SET y_position=7, width=7 WHERE config_attribute_id=48;

-- update Restrictions in Exam Window
UPDATE orientation SET group_id='examWindow', y_position=9, width=7 WHERE config_attribute_id=42;
UPDATE orientation SET group_id='examWindow', y_position=10, width=7 WHERE config_attribute_id=44;
UPDATE orientation SET group_id='examWindow', y_position=11, width=7 WHERE config_attribute_id=46;

-- update Restrictions in Additional Window
UPDATE orientation SET group_id='additionalWindow', x_position=0, y_position=12, width=7 WHERE config_attribute_id=43;
UPDATE orientation SET group_id='additionalWindow', x_position=0, y_position=13, width=7 WHERE config_attribute_id=45;
UPDATE orientation SET group_id='additionalWindow', x_position=0, y_position=14, width=7 WHERE config_attribute_id=47;

-- update Enable SEB browser and Suffix
UPDATE orientation SET x_position=7, y_position=11, width=5 WHERE config_attribute_id=57;
UPDATE orientation SET x_position=7, y_position=13, width=5 WHERE config_attribute_id=58;

-- insert Set VMWare Configuration
UPDATE orientation SET y_position=8 WHERE config_attribute_id=406;
UPDATE orientation SET y_position=9 WHERE config_attribute_id=407;
UPDATE orientation SET y_position=10 WHERE config_attribute_id=408;
