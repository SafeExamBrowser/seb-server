-- -----------------------------------------------------
-- Table `remote_proctoring_room`
-- -----------------------------------------------------
CREATE TABLE IF NOT EXISTS `remote_proctoring_room` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  `exam_id` BIGINT UNSIGNED NOT NULL,
  `name` VARCHAR(255) NOT NULL,
  `size` INT NULL,
  `subject` VARCHAR(255) NULL,
  PRIMARY KEY (`id`),
  INDEX `proctor_room_exam_id_idx` (`exam_id` ASC),
  CONSTRAINT `proctorRoomExamRef`
    FOREIGN KEY (`exam_id`)
    REFERENCES `exam` (`id`)
    ON DELETE NO ACTION
    ON UPDATE NO ACTION);
    
-- -----------------------------------------------------
-- Alter Table `client_connection`
-- -----------------------------------------------------
ALTER TABLE `client_connection`
ADD COLUMN IF NOT EXISTS `remote_proctoring_room_id` BIGINT UNSIGNED NULL,
ADD COLUMN IF NOT EXISTS `remote_proctoring_room_update` INT(1) UNSIGNED NULL,
ADD INDEX IF NOT EXISTS `clientConnectionProctorRoomRef_idx` (`remote_proctoring_room_id` ASC),
ADD CONSTRAINT `clientConnectionRemoteProctoringRoomRef` 
    FOREIGN KEY IF NOT EXISTS (`remote_proctoring_room_id`) 
    REFERENCES `remote_proctoring_room` (`id`);

-- -----------------------------------------------------
-- Alter Table `client_instruction`
-- -----------------------------------------------------
ALTER TABLE `client_instruction`
ADD COLUMN IF NOT EXISTS `needs_confirmation` INT(1) UNSIGNED NOT NULL DEFAULT 0 AFTER `attributes`,
ADD COLUMN IF NOT EXISTS `timestamp` BIGINT UNSIGNED NOT NULL DEFAULT 0 AFTER `needs_confirmation`;