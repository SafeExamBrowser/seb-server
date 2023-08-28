-- -----------------------------------------------------------------
-- SEBSERV-405 starting with id 1200 and 1300
-- -----------------------------------------------------------------

INSERT IGNORE INTO configuration_attribute VALUES
    (1200, 'signature', 'TEXTBOX', null, null, null, null, null),
    (1201, 'clipboardPolicy', 'RADIO_SELECTION', null, '0,1,2', null, null, '2'),
    
    (1300, 'enableScreenProctoring', 'CHECKBOX', null, null, null, null, 'false'),
    (1301, 'screenProctoringScreenshotMinInterval', 'NUMBER_INPUT ', null, null, null, null, '1000'),
    (1302, 'screenProctoringScreenshotMaxInterval', 'NUMBER_INPUT ', null, null, null, null, '5000'),
    (1303, 'screenProctoringImageFormat', 'SINGLE_SELECTION ', null, '0,1', null, null, '0'),
    (1304, 'screenProctoringImageBitsPerPixel', 'SINGLE_SELECTION ', null, '0,1,2,3,4,5,6', null, null, '2'),
    (1305, 'screenProctoringImageQuantization', 'SINGLE_SELECTION ', null, '0,1,2,3', null, null, '0'),
    (1306, 'screenProctoringImageDownscale', 'SINGLE_SELECTION ', null, '0,1,2,3,4,5,6,7,8,9,10', null, null, '0'),
    (1320, 'screenProctoringMetadataURLEnabled', 'CHECKBOX ', null, null, null, null, 'true'),
    (1321, 'screenProctoringMetadataWindowTitleEnabled', 'CHECKBOX ', null, null, null, null, 'true')
;