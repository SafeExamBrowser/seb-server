-- -----------------------------------------------------------------
-- SEBSERV-403 - Allowed Displays
-- -----------------------------------------------------------------

UPDATE orientation SET y_position=10 WHERE config_attribute_id=301 AND template_id=0;
UPDATE orientation SET y_position=14 WHERE config_attribute_id=305 AND template_id=0;
UPDATE orientation SET y_position=17 WHERE config_attribute_id=306 AND template_id=0;
UPDATE orientation SET y_position=16 WHERE config_attribute_id=307 AND template_id=0;
UPDATE orientation SET y_position=2 WHERE config_attribute_id=309 AND template_id=0;
UPDATE orientation SET y_position=3 WHERE config_attribute_id=310 AND template_id=0;
UPDATE orientation SET y_position=4 WHERE config_attribute_id=311 AND template_id=0;
UPDATE orientation SET y_position=5 WHERE config_attribute_id=312 AND template_id=0;
UPDATE orientation SET y_position=6 WHERE config_attribute_id=313 AND template_id=0;
UPDATE orientation SET y_position=7 WHERE config_attribute_id=314 AND template_id=0;

UPDATE orientation SET y_position=11, group_id=NULL WHERE config_attribute_id=315 AND template_id=0;

UPDATE orientation SET y_position=8 WHERE config_attribute_id=316 AND template_id=0;
UPDATE orientation SET y_position=19 WHERE config_attribute_id=317 AND template_id=0;
UPDATE orientation SET y_position=15 WHERE config_attribute_id=319 AND template_id=0;
UPDATE orientation SET y_position=16 WHERE config_attribute_id=320 AND template_id=0;
UPDATE orientation SET y_position=13 WHERE config_attribute_id=947 AND template_id=0;

INSERT IGNORE INTO orientation (config_attribute_id, template_id, view_id, group_id, x_position, y_position, width, height, title) VALUES
    (1551, 0, 9, null, 7, 12, 5, 1, 'NONE');