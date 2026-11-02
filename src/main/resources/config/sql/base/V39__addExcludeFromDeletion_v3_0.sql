-- ----------------------------------------------------------------
-- Add  exclude_from_deletion to exam table SEBSERV-673
-- ----------------------------------------------------------------

ALTER TABLE `exam`
ADD COLUMN IF NOT EXISTS `exclude_from_deletion` INT(1) NULL
;