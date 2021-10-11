-- -----------------------------------------------------
-- Table `batch_action`
-- -----------------------------------------------------
CREATE TABLE IF NOT EXISTS `batch_action` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  `institution_id` BIGINT UNSIGNED NOT NULL,
  `action_type` VARCHAR(45) NOT NULL,
  `source_ids` VARCHAR(8000) NULL,
  `successful` VARCHAR(4000) NULL,
  `last_update` BIGINT NULL,
  `processor_id` VARCHAR(45) NULL,
  PRIMARY KEY (`id`),
  INDEX `batch_action_institution_ref_idx` (`institution_id` ASC),
  CONSTRAINT `batch_action_institution_ref`
    FOREIGN KEY (`institution_id`)
    REFERENCES `institution` (`id`)
    ON DELETE NO ACTION
    ON UPDATE NO ACTION)
;