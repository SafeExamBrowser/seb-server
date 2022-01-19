-- -----------------------------------------------------
-- Table `exam_template`
-- -----------------------------------------------------
CREATE TABLE IF NOT EXISTS `exam_template` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  `institution_id` BIGINT UNSIGNED NOT NULL,
  `configuration_template_id` BIGINT UNSIGNED NULL,
  `name` VARCHAR(255) NOT NULL,
  `description` VARCHAR(4000) NULL,
  `exam_type` VARCHAR(45) NULL,
  `supporter` VARCHAR(4000) NULL,
  `indicator_templates` VARCHAR(6000) NULL,
  `institutional_default` INT(1) UNSIGNED NOT NULL DEFAULT 0,
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
ADD COLUMN IF NOT EXISTS `exam_template_id` BIGINT UNSIGNED NULL AFTER `active`,
ADD COLUMN IF NOT EXISTS `last_modified` BIGINT NULL AFTER `exam_template_id`;

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

-- -----------------------------------------------------
-- Table `client_indicator`
-- -----------------------------------------------------
CREATE TABLE IF NOT EXISTS `client_indicator` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  `client_connection_id` BIGINT UNSIGNED NOT NULL,
  `type` INT(2) NOT NULL,
  `value` BIGINT NULL,
  PRIMARY KEY (`id`),
  INDEX `clientIndicatorConnectionRef_idx` (`client_connection_id` ASC),
  INDEX `clientIndicatorType` (`type` ASC),
  CONSTRAINT `clientIndicatorConnectionRef`
    FOREIGN KEY (`client_connection_id`)
    REFERENCES `client_connection` (`id`)
    ON DELETE NO ACTION
    ON UPDATE NO ACTION)
;

-- -----------------------------------------------------
-- Table `client_notification`
-- -----------------------------------------------------
CREATE TABLE IF NOT EXISTS `client_notification` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  `client_connection_id` BIGINT UNSIGNED NOT NULL,
  `event_type` INT(2) NOT NULL,
  `notification_type` INT(2) NOT NULL,
  `value` BIGINT NULL,
  `text` VARCHAR(512) NULL,
  PRIMARY KEY (`id`),
  INDEX `clientNotificationConnectionRef_idx` (`client_connection_id` ASC),
  CONSTRAINT `clientNotificationConnectionRef`
    FOREIGN KEY (`client_connection_id`)
    REFERENCES `client_connection` (`id`)
    ON DELETE NO ACTION
    ON UPDATE NO ACTION)
;
