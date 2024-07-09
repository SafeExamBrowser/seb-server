-- -----------------------------------------------------
-- Table `institution`
-- -----------------------------------------------------
ALTER TABLE `SEBServer`.`institution` CHARACTER SET = utf8mb4, COLLATE = utf8mb4_general_ci,
  CHANGE COLUMN `name` `name` VARCHAR(255) CHARACTER SET 'utf8mb4' COLLATE 'utf8mb4_general_ci' NOT NULL,
  CHANGE COLUMN `url_suffix` `url_suffix` VARCHAR(45) CHARACTER SET 'utf8mb4' COLLATE 'utf8mb4_general_ci' NULL,
  CHANGE COLUMN `theme_name` `theme_name` VARCHAR(45) CHARACTER SET 'utf8mb4' COLLATE 'utf8mb4_general_ci' NULL;

-- -----------------------------------------------------
-- Table `lms_setup`
-- -----------------------------------------------------
ALTER TABLE `SEBServer`.`lms_setup` CHARACTER SET = utf8mb4, COLLATE = utf8mb4_general_ci,
  CHANGE COLUMN `name` `name` VARCHAR(255) CHARACTER SET 'utf8mb4' COLLATE 'utf8mb4_general_ci' NOT NULL,
  CHANGE COLUMN `lms_type` `lms_type` VARCHAR(45) CHARACTER SET 'utf8mb4' COLLATE 'utf8mb4_general_ci' NOT NULL,
  CHANGE COLUMN `lms_url` `lms_url` VARCHAR(255) CHARACTER SET 'utf8mb4' COLLATE 'utf8mb4_general_ci' NULL,
  CHANGE COLUMN `lms_clientname` `lms_clientname` VARCHAR(4000) CHARACTER SET 'utf8mb4' COLLATE 'utf8mb4_general_ci' NULL,
  CHANGE COLUMN `lms_clientsecret` `lms_clientsecret` VARCHAR(4000) CHARACTER SET 'utf8mb4' COLLATE 'utf8mb4_general_ci' NULL,
  CHANGE COLUMN `lms_rest_api_token` `lms_rest_api_token` VARCHAR(4000) CHARACTER SET 'utf8mb4' COLLATE 'utf8mb4_general_ci' NULL,
  CHANGE COLUMN `lms_proxy_host` `lms_proxy_host` VARCHAR(255) CHARACTER SET 'utf8mb4' COLLATE 'utf8mb4_general_ci' NULL,
  CHANGE COLUMN `lms_proxy_auth_username` `lms_proxy_auth_username` VARCHAR(255) CHARACTER SET 'utf8mb4' COLLATE 'utf8mb4_general_ci' NULL,
  CHANGE COLUMN `lms_proxy_auth_secret` `lms_proxy_auth_secret` VARCHAR(255) CHARACTER SET 'utf8mb4' COLLATE 'utf8mb4_general_ci' NULL,
  CHANGE COLUMN `connection_id` `connection_id` VARCHAR(255) CHARACTER SET 'utf8mb4' COLLATE 'utf8mb4_general_ci' NULL;

-- -----------------------------------------------------
-- Table `exam`
-- -----------------------------------------------------
ALTER TABLE `SEBServer`.`exam` CHARACTER SET = utf8mb4, COLLATE = utf8mb4_general_ci,
  CHANGE COLUMN `external_id` `external_id` VARCHAR(255) CHARACTER SET 'utf8mb4' COLLATE 'utf8mb4_general_ci' NOT NULL,
  CHANGE COLUMN `owner` `owner` VARCHAR(255) CHARACTER SET 'utf8mb4' COLLATE 'utf8mb4_general_ci' NOT NULL,
  CHANGE COLUMN `supporter` `supporter` VARCHAR(4000) CHARACTER SET 'utf8mb4' COLLATE 'utf8mb4_general_ci' NULL COMMENT 'comma separated list of user_uuid',
  CHANGE COLUMN `type` `type` VARCHAR(45) CHARACTER SET 'utf8mb4' COLLATE 'utf8mb4_general_ci' NOT NULL,
  CHANGE COLUMN `quit_password` `quit_password` VARCHAR(4000) CHARACTER SET 'utf8mb4' COLLATE 'utf8mb4_general_ci' NULL,
  CHANGE COLUMN `browser_keys` `browser_keys` VARCHAR(4000) CHARACTER SET 'utf8mb4' COLLATE 'utf8mb4_general_ci' NULL,
  CHANGE COLUMN `status` `status` VARCHAR(255) CHARACTER SET 'utf8mb4' COLLATE 'utf8mb4_general_ci' NOT NULL,
  CHANGE COLUMN `lastUpdate` `lastUpdate` VARCHAR(255) CHARACTER SET 'utf8mb4' COLLATE 'utf8mb4_general_ci' NULL,
  CHANGE COLUMN `quiz_name` `quiz_name` VARCHAR(255) CHARACTER SET 'utf8mb4' COLLATE 'utf8mb4_general_ci' NULL;

-- -----------------------------------------------------
-- Table `remote_proctoring_room`
-- -----------------------------------------------------
ALTER TABLE `SEBServer`.`remote_proctoring_room` CHARACTER SET = utf8mb4, COLLATE = utf8mb4_general_ci,
  CHANGE COLUMN `name` `name` VARCHAR(255) CHARACTER SET 'utf8mb4' COLLATE 'utf8mb4_general_ci' NOT NULL,
  CHANGE COLUMN `subject` `subject` VARCHAR(255) CHARACTER SET 'utf8mb4' COLLATE 'utf8mb4_general_ci' NULL,
  CHANGE COLUMN `break_out_connections` `break_out_connections` VARCHAR(4000) CHARACTER SET 'utf8mb4' COLLATE 'utf8mb4_general_ci' NULL,
  CHANGE COLUMN `join_key` `join_key` VARCHAR(255) CHARACTER SET 'utf8mb4' COLLATE 'utf8mb4_general_ci' NOT NULL,
  CHANGE COLUMN `room_data` `room_data` VARCHAR(4000) CHARACTER SET 'utf8mb4' COLLATE 'utf8mb4_general_ci' NULL;

-- -----------------------------------------------------
-- Table `screen_proctoring_group`
-- -----------------------------------------------------
ALTER TABLE `SEBServer`.`screen_proctoring_group` CHARACTER SET = utf8mb4, COLLATE = utf8mb4_general_ci,
  CHANGE COLUMN `uuid` `uuid` VARCHAR(255) CHARACTER SET 'utf8mb4' COLLATE 'utf8mb4_general_ci' NOT NULL,
  CHANGE COLUMN `name` `name` VARCHAR(255) CHARACTER SET 'utf8mb4' COLLATE 'utf8mb4_general_ci' NOT NULL,
  CHANGE COLUMN `data` `data` VARCHAR(4000) CHARACTER SET 'utf8mb4' COLLATE 'utf8mb4_general_ci' NULL;

-- -----------------------------------------------------
-- Table `client_connection`
-- -----------------------------------------------------
ALTER TABLE `SEBServer`.`client_connection` CHARACTER SET = utf8mb4, COLLATE = utf8mb4_general_ci,
  CHANGE COLUMN `status` `status` VARCHAR(45) CHARACTER SET 'utf8mb4' COLLATE 'utf8mb4_general_ci' NOT NULL,
  CHANGE COLUMN `connection_token` `connection_token` VARCHAR(255) CHARACTER SET 'utf8mb4' COLLATE 'utf8mb4_general_ci' NOT NULL,
  CHANGE COLUMN `exam_user_session_id` `exam_user_session_id` VARCHAR(255) CHARACTER SET 'utf8mb4' COLLATE 'utf8mb4_general_ci' NULL,
  CHANGE COLUMN `client_address` `client_address` VARCHAR(45) CHARACTER SET 'utf8mb4' COLLATE 'utf8mb4_general_ci' NOT NULL,
  CHANGE COLUMN `virtual_client_address` `virtual_client_address` VARCHAR(45) CHARACTER SET 'utf8mb4' COLLATE 'utf8mb4_general_ci' NULL,
  CHANGE COLUMN `vdi_pair_token` `vdi_pair_token` VARCHAR(255) CHARACTER SET 'utf8mb4' COLLATE 'utf8mb4_general_ci' NULL,
  CHANGE COLUMN `client_machine_name` `client_machine_name` VARCHAR(255) CHARACTER SET 'utf8mb4' COLLATE 'utf8mb4_general_ci' NULL,
  CHANGE COLUMN `client_os_name` `client_os_name` VARCHAR(255) CHARACTER SET 'utf8mb4' COLLATE 'utf8mb4_general_ci' NULL,
  CHANGE COLUMN `client_version` `client_version` VARCHAR(255) CHARACTER SET 'utf8mb4' COLLATE 'utf8mb4_general_ci' NULL,
  CHANGE COLUMN `ask` `ask` VARCHAR(255) CHARACTER SET 'utf8mb4' COLLATE 'utf8mb4_general_ci' NULL;

-- -----------------------------------------------------
-- Table `client_event`
-- -----------------------------------------------------
ALTER TABLE `SEBServer`.`client_event` CHARACTER SET = utf8mb4, COLLATE = utf8mb4_general_ci,
  CHANGE COLUMN `text` `text` VARCHAR(512) CHARACTER SET 'utf8mb4' COLLATE 'utf8mb4_general_ci' NULL;

-- -----------------------------------------------------
-- Table `indicator`
-- -----------------------------------------------------
ALTER TABLE `SEBServer`.`indicator` CHARACTER SET = utf8mb4, COLLATE = utf8mb4_general_ci,
  CHANGE COLUMN `type` `type` VARCHAR(45) CHARACTER SET 'utf8mb4' COLLATE 'utf8mb4_general_ci' NOT NULL,
  CHANGE COLUMN `name` `name` VARCHAR(45) CHARACTER SET 'utf8mb4' COLLATE 'utf8mb4_general_ci' NOT NULL,
  CHANGE COLUMN `color` `color` VARCHAR(45) CHARACTER SET 'utf8mb4' COLLATE 'utf8mb4_general_ci' NULL,
  CHANGE COLUMN `icon` `icon` VARCHAR(45) CHARACTER SET 'utf8mb4' COLLATE 'utf8mb4_general_ci' NULL,
  CHANGE COLUMN `tags` `tags` VARCHAR(255) CHARACTER SET 'utf8mb4' COLLATE 'utf8mb4_general_ci' NULL;

-- -----------------------------------------------------
-- Table `configuration_node`
-- -----------------------------------------------------
ALTER TABLE `SEBServer`.`configuration_node` CHARACTER SET = utf8mb4, COLLATE = utf8mb4_general_ci,
  CHANGE COLUMN `owner` `owner` VARCHAR(255) CHARACTER SET 'utf8mb4' COLLATE 'utf8mb4_general_ci' NOT NULL,
  CHANGE COLUMN `name` `name` VARCHAR(255) CHARACTER SET 'utf8mb4' COLLATE 'utf8mb4_general_ci' NOT NULL,
  CHANGE COLUMN `description` `description` VARCHAR(4000) CHARACTER SET 'utf8mb4' COLLATE 'utf8mb4_general_ci' NULL,
  CHANGE COLUMN `type` `type` VARCHAR(45) CHARACTER SET 'utf8mb4' COLLATE 'utf8mb4_general_ci' NULL,
  CHANGE COLUMN `status` `status` VARCHAR(45) CHARACTER SET 'utf8mb4' COLLATE 'utf8mb4_general_ci' NOT NULL,
  CHANGE COLUMN `last_update_user` `last_update_user` VARCHAR(255) CHARACTER SET 'utf8mb4' COLLATE 'utf8mb4_general_ci' NULL;

-- -----------------------------------------------------
-- Table `configuration`
-- -----------------------------------------------------
ALTER TABLE `SEBServer`.`configuration` CHARACTER SET = utf8mb4, COLLATE = utf8mb4_general_ci,
  CHANGE COLUMN `version` `version` VARCHAR(255) CHARACTER SET 'utf8mb4' COLLATE 'utf8mb4_general_ci' NULL;

-- -----------------------------------------------------
-- Table `configuration_attribute`
-- -----------------------------------------------------
ALTER TABLE `SEBServer`.`configuration_attribute` CHARACTER SET = utf8mb4, COLLATE = utf8mb4_general_ci,
  CHANGE COLUMN `name` `name` VARCHAR(45) CHARACTER SET 'utf8mb4' COLLATE 'utf8mb4_general_ci' NOT NULL,
  CHANGE COLUMN `type` `type` VARCHAR(45) CHARACTER SET 'utf8mb4' COLLATE 'utf8mb4_general_ci' NOT NULL,
  CHANGE COLUMN `resources` `resources` VARCHAR(255) CHARACTER SET 'utf8mb4' COLLATE 'utf8mb4_general_ci' NULL,
  CHANGE COLUMN `validator` `validator` VARCHAR(45) CHARACTER SET 'utf8mb4' COLLATE 'utf8mb4_general_ci' NULL,
  CHANGE COLUMN `dependencies` `dependencies` VARCHAR(255) CHARACTER SET 'utf8mb4' COLLATE 'utf8mb4_general_ci' NULL,
  CHANGE COLUMN `default_value` `default_value` VARCHAR(255) CHARACTER SET 'utf8mb4' COLLATE 'utf8mb4_general_ci' NULL;

-- -----------------------------------------------------
-- Table `configuration_value`
-- -----------------------------------------------------
ALTER TABLE `SEBServer`.`configuration_value` CHARACTER SET = utf8mb4, COLLATE = utf8mb4_general_ci,
  CHANGE COLUMN `value` `value` VARCHAR(16000) CHARACTER SET 'utf8mb4' COLLATE 'utf8mb4_general_ci' NULL;

-- -----------------------------------------------------
-- Table `view`
-- -----------------------------------------------------
ALTER TABLE `SEBServer`.`view` CHARACTER SET = utf8mb4, COLLATE = utf8mb4_general_ci,
  CHANGE COLUMN `name` `name` VARCHAR(255) CHARACTER SET 'utf8mb4' COLLATE 'utf8mb4_general_ci' NULL;

-- -----------------------------------------------------
-- Table `orientation`
-- -----------------------------------------------------
ALTER TABLE `SEBServer`.`orientation` CHARACTER SET = utf8mb4, COLLATE = utf8mb4_general_ci,
  CHANGE COLUMN `group_id` `group_id` VARCHAR(45) CHARACTER SET 'utf8mb4' COLLATE 'utf8mb4_general_ci' NULL,
  CHANGE COLUMN `title` `title` VARCHAR(45) CHARACTER SET 'utf8mb4' COLLATE 'utf8mb4_general_ci' NULL;

-- -----------------------------------------------------
-- Table `exam_configuration_map`
-- -----------------------------------------------------
ALTER TABLE `SEBServer`.`exam_configuration_map` CHARACTER SET = utf8mb4, COLLATE = utf8mb4_general_ci,
  CHANGE COLUMN `encrypt_secret` `encrypt_secret` VARCHAR(255) CHARACTER SET 'utf8mb4' COLLATE 'utf8mb4_general_ci' NULL;

-- -----------------------------------------------------
-- Table `user`
-- -----------------------------------------------------
ALTER TABLE `SEBServer`.`user` CHARACTER SET = utf8mb4, COLLATE = utf8mb4_general_ci,
  CHANGE COLUMN `uuid` `uuid` VARCHAR(255) CHARACTER SET 'utf8mb4' COLLATE 'utf8mb4_general_ci' NOT NULL,
  CHANGE COLUMN `name` `name` VARCHAR(255) CHARACTER SET 'utf8mb4' COLLATE 'utf8mb4_general_ci' NOT NULL,
  CHANGE COLUMN `surname` `surname` VARCHAR(255) CHARACTER SET 'utf8mb4' COLLATE 'utf8mb4_general_ci' NULL,
  CHANGE COLUMN `username` `username` VARCHAR(255) CHARACTER SET 'utf8mb4' COLLATE 'utf8mb4_general_ci' NOT NULL,
  CHANGE COLUMN `password` `password` VARCHAR(255) CHARACTER SET 'utf8mb4' COLLATE 'utf8mb4_general_ci' NOT NULL,
  CHANGE COLUMN `email` `email` VARCHAR(255) CHARACTER SET 'utf8mb4' COLLATE 'utf8mb4_general_ci' NULL,
  CHANGE COLUMN `language` `language` VARCHAR(45) CHARACTER SET 'utf8mb4' COLLATE 'utf8mb4_general_ci' NOT NULL,
  CHANGE COLUMN `timeZone` `timeZone` VARCHAR(45) CHARACTER SET 'utf8mb4' COLLATE 'utf8mb4_general_ci' NOT NULL;

-- -----------------------------------------------------
-- Table `user_role`
-- -----------------------------------------------------
ALTER TABLE `SEBServer`.`user_role` CHARACTER SET = utf8mb4, COLLATE = utf8mb4_general_ci,
  CHANGE COLUMN `role_name` `role_name` VARCHAR(45) CHARACTER SET 'utf8mb4' COLLATE 'utf8mb4_general_ci' NOT NULL;

-- -----------------------------------------------------
-- Table `threshold`
-- -----------------------------------------------------
ALTER TABLE `SEBServer`.`threshold` CHARACTER SET = utf8mb4, COLLATE = utf8mb4_general_ci,
  CHANGE COLUMN `color` `color` VARCHAR(45) CHARACTER SET 'utf8mb4' COLLATE 'utf8mb4_general_ci' NULL,
  CHANGE COLUMN `icon` `icon` VARCHAR(45) CHARACTER SET 'utf8mb4' COLLATE 'utf8mb4_general_ci' NULL;

-- -----------------------------------------------------
-- Table `user_activity_log`
-- -----------------------------------------------------
ALTER TABLE `SEBServer`.`user_activity_log` CHARACTER SET = utf8mb4, COLLATE = utf8mb4_general_ci,
  CHANGE COLUMN `user_uuid` `user_uuid` VARCHAR(255) CHARACTER SET 'utf8mb4' COLLATE 'utf8mb4_general_ci' NOT NULL,
  CHANGE COLUMN `activity_type` `activity_type` VARCHAR(45) CHARACTER SET 'utf8mb4' COLLATE 'utf8mb4_general_ci' NOT NULL,
  CHANGE COLUMN `entity_type` `entity_type` VARCHAR(45) CHARACTER SET 'utf8mb4' COLLATE 'utf8mb4_general_ci' NOT NULL,
  CHANGE COLUMN `entity_id` `entity_id` VARCHAR(255) CHARACTER SET 'utf8mb4' COLLATE 'utf8mb4_general_ci' NOT NULL,
  CHANGE COLUMN `message` `message` VARCHAR(4000) CHARACTER SET 'utf8mb4' COLLATE 'utf8mb4_general_ci' NULL;

-- -----------------------------------------------------
-- Table `additional_attributes`
-- -----------------------------------------------------
ALTER TABLE `SEBServer`.`additional_attributes` CHARACTER SET = utf8mb4, COLLATE = utf8mb4_general_ci,
  CHANGE COLUMN `entity_type` `entity_type` VARCHAR(45) CHARACTER SET 'utf8mb4' COLLATE 'utf8mb4_general_ci' NOT NULL,
  CHANGE COLUMN `name` `name` VARCHAR(255) CHARACTER SET 'utf8mb4' COLLATE 'utf8mb4_general_ci' NOT NULL,
  CHANGE COLUMN `value` `value` VARCHAR(4000) CHARACTER SET 'utf8mb4' COLLATE 'utf8mb4_general_ci' NULL;

-- -----------------------------------------------------
-- Table `seb_client_configuration`
-- -----------------------------------------------------
ALTER TABLE `SEBServer`.`seb_client_configuration` CHARACTER SET = utf8mb4, COLLATE = utf8mb4_general_ci,
  CHANGE COLUMN `name` `name` VARCHAR(255) CHARACTER SET 'utf8mb4' COLLATE 'utf8mb4_general_ci' NOT NULL,
  CHANGE COLUMN `client_name` `client_name` VARCHAR(4000) CHARACTER SET 'utf8mb4' COLLATE 'utf8mb4_general_ci' NOT NULL,
  CHANGE COLUMN `client_secret` `client_secret` VARCHAR(4000) CHARACTER SET 'utf8mb4' COLLATE 'utf8mb4_general_ci' NOT NULL,
  CHANGE COLUMN `encrypt_secret` `encrypt_secret` VARCHAR(255) CHARACTER SET 'utf8mb4' COLLATE 'utf8mb4_general_ci' NULL,
  CHANGE COLUMN `last_update_user` `last_update_user` VARCHAR(255) CHARACTER SET 'utf8mb4' COLLATE 'utf8mb4_general_ci' NULL;

-- -----------------------------------------------------
-- Table `webservice_server_info`
-- -----------------------------------------------------
ALTER TABLE `SEBServer`.`webservice_server_info` CHARACTER SET = utf8mb4, COLLATE = utf8mb4_general_ci,
  CHANGE COLUMN `uuid` `uuid` VARCHAR(255) CHARACTER SET 'utf8mb4' COLLATE 'utf8mb4_general_ci' NOT NULL,
  CHANGE COLUMN `service_address` `service_address` VARCHAR(255) CHARACTER SET 'utf8mb4' COLLATE 'utf8mb4_general_ci' NOT NULL;

-- -----------------------------------------------------
-- Table `client_instruction`
-- -----------------------------------------------------
ALTER TABLE `SEBServer`.`client_instruction` CHARACTER SET = utf8mb4, COLLATE = utf8mb4_general_ci,
  CHANGE COLUMN `connection_token` `connection_token` VARCHAR(255) CHARACTER SET 'utf8mb4' COLLATE 'utf8mb4_general_ci' NOT NULL,
  CHANGE COLUMN `type` `type` VARCHAR(45) CHARACTER SET 'utf8mb4' COLLATE 'utf8mb4_general_ci' NOT NULL,
  CHANGE COLUMN `attributes` `attributes` VARCHAR(4000) CHARACTER SET 'utf8mb4' COLLATE 'utf8mb4_general_ci' NULL;

-- -----------------------------------------------------
-- Table `certificate`
-- -----------------------------------------------------
ALTER TABLE `SEBServer`.`certificate` CHARACTER SET = utf8mb4, COLLATE = utf8mb4_general_ci,
  CHANGE COLUMN `aliases` `aliases` VARCHAR(4000) CHARACTER SET 'utf8mb4' COLLATE 'utf8mb4_general_ci' NULL;

-- -----------------------------------------------------
-- Table `exam_template`
-- -----------------------------------------------------
ALTER TABLE `SEBServer`.`exam_template` CHARACTER SET = utf8mb4, COLLATE = utf8mb4_general_ci,
  CHANGE COLUMN `name` `name` VARCHAR(255) CHARACTER SET 'utf8mb4' COLLATE 'utf8mb4_general_ci' NOT NULL,
  CHANGE COLUMN `description` `description` VARCHAR(4000) CHARACTER SET 'utf8mb4' COLLATE 'utf8mb4_general_ci' NULL,
  CHANGE COLUMN `exam_type` `exam_type` VARCHAR(45) CHARACTER SET 'utf8mb4' COLLATE 'utf8mb4_general_ci' NULL,
  CHANGE COLUMN `supporter` `supporter` VARCHAR(4000) CHARACTER SET 'utf8mb4' COLLATE 'utf8mb4_general_ci' NULL,
  CHANGE COLUMN `indicator_templates` `indicator_templates` VARCHAR(6000) CHARACTER SET 'utf8mb4' COLLATE 'utf8mb4_general_ci' NULL;

-- -----------------------------------------------------
-- Table `batch_action`
-- -----------------------------------------------------
ALTER TABLE `SEBServer`.`batch_action` CHARACTER SET = utf8mb4, COLLATE = utf8mb4_general_ci,
  CHANGE COLUMN `owner` `owner` VARCHAR(255) CHARACTER SET 'utf8mb4' COLLATE 'utf8mb4_general_ci' NULL,
  CHANGE COLUMN `action_type` `action_type` VARCHAR(45) CHARACTER SET 'utf8mb4' COLLATE 'utf8mb4_general_ci' NOT NULL,
  CHANGE COLUMN `attributes` `attributes` VARCHAR(4000) CHARACTER SET 'utf8mb4' COLLATE 'utf8mb4_general_ci' NULL,
  CHANGE COLUMN `source_ids` `source_ids` VARCHAR(4000) CHARACTER SET 'utf8mb4' COLLATE 'utf8mb4_general_ci' NULL,
  CHANGE COLUMN `successful` `successful` VARCHAR(4000) CHARACTER SET 'utf8mb4' COLLATE 'utf8mb4_general_ci' NULL,
  CHANGE COLUMN `processor_id` `processor_id` VARCHAR(45) CHARACTER SET 'utf8mb4' COLLATE 'utf8mb4_general_ci' NULL;

-- -----------------------------------------------------
-- Table `client_notification`
-- -----------------------------------------------------
ALTER TABLE `SEBServer`.`client_notification` CHARACTER SET = utf8mb4, COLLATE = utf8mb4_general_ci,
  CHANGE COLUMN `text` `text` VARCHAR(512) CHARACTER SET 'utf8mb4' COLLATE 'utf8mb4_general_ci' NULL;

-- -----------------------------------------------------
-- Table `client_group`
-- -----------------------------------------------------
ALTER TABLE `SEBServer`.`client_group` CHARACTER SET = utf8mb4, COLLATE = utf8mb4_general_ci,
  CHANGE COLUMN `name` `name` VARCHAR(255) CHARACTER SET 'utf8mb4' COLLATE 'utf8mb4_general_ci' NOT NULL,
  CHANGE COLUMN `type` `type` VARCHAR(45) CHARACTER SET 'utf8mb4' COLLATE 'utf8mb4_general_ci' NOT NULL,
  CHANGE COLUMN `color` `color` VARCHAR(45) CHARACTER SET 'utf8mb4' COLLATE 'utf8mb4_general_ci' NULL,
  CHANGE COLUMN `icon` `icon` VARCHAR(45) CHARACTER SET 'utf8mb4' COLLATE 'utf8mb4_general_ci' NULL,
  CHANGE COLUMN `data` `data` VARCHAR(4000) CHARACTER SET 'utf8mb4' COLLATE 'utf8mb4_general_ci' NULL;

-- -----------------------------------------------------
-- Table `seb_security_key_registry`
-- -----------------------------------------------------
ALTER TABLE `SEBServer`.`seb_security_key_registry` CHARACTER SET = utf8mb4, COLLATE = utf8mb4_general_ci,
  CHANGE COLUMN `key_type` `key_type` VARCHAR(45) CHARACTER SET 'utf8mb4' COLLATE 'utf8mb4_general_ci' NOT NULL,
  CHANGE COLUMN `key_value` `key_value` VARCHAR(4000) CHARACTER SET 'utf8mb4' COLLATE 'utf8mb4_general_ci' NOT NULL,
  CHANGE COLUMN `tag` `tag` VARCHAR(255) CHARACTER SET 'utf8mb4' COLLATE 'utf8mb4_general_ci' NULL;

-- -----------------------------------------------------
-- Table `entity_privilege`
-- -----------------------------------------------------
ALTER TABLE `SEBServer`.`entity_privilege` CHARACTER SET = utf8mb4, COLLATE = utf8mb4_general_ci,
  CHANGE COLUMN `entity_type` `entity_type` VARCHAR(45) CHARACTER SET 'utf8mb4' COLLATE 'utf8mb4_general_ci' NOT NULL,
  CHANGE COLUMN `user_uuid` `user_uuid` VARCHAR(255) CHARACTER SET 'utf8mb4' COLLATE 'utf8mb4_general_ci' NOT NULL;

-- -----------------------------------------------------
-- Table `feature_privilege`
-- -----------------------------------------------------

ALTER TABLE `SEBServer`.`feature_privilege` CHARACTER SET = utf8mb4, COLLATE = utf8mb4_general_ci,
  CHANGE COLUMN `user_uuid` `user_uuid` VARCHAR(255) CHARACTER SET 'utf8mb4' COLLATE 'utf8mb4_general_ci' NOT NULL;
