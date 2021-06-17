-- -----------------------------------------------------
-- Remove all enableTouchExit attributes (9) from orientation
-- -----------------------------------------------------
DELETE FROM `orientation` WHERE `config_attribute_id`=9;
UPDATE orientation SET width=7 WHERE config_attribute_id=8;