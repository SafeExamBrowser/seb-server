-- -----------------------------------------------------------------
-- SEBSERV-405 starting with id 1200 and 1300
-- -----------------------------------------------------------------

INSERT IGNORE INTO configuration_attribute VALUES
    (1200, 'signature', 'TEXT_FIELD', null, null, null, null, null),
    (1201, 'clipboardPolicy', 'RADIO_SELECTION', null, '0,1,2', null, null, '2'),
    (1202, 'browserShowFileSystemElementPath', 'CHECKBOX', null, null, null, null, 'true'),
    
    (1300, 'enableScreenProctoring', 'CHECKBOX', null, null, null, null, 'false'),
    (1301, 'screenProctoringScreenshotMinInterval', 'INTEGER', null, null, null, null, '1000'),
    (1302, 'screenProctoringScreenshotMaxInterval', 'INTEGER', null, null, null, null, '5000'),
    (1303, 'screenProctoringImageFormat', 'SINGLE_SELECTION', null, '0', null, null, '0'),
    (1305, 'screenProctoringImageQuantization', 'SINGLE_SELECTION', null, '0,1,2,3,4,5', null, null, '2'),
    (1306, 'screenProctoringImageDownscale', 'SINGLE_SELECTION', null, '0,1,2,3,4,5,6,7,8,9,10', null, null, '0'),
    (1320, 'screenProctoringMetadataURLEnabled', 'CHECKBOX', null, null, null, null, 'true'),
    (1321, 'screenProctoringMetadataWindowTitleEnabled', 'CHECKBOX', null, null, null, null, 'true')
;

SET @proct_view_id = (SELECT id FROM view WHERE name='proctoring' AND template_id=0 LIMIT 1);

INSERT IGNORE INTO orientation (config_attribute_id, template_id, view_id, group_id, x_position, y_position, width, height, title) VALUES
    (1300, 0,  @proct_view_id, '[proctoring|ScreenProctoring]', 6, 1, 6, 1, 'NONE'),
    (1301, 0,  @proct_view_id, 'screenshot[proctoring|ScreenProctoring]', 9, 2, 3, 1, 'LEFT_SPAN'),
    (1302, 0,  @proct_view_id, 'screenshot[proctoring|ScreenProctoring]', 9, 3, 3, 1, 'LEFT_SPAN'),
    (1303, 0,  @proct_view_id, 'screenshot[proctoring|ScreenProctoring]', 9, 4, 3, 1, 'LEFT_SPAN'),
    (1305, 0,  @proct_view_id, 'screenshot[proctoring|ScreenProctoring]', 9, 5, 3, 1, 'LEFT_SPAN'),
    (1306, 0,  @proct_view_id, 'screenshot[proctoring|ScreenProctoring]', 9, 6, 3, 1, 'LEFT_SPAN'),
    
    
    (1320, 0,  @proct_view_id, 'metadata[proctoring|ScreenProctoring]', 6, 8, 6, 1, 'NONE'),
    (1321, 0,  @proct_view_id, 'metadata[proctoring|ScreenProctoring]', 6, 9, 6, 1, 'NONE')
;