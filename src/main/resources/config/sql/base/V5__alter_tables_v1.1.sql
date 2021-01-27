-- -----------------------------------------------------
-- Alter Table `client_event`
-- -----------------------------------------------------
ALTER TABLE `client_event`
MODIFY `numeric_value` DECIMAL(18,4) NULL;

-- -----------------------------------------------------
-- Alter Table `webservice_server_info`
-- -----------------------------------------------------
ALTER TABLE `webservice_server_info`
MODIFY `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
ADD COLUMN IF NOT EXISTS `master` INT(1) UNSIGNED NOT NULL DEFAULT 0,
ADD COLUMN IF NOT EXISTS `update_time` BIGINT NULL;