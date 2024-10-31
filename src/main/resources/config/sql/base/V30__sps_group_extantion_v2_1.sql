-- ----------------------------------------------------------------
-- Add collecting_strategy and seb_group_id to screen_proctoring_group table SEBSP-116
-- ----------------------------------------------------------------

ALTER TABLE `screen_proctoring_group`
ADD COLUMN IF NOT EXISTS `collecting_strategy` VARCHAR(45) NULL,
ADD COLUMN IF NOT EXISTS `seb_group_id` BIGINT UNSIGNED NULL;