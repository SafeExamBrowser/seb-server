-- -----------------------------------------------------
-- Alter Table `batch_action`
-- -----------------------------------------------------

ALTER TABLE `batch_action`
MODIFY `source_ids` VARCHAR(4000) NULL,
ADD COLUMN IF NOT EXISTS `owner` VARCHAR(255) NULL AFTER `institution_id`,
ADD COLUMN IF NOT EXISTS `attributes` VARCHAR(4000) NULL AFTER `action_type`
;

-- -----------------------------------------------------
-- Alter Table `configuration_node`
-- -----------------------------------------------------

ALTER TABLE `configuration_node`
ADD COLUMN IF NOT EXISTS `last_update_time` BIGINT UNSIGNED NULL AFTER `status`,
ADD COLUMN IF NOT EXISTS `last_update_user` VARCHAR(255) NULL AFTER `last_update_time`
;

-- -----------------------------------------------------
-- Alter Table `seb_client_configuration`
-- -----------------------------------------------------

ALTER TABLE `seb_client_configuration`
ADD COLUMN IF NOT EXISTS `last_update_time` BIGINT UNSIGNED NULL AFTER `active`,
ADD COLUMN IF NOT EXISTS `last_update_user` VARCHAR(255) NULL AFTER `last_update_time`
;

-- -----------------------------------------------------
-- Alter Table `exam`
-- -----------------------------------------------------

ALTER TABLE `exam`
ADD COLUMN IF NOT EXISTS `quiz_name` VARCHAR(255) NULL AFTER `last_modified`,
ADD COLUMN IF NOT EXISTS `quiz_start_time` DATETIME NULL AFTER `quiz_name`,
ADD COLUMN IF NOT EXISTS `quiz_end_time` DATETIME NULL AFTER `quiz_start_time`,
ADD COLUMN IF NOT EXISTS `lms_available` INT(1) NULL AFTER `quiz_end_time`
;