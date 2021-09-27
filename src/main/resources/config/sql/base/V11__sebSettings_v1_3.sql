-- -----------------------------------------------------
-- Remove SEB Settings from GUI (and templates too)
-- -----------------------------------------------------

DELETE FROM `orientation` WHERE `config_attribute_id`='5';
DELETE FROM `orientation` WHERE `config_attribute_id`='6';
DELETE FROM `orientation` WHERE `config_attribute_id`='7';
