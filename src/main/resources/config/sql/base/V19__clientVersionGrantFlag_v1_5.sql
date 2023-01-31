-- -----------------------------------------------------
-- Alter Table  `client_connection`
-- -----------------------------------------------------
ALTER TABLE `client_connection`
ADD COLUMN IF NOT EXISTS `client_version_granted` TINYINT(1) UNSIGNED NULL AFTER `ask`;
