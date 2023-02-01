-- -----------------------------------------------------------------
-- Fix SEB Settings according to SEBSERV-329
-- -----------------------------------------------------------------

UPDATE configuration_attribute SET default_value='1' WHERE id=1565;
UPDATE configuration_attribute SET default_value='1' WHERE id=1566;
UPDATE configuration_attribute SET parent_id=93 WHERE id=1577;