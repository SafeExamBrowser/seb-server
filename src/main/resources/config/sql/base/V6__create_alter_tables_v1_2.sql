-- -----------------------------------------------------
-- Alter Table `client_connection`
-- -----------------------------------------------------
ALTER TABLE `client_connection`
ADD COLUMN IF NOT EXISTS `vdi` INT(1) UNSIGNED NULL DEFAULT 0 AFTER `virtual_client_address`,
ADD COLUMN IF NOT EXISTS `vdi_pair_token` VARCHAR(255) NULL AFTER `vdi`;

-- -----------------------------------------------------
-- Alter Table `client_connection`
-- -----------------------------------------------------
ALTER TABLE `remote_proctoring_room`
ADD COLUMN IF NOT EXISTS `break_out_connections` VARCHAR(4000) NULL;