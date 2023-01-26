-- -----------------------------------------------------------------
-- SEBSERV-329 starting with id 1550
-- -----------------------------------------------------------------

INSERT IGNORE INTO configuration_attribute VALUES
    (1550, 'aacDnsPrePinning', 'CHECKBOX', null, null, null, null, 'false'),
    (1551, 'allowedDisplayBuiltinEnforce', 'CHECKBOX', null, null, null, null, 'true'),
    (1552, 'allowedDisplayBuiltinExceptDesktop', 'CHECKBOX', null, null, null, null, 'true'),
    (1553, 'allowMacOSVersionNumberCheckFull', 'CHECKBOX', null, null, null, null, 'false'),
    (1554, 'allowMacOSVersionNumberMajor', 'COMBO_SELECTION', null, '10,11,12,13,14', null, null, '10'),
    (1555, 'allowMacOSVersionNumberMinor', 'COMBO_SELECTION', null, '0,1,2,3,4,5,6,7,8,9,10,11,12,13,14,15,16,17,18,19,20', null, null, '11'),
    (1556, 'allowMacOSVersionNumberPatch', 'COMBO_SELECTION', null, '0,1,2,3,4,5,6,7,8,9,10,11,12,13,14,15,16,17,18,19,20', null, null, '0'),
    (1557, 'allowScreenCapture', 'CHECKBOX', null, null, null, null, 'false'),
    (1558, 'browserMediaAutoplay', 'CHECKBOX', null, null, null, null, 'true'),
    (1559, 'browserMediaAutoplayAudio', 'CHECKBOX', null, null, null, null, 'true'),
    (1560, 'browserMediaAutoplayVideo', 'CHECKBOX', null, null, null, null, 'true'),
    (1561, 'browserMediaCaptureCamera', 'CHECKBOX', null, null, null, null, 'false'),
    (1562, 'browserMediaCaptureMicrophone', 'CHECKBOX', null, null, null, null, 'false'),
    (1563, 'browserMediaCaptureScreen', 'CHECKBOX', null, null, null, null, 'false'),
    (1564, 'browserWindowWebView', 'SINGLE_SELECTION', null, '0,1,2,3', null, null, '2'),
    
    (1565, 'defaultPageZoomLevel', 'DECIMAL', null, '0.25,4.0', 'DecimalTypeValidator', null, '1.0'),
    (1566, 'defaultTextZoomLevel', 'DECIMAL', null, '0.9,3.5', 'DecimalTypeValidator', null, '1.0'),
    (1567, 'enableMacOSAAC', 'CHECKBOX', null, null, null, null, 'false'),
    (1568, 'enableRightMouseMac', 'CHECKBOX', null, null, null, null, 'false'),
    (1569, 'mobileAllowInlineMediaPlayback', 'CHECKBOX', null, null, null, null, 'true'),
    (1570, 'mobileCompactAllowInlineMediaPlayback', 'CHECKBOX', null, null, null, null, 'false'),
    (1571, 'mobileSleepModeLockScreen', 'CHECKBOX', null, null, null, null, 'true'),
    (1572, 'showQuitButton', 'CHECKBOX', null, null, null, null, 'true'),
    (1573, 'showScrollLockButton', 'CHECKBOX', null, null, null, null, 'true'),
    (1574, 'startURLAllowDeepLink', 'CHECKBOX', null, null, null, null, 'false'),
    (1575, 'tabFocusesLinks', 'CHECKBOX', null, null, null, null, 'true'),
    (1576, 'terminateProcesses', 'CHECKBOX', null, null, null, null, 'false'),
    (1577, 'prohibitedProcesses.ignoreInAAC', 'CHECKBOX', 73, null, null, null, 'true'),
    
    (1578, 'sebAllowedVersions', 'TEXT_FIELD_LIST', null, null, null, null, null)
;