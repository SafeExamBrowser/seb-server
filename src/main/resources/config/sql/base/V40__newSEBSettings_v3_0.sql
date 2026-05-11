-- -----------------------------------------------------------------
-- SEBSERV-774 - Insert new SEB settings
-- -----------------------------------------------------------------

INSERT IGNORE INTO configuration_attribute VALUES
    (1660, 'accessibilityFeatureVoiceOver', 'SINGLE_SELECTION', null, '0,1,2', null, null, '0'),
    (1661, 'accessibilityFeatureAssistiveTouch', 'SINGLE_SELECTION', null, '0,1,2', null, null, '0'),
    (1662, 'accessibilityFeatureGrayscaleDisplay', 'SINGLE_SELECTION', null, '0,1,2', null, null, '0'),
    (1663, 'accessibilityFeatureInvertColors', 'SINGLE_SELECTION', null, '0,1,2', null, null, '0'),
    (1664, 'accessibilityFeatureZoom', 'SINGLE_SELECTION', null, '0,1,2', null, null, '0'),
    (1665, 'showQRVerifyButton', 'CHECKBOX', null, null, null, null, 'true')
;
