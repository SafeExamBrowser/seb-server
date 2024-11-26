-- -----------------------------------------------------------------
-- SEBSP-172, SEBSERV-596 and SEBSERV-592
-- -----------------------------------------------------------------

INSERT IGNORE INTO configuration_attribute VALUES
    (1323, 'screenProctoringCacheSize', 'INTEGER', null, null, null, null, '500'),
    (1324, 'screenProctoringStopURL', 'TEXT_FIELD', null, null, null, null, null),
    (1325, 'screenProctoringStopURLConfirm', 'CHECKBOX', null, null, null, null, 'true'),
    
    (1600, 'enableCursorVerification', 'CHECKBOX', null, null, null, null, 'true'),
    (1601, 'allowStickyKeys', 'CHECKBOX', null, null, null, null, 'false'),
    (1602, 'browserWindowWebViewClassicHideDeprecationNote', 'CHECKBOX', null, null, null, null, 'false'),
    (1603, 'browserConnectionErrorReload', 'CHECKBOX', null, null, null, null, 'false')
;

SET @proct_view_id = (SELECT id FROM view WHERE name='proctoring' AND template_id=0 LIMIT 1);

INSERT IGNORE INTO orientation (config_attribute_id, template_id, view_id, group_id, x_position, y_position, width, height, title) VALUES
    (1323, 0,  @proct_view_id, 'screenshot[proctoring|ScreenProctoring]', 9, 7, 3, 1, 'LEFT_SPAN'),
    (1324, 0,  @proct_view_id, 'screenshot[proctoring|ScreenProctoring]', 9, 8, 3, 1, 'LEFT_SPAN'),
    (1325, 0,  @proct_view_id, 'screenshot[proctoring|ScreenProctoring]', 9, 9, 3, 1, 'LEFT_SPAN')
;

UPDATE orientation SET y_position=11 WHERE config_attribute_id=1320 AND template_id=0;
UPDATE orientation SET y_position=12 WHERE config_attribute_id=1321 AND template_id=0;
UPDATE orientation SET y_position=13 WHERE config_attribute_id=1322 AND template_id=0;
