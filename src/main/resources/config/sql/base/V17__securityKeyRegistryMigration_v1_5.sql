-- -----------------------------------------------------
-- Table `seb_security_key_registry`
-- -----------------------------------------------------
DROP TABLE IF EXISTS `seb_security_key_registry` ;

CREATE TABLE IF NOT EXISTS `seb_security_key_registry` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  `institution_id` BIGINT UNSIGNED NOT NULL,
  `type` VARCHAR(45) NOT NULL,
  `key` VARCHAR(4000) NOT NULL,
  `tag` VARCHAR(255) NULL,
  `exam_id` BIGINT UNSIGNED NULL,
  `exam_template_id` BIGINT UNSIGNED NULL,
  PRIMARY KEY (`id`),
  INDEX `segKeyReg_exam_ref_idx` (`exam_id` ASC),
  INDEX `segKeyReg_exam_template_ref_idx` (`exam_template_id` ASC),
  INDEX `segKeyReg_institution_ref_idx` (`institution_id` ASC),
  CONSTRAINT `segKeyReg_exam_ref`
    FOREIGN KEY (`exam_id`)
    REFERENCES `exam` (`id`)
    ON DELETE NO ACTION
    ON UPDATE NO ACTION,
  CONSTRAINT `segKeyReg_exam_template_ref`
    FOREIGN KEY (`exam_template_id`)
    REFERENCES `exam_template` (`id`)
    ON DELETE NO ACTION
    ON UPDATE NO ACTION,
  CONSTRAINT `segKeyReg_institution_ref`
    FOREIGN KEY (`institution_id`)
    REFERENCES `institution` (`id`)
    ON DELETE NO ACTION
    ON UPDATE NO ACTION)
;

-- -----------------------------------------------------
-- Alter Table  `client_connection`
-- -----------------------------------------------------
ALTER TABLE `client_connection`
ADD COLUMN IF NOT EXISTS `security_check_granted` TINYINT(1) UNSIGNED NULL AFTER `client_version`
;
