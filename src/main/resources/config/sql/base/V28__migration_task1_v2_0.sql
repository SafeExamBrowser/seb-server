-- ----------------------------------------------------------------
-- Add direct_login and local_account to user table SEBSERV-417
-- ----------------------------------------------------------------

ALTER TABLE `user`
ADD COLUMN IF NOT EXISTS `direct_login` INT(1) NOT NULL DEFAULT 1,
ADD COLUMN IF NOT EXISTS `local_account` INT(1) NOT NULL DEFAULT 1;

-- ----------------------------------------------------------------
-- Add connection_id to lms_setup table SEBSERV-417
-- ----------------------------------------------------------------

ALTER TABLE `lms_setup`
ADD COLUMN IF NOT EXISTS `connection_id` VARCHAR(255) NULL,
ADD COLUMN IF NOT EXISTS `integration_active` INT(1) NOT NULL;

-- ----------------------------------------------------------------
-- Add lms_integration to exam_template table SEBSERV-417
-- ----------------------------------------------------------------

ALTER TABLE `exam_template`
ADD COLUMN IF NOT EXISTS `lms_integration` INT(1) NOT NULL DEFAULT 0;

-- ----------------------------------------------------------------
-- Add clipboard policy to GUI SEBSERV-534
-- ----------------------------------------------------------------
UPDATE orientation SET y_position=21 WHERE config_attribute_id=1578 AND template_id=0;
INSERT IGNORE INTO orientation (config_attribute_id, template_id, view_id, group_id, x_position, y_position, width, height, title) VALUES
    (1201, 0, 9, 'clipboardPolicy', 7, 18, 5, 2, 'NONE');