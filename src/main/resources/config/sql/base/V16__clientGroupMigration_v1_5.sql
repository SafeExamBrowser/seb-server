-- -----------------------------------------------------
-- Table `client_group`
-- -----------------------------------------------------

CREATE TABLE IF NOT EXISTS `client_group` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  `exam_id` BIGINT UNSIGNED NOT NULL,
  `name` VARCHAR(255) NOT NULL,
  `type` VARCHAR(45) NOT NULL,
  `color` VARCHAR(45) NULL,
  `icon` VARCHAR(45) NULL,
  `data` VARCHAR(4000) NULL,
  PRIMARY KEY (`id`),
  INDEX `clientGroupExamRef_idx` (`exam_id` ASC),
  CONSTRAINT `clientGroupExamRef`
    FOREIGN KEY (`exam_id`)
    REFERENCES `exam` (`id`)
    ON DELETE NO ACTION
    ON UPDATE NO ACTION)
;

-- -----------------------------------------------------
-- Alter Table `exam_configuration_map`
-- -----------------------------------------------------
ALTER TABLE `exam_configuration_map`
DROP COLUMN IF EXISTS `user_names`,
ADD COLUMN IF NOT EXISTS `client_group_id` BIGINT NULL AFTER `encrypt_secret`
;