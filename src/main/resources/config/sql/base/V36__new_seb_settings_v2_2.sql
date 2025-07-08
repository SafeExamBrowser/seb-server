-- -----------------------------------------------------------------
-- SEBSERV-654 - New iOS setting "mobileShowEditConfigShortcutItem" (default true)
-- -----------------------------------------------------------------

INSERT IGNORE INTO configuration_attribute VALUES
    (1650, 'mobileShowEditConfigShortcutItem', 'CHECKBOX', null, null, null, null, 'true')
;

-- -----------------------------------------------------------------
-- SEBSERV-654 - New iOS setting "allowUploadsiOS" (default false)
-- -----------------------------------------------------------------

INSERT IGNORE INTO configuration_attribute VALUES
    (1651, 'allowUploadsiOS', 'CHECKBOX', null, null, null, null, 'false')
;

INSERT IGNORE INTO orientation (config_attribute_id, template_id, view_id, group_id, x_position, y_position, width, height, title) VALUES
    (1651, 0,  4, null, 0, 14, 8, 1, 'NONE')
;

-- -----------------------------------------------------------------
-- SEBSERV-654 -new setting downloadFileTypes Array of downloadFileType dictionaries
-- -----------------------------------------------------------------

INSERT IGNORE INTO configuration_attribute VALUES
    (1652, 'downloadFileTypes', 'TABLE', null, null, null, null, null),
    (1653, 'downloadFileTypes.associatedAppId', 'TEXT_FIELD', 1652, null, null, null, ''),
    (1654, 'downloadFileTypes.extension', 'TEXT_FIELD', 1652, null, null, null, ''),
    (1655, 'downloadFileTypes.os', 'SINGLE_SELECTION', 1652, '0,1,2', null, null, '0')
;

INSERT IGNORE INTO orientation (config_attribute_id, template_id, view_id, group_id, x_position, y_position, width, height, title) VALUES
    (1652, 0,  4, null, 0, 16, 10, 6, 'TOP'),
    (1653, 0,  4, null, 6, 3, 4, 1, 'LEFT'),
    (1654, 0,  4, null, 1, 1, 4, 1, 'LEFT'),
    (1655, 0,  4, null, 3, 2, 2, 1, 'LEFT')
;