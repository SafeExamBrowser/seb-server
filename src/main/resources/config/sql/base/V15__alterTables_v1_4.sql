-- -----------------------------------------------------
-- Alter Table `batch_action`
-- -----------------------------------------------------

ALTER TABLE `batch_action`
MODIFY `source_ids` VARCHAR(4000) NULL,
ADD COLUMN IF NOT EXISTS `owner` VARCHAR(255) NULL AFTER `institution_id`,
ADD COLUMN IF NOT EXISTS `attributes` VARCHAR(4000) NULL AFTER `action_type`
;
