-- -----------------------------------------------------
-- Alter Table `batch_action`
-- -----------------------------------------------------

ALTER TABLE `batch_action`
MODIFY `source_ids` VARCHAR(4000) NULL,
ADD COLUMN IF NOT EXISTS `attributes` VARCHAR(4000) NULL AFTER `action_type`
;
