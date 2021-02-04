-- -----------------------------------------------------
-- Alter Table `client_connection`
-- -----------------------------------------------------
ALTER TABLE `client_connection`
CHANGE COLUMN `virtual_client_address` client_name VARCHAR(45) NULL,
ADD COLUMN IF NOT EXISTS `vdi` INT(1) UNSIGNED NULL DEFAULT 0,
ADD COLUMN IF NOT EXISTS `vdi_pair_token` VARCHAR(255) NULL;