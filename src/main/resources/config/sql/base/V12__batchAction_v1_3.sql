-- -----------------------------------------------------
-- Table `batch_action`
-- -----------------------------------------------------
CREATE TABLE IF NOT EXISTS `batch_action` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  `institution_id` BIGINT UNSIGNED NOT NULL,
  `action_type` VARCHAR(45) NOT NULL,
  `entity_type` VARCHAR(45) NOT NULL,
  `source_ids` VARCHAR(4000) NULL,
  `successful_ids` VARCHAR(4000) NULL,
  `failed_ids` VARCHAR(2000) NULL,
  `start_time` BIGINT NULL,
  `running` INT(1) NOT NULL,
  PRIMARY KEY (`id`),
  INDEX `batch_action_institution_ref_idx` (`institution_id` ASC),
  CONSTRAINT `batch_action_institution_ref`
    FOREIGN KEY (`institution_id`)
    REFERENCES `institution` (`id`)
    ON DELETE NO ACTION
    ON UPDATE NO ACTION)
;