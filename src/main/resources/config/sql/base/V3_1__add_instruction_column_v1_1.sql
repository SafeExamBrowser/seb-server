ALTER TABLE `client_instruction`
ADD COLUMN IF NOT EXISTS `needs_confirmation` INT(1) UNSIGNED NOT NULL DEFAULT 0 AFTER `attributes`,
ADD COLUMN IF NOT EXISTS `timestamp` BIGINT UNSIGNED NOT NULL DEFAULT 0 AFTER `needs_confirmation`;