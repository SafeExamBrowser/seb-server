-- -----------------------------------------------------------------
-- SEBSERV-614 - remove sps stop feature settings from GUI
-- -----------------------------------------------------------------

DELETE FROM `orientation` WHERE `config_attribute_id`=1324;
DELETE FROM `orientation` WHERE `config_attribute_id`=1325;

UPDATE orientation SET y_position=8 WHERE config_attribute_id=1326;
UPDATE orientation SET y_position=9 WHERE config_attribute_id=1320;
UPDATE orientation SET y_position=10 WHERE config_attribute_id=1321;
UPDATE orientation SET y_position=11 WHERE config_attribute_id=1322;