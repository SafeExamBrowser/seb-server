-- -----------------------------------------------------------------
-- SEBSERV-654 - Change default value of "enableLogging" from false to true
-- -----------------------------------------------------------------

UPDATE configuration_attribute SET default_value='true' WHERE name='enableLogging';

-- -----------------------------------------------------------------
-- SEBSERV-654 - Change default value of "quitURLRestart" from true to false
-- -----------------------------------------------------------------

UPDATE configuration_attribute SET default_value='false' WHERE name='quitURLRestart';

-- -----------------------------------------------------------------
-- SEBSERV-654 - Remove (Allow Flash to switch to full screen mode) from GUI
-- -----------------------------------------------------------------

DELETE FROM `orientation` WHERE `config_attribute_id`=92;