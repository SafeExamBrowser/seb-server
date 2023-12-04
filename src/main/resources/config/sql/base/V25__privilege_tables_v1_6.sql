-- -----------------------------------------------------
-- Table `entity_privilege`
-- -----------------------------------------------------
CREATE TABLE IF NOT EXISTS `entity_privilege` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  `entity_type` VARCHAR(45) NOT NULL,
  `entity_id` BIGINT UNSIGNED NOT NULL,
  `user_uuid` VARCHAR(255) NOT NULL,
  `privilege_type` TINYINT(1) NOT NULL,
  PRIMARY KEY (`id`))
;


-- -----------------------------------------------------
-- Table `feature_privilege`
-- -----------------------------------------------------
CREATE TABLE IF NOT EXISTS `feature_privilege` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  `feature_id` BIGINT UNSIGNED NOT NULL,
  `user_uuid` VARCHAR(255) NOT NULL,
  PRIMARY KEY (`id`))
;