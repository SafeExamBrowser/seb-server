-- -----------------------------------------------------
-- Alter Table `client_connection`
-- -----------------------------------------------------

ALTER TABLE `client_connection`
MODIFY `client_machine_name` VARCHAR(255) NULL,
MODIFY `client_os_name` VARCHAR(255) NULL,
MODIFY `client_version` VARCHAR(255) NULL
;