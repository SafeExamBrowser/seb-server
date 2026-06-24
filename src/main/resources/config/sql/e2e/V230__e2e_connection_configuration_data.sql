-- Connection Configuration (seb_client_configuration) READ Get-All e2e fixtures.
-- Self-contained: all rows belong to the shared base institution 11 ('sebserv').
-- A distinct 'e2e-getall-connection-config' name token keeps these independent
-- of any other seeded connection configurations.

INSERT INTO seb_client_configuration
(id, institution_id, name, date, client_name, client_secret, encrypt_secret, active, last_update_time, last_update_user)
VALUES
    -- active + inactive pair (status filter) sharing the search token
    (9001, 11, 'e2e-getall-connection-config-01', NOW(), 'e2e-client', 'e2e-secret', NULL, 1, 1773070766921, 'test-main'),
    (9002, 11, 'e2e-getall-connection-config-02', NOW(), 'e2e-client', 'e2e-secret', NULL, 0, 1773070766921, 'test-main'),
    -- extra matching rows so a 2nd page exists at page_size=5
    (9003, 11, 'e2e-getall-connection-config-03', NOW(), 'e2e-client', 'e2e-secret', NULL, 1, 1773070766921, 'test-main'),
    (9004, 11, 'e2e-getall-connection-config-04', NOW(), 'e2e-client', 'e2e-secret', NULL, 1, 1773070766921, 'test-main'),
    (9005, 11, 'e2e-getall-connection-config-05', NOW(), 'e2e-client', 'e2e-secret', NULL, 1, 1773070766921, 'test-main'),
    (9006, 11, 'e2e-getall-connection-config-06', NOW(), 'e2e-client', 'e2e-secret', NULL, 1, 1773070766921, 'test-main');
