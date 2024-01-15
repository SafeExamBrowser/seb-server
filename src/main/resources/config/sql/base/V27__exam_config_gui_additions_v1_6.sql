-- ----------------------------------------------------------------
-- Repair SEB Settings signature to be child of prohibitedProcesses
-- ----------------------------------------------------------------

UPDATE configuration_attribute SET name="permittedProcesses.signature", parent_id=73 WHERE id=1200;

-- ----------------------------------------------------------------
-- New Settings from issue SEBSERV-501
-- ----------------------------------------------------------------

INSERT IGNORE INTO configuration_attribute VALUES
    (1580, 'allowUploads', 'CHECKBOX', null, null, null, null, 'true'),
    (1581, 'allowDownloads', 'CHECKBOX', null, null, null, null, 'true')
;

-- ----------------------------------------------------------------
-- Add SEB Settings GUI additions (SEBSERV-465)
-- ----------------------------------------------------------------

INSERT IGNORE INTO orientation (config_attribute_id, template_id, view_id, group_id, x_position, y_position, width, height, title) VALUES
    (1564, 0,  3, null, 7, 15, 5, 1, 'TOP');

-- ----------------------------------------------------------------
-- Add SEB Settings GUI additions (SEBSERV-414)
-- ----------------------------------------------------------------

