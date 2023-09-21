-- -----------------------------------------------------
-- Table `screen_proctoring_group`
-- -----------------------------------------------------

CREATE TABLE IF NOT EXISTS `screen_proctoring_group` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  `exam_id` BIGINT UNSIGNED NOT NULL,
  `uuid` VARCHAR(255) NOT NULL,
  `name` VARCHAR(255) NOT NULL,
  `size` INT NULL,
  `data` VARCHAR(4000) NULL,
  PRIMARY KEY (`id`),
  UNIQUE INDEX `uuid_UNIQUE` (`uuid` ASC),
  INDEX `screenProctoringGroupExamRef_idx` (`exam_id` ASC),
  CONSTRAINT `screenProctoringGroupExamRef`
    FOREIGN KEY (`exam_id`)
    REFERENCES `exam` (`id`)
    ON DELETE NO ACTION
    ON UPDATE NO ACTION);

-- -----------------------------------------------------
-- Alter Table `client_connection`
-- -----------------------------------------------------
ALTER TABLE `client_connection`
ADD COLUMN IF NOT EXISTS `screen_proctoring_group_id` BIGINT UNSIGNED NULL AFTER `update_time`,
ADD COLUMN IF NOT EXISTS `screen_proctoring_group_update` TINYINT(1) UNSIGNED NULL,
ADD CONSTRAINT `clientConnectionScreenProctoringGroupRef` 
    FOREIGN KEY IF NOT EXISTS (`screen_proctoring_group_id`) 
    REFERENCES `screen_proctoring_group` (`id`);