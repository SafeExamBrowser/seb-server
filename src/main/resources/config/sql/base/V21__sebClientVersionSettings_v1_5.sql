-- -----------------------------------------------------------------
-- SEBSERV-376 Add SEBVersionValidator to setting
-- -----------------------------------------------------------------

UPDATE configuration_attribute SET validator='SEBVersionValidator' WHERE id=1578;

-- -----------------------------------------------------------------
-- SEBSERV-376 Reorder security / logging section for default template
-- -----------------------------------------------------------------

UPDATE orientation SET width=6 WHERE config_attribute_id=305 AND template_id=0;
UPDATE orientation SET width=4 WHERE config_attribute_id=306 AND template_id=0;
UPDATE orientation SET width=4 WHERE config_attribute_id=307 AND template_id=0;
UPDATE orientation SET width=4 WHERE config_attribute_id=317 AND template_id=0;
UPDATE orientation SET width=4 WHERE config_attribute_id=319 AND template_id=0;
UPDATE orientation SET width=4 WHERE config_attribute_id=320 AND template_id=0;

-- -----------------------------------------------------------------
-- SEBSERV-376 add new orientation for default template
-- -----------------------------------------------------------------

INSERT IGNORE INTO orientation (config_attribute_id, template_id, view_id, group_id, x_position, y_position, width, height, title) VALUES
    (1578, 0, 9, null, 7, 14, 4, 12, 'NONE');