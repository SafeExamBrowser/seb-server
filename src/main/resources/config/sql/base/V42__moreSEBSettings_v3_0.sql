-- -----------------------------------------------------------------
-- SEBSERV-846 - Insert more new SEB settings
-- -----------------------------------------------------------------

INSERT IGNORE INTO configuration_attribute VALUES
    (1670, 'showProctoringDisclaimer', 'CHECKBOX', null, null, null, null, 'true'),
    (1671, 'detectAccessibilityApps', 'CHECKBOX', null, null, null, null, 'true'),
    (1672, 'screenProctoringAACCapturePolicy', 'SINGLE_SELECTION', null, '0,1,2', null, null, '2'),
    (1673, 'hideWiFiControls', 'CHECKBOX', null, null, null, null, 'false'),
    (1674, 'lockdownModePolicy', 'SINGLE_SELECTION', null, '0,1,2', null, null, '0'),
    (1675, 'permittedProcesses.allowAccessibility', 'CHECKBOX', 73, null, null, null, 'false')
;

-- -----------------------------------------------------------------
-- SEBSERV-935 - Insert new new SEB setting and change former
-- -----------------------------------------------------------------

INSERT IGNORE INTO configuration_attribute VALUES
    (1676, 'allowScreenCaptureiOS', 'CHECKBOX', null, null, null, null, 'false')
;

-- change allowiOSBetaVersionNumber from SINGLE_SELECTION to INTEGER
UPDATE configuration_attribute SET type = 'INTEGER', resources = '' WHERE (`id` = '911');