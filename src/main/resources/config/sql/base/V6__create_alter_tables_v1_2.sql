-- -----------------------------------------------------
-- Alter Table `client_connection`
-- -----------------------------------------------------
ALTER TABLE `client_connection`
RENAME COLUMN `virtual_client_address` TO `vdi_connection_id`;