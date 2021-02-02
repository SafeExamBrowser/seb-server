-- -----------------------------------------------------
-- Alter Table `client_connection`
-- -----------------------------------------------------
ALTER TABLE `client_connection`
CHANGE COLUMN `virtual_client_address` vdi_connection_id VARCHAR(45) NULL;