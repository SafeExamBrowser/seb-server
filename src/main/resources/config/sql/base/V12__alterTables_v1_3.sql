-- -----------------------------------------------------
-- Alter Table `client_connection`
-- -----------------------------------------------------

ALTER TABLE `client_connection`
ADD COLUMN IF NOT EXISTS `client_machine_name` VARCHAR(45) NULL,
ADD COLUMN IF NOT EXISTS `client_os_name` VARCHAR(45) NULL,
ADD COLUMN IF NOT EXISTS `client_version` VARCHAR(45) NULL
;