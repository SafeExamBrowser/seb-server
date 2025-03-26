
-- -----------------------------------------------------
-- Alter Table `exam` add followup_id SEBSERV-225
-- -----------------------------------------------------
ALTER TABLE `exam`
ADD COLUMN IF NOT EXISTS `followup_id` BIGINT UNSIGNED NULL;