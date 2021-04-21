-- -----------------------------------------------------
-- Table `certificate`
-- -----------------------------------------------------
CREATE TABLE IF NOT EXISTS `certificate` (
  `id` BIGINT UNSIGNED NOT NULL,
  `institution_id` BIGINT UNSIGNED NOT NULL,
  `aliases` VARCHAR(4000) NULL,
  `cert_store` BLOB NULL,
  PRIMARY KEY (`id`),
  INDEX `certificateInstitution_idx` (`institution_id` ASC),
  CONSTRAINT `certificateInstitution`
    FOREIGN KEY (`institution_id`)
    REFERENCES `institution` (`id`)
    ON DELETE NO ACTION
    ON UPDATE NO ACTION)
;