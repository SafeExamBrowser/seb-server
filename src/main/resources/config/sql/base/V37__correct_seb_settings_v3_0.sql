-- -----------------------------------------------------------------
-- SEBSERV-774 - Correct allowOpenAndSavePanel and allowShareSheet
-- -----------------------------------------------------------------

-- Delete old values and orientation first.
DELETE FROM configuration_value WHERE configuration_attribute_id=1630;
DELETE FROM configuration_value WHERE configuration_attribute_id=1631;
DELETE FROM orientation WHERE config_attribute_id=1630;
DELETE FROM orientation WHERE config_attribute_id=1631;

-- Update the attribute (name and parent)
UPDATE configuration_attribute SET name='allowOpenAndSavePanel', parent_id=NULL WHERE id=1630;
UPDATE configuration_attribute SET name='allowShareSheet', parent_id=NULL WHERE id=1631;

-- -----------------------------------------------------------------
-- SEBSERV-774 - Add browserViewModeTouch (2) option to  browserViewMode.
-- -----------------------------------------------------------------
UPDATE configuration_attribute SET resources = '0,1,2' WHERE (`id` = '8');
