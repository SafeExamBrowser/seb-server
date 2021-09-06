-- -----------------------------------------------------
-- Table `exam_template`
-- -----------------------------------------------------
DROP TABLE IF EXISTS `exam_template` ;

CREATE TABLE IF NOT EXISTS `exam_template` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  `institution_id` BIGINT UNSIGNED NOT NULL,
  `configuration_template_id` BIGINT UNSIGNED NOT NULL,
  `name` VARCHAR(255) NOT NULL,
  `description` VARCHAR(4000) NULL,
  `exam_type` VARCHAR(45) NULL,
  `supporter` VARCHAR(4000) NULL,
  `indicator_templates` VARCHAR(6000) NULL,
  PRIMARY KEY (`id`),
  INDEX `examTemplateInstitutionRef_idx` (`institution_id` ASC),
  INDEX `examTemplateConfigTemplateRef_idx` (`configuration_template_id` ASC),
  CONSTRAINT `examTemplateInstitutionRef`
    FOREIGN KEY (`institution_id`)
    REFERENCES `institution` (`id`)
    ON DELETE NO ACTION
    ON UPDATE NO ACTION,
  CONSTRAINT `examTemplateConfigTemplateRef`
    FOREIGN KEY (`configuration_template_id`)
    REFERENCES `configuration_node` (`id`)
    ON DELETE NO ACTION
    ON UPDATE NO ACTION)
;

-- -----------------------------------------------------
-- Alter Table `exam`
-- -----------------------------------------------------
ALTER TABLE `exam`
ADD COLUMN IF NOT EXISTS `exam_template_id` BIGINT UNSIGNED NULL AFTER `active`;