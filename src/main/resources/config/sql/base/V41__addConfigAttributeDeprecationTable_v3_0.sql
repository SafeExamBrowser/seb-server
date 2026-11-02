-- ----------------------------------------------------------------
-- SEBSERV-843 Add configuration_attribute_deprecation table
-- ----------------------------------------------------------------

CREATE TABLE IF NOT EXISTS `configuration_attribute_deprecation` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  `configuration_attribute_id` BIGINT UNSIGNED NOT NULL,
  PRIMARY KEY (`id`),
  INDEX `configuration_attribute_idRef_idx` (`configuration_attribute_id` ASC),
  CONSTRAINT `configuration_attribute_idRef`
    FOREIGN KEY (`configuration_attribute_id`)
    REFERENCES `configuration_attribute` (`id`)
    ON DELETE NO ACTION
    ON UPDATE NO ACTION);

-- ----------------------------------------------------------------
-- SEBSERV-843 Add deprecations into table
-- ----------------------------------------------------------------

INSERT IGNORE INTO configuration_attribute_deprecation VALUES
    (1, 3),  -- ignoreExitKeys
    (2, 5),  -- exitKey1
    (3, 6),  -- exitKey2
    (4, 7),  -- exitKey3
    (5, 9),  -- enableTouchExit
    (6, 23),  -- zoomMode
    (7, 30),  -- allowSpellCheckDictionary
    (8, 36),  -- enablePlugIns
    (9, 38),  -- enableJava
    (10, 40),  -- allowVideoCapture
    (11, 41),  -- allowAudioCapture
    (12, 49),  -- removeLocalStorage
    (13, 92),  -- allowFlashFullscreen
    (14, 800),  -- browserMessagingSocket
    (15, 801),  -- browserMessagingPingTime
    (16, 805),  -- browserScreenKeyboard
    (17, 806),  -- newBrowserWindowByScriptPolicy
    (18, 807),  -- newBrowserWindowByScriptBlockForeign
    (19, 808),  -- monitorProcesses
    (20, 809),  -- blacklistURLFilter
    (21, 810),  -- whitelistURLFilter
    (22, 903),  -- showSettingsInApp
    (23, 906),  -- lockOnMessageSocketClose
    (24, 907),  -- enableDrawingEditor
    (25, 910),  -- allowUserSwitching
    (26, 915),  -- browserURLSalt
    (27, 933),  -- startResource
    (28, 946),  -- enableAAC
    (29, 1202),  -- browserShowFileSystemElementPath
    (30, 1576),  -- terminateProcesses
    (31, 1583),  -- systemAlwaysOn
    (32, 1584),  -- displayAlwaysOn
    (33, 1585),  -- disableSessionChangeLockScreen
    (34, 1610),  -- batteryChargeThresholdCritical
    (35, 1611)  -- batteryChargeThresholdLow
;
