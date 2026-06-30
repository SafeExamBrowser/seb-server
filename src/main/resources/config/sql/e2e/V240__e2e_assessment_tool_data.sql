-- Assessment Tool (lms_setup) READ Get-All e2e fixtures.
-- Self-contained: all rows belong to the shared base institution 11 ('sebserv').
-- A distinct 'e2e-getall-assessment-tool' name token keeps these independent of
-- any other seeded LMS setups.

-- `integration_active` (added by V28) is INT(1) NOT NULL with no default, so it
-- must be set explicitly; 0 = LMS integration inactive, which is what these
-- read-only MOCKUP fixtures need.
INSERT INTO lms_setup (id, institution_id, name, lms_type, active, integration_active)
VALUES
    -- active + inactive pair (status filter) sharing the search token
    (9101, 11, 'e2e-getall-assessment-tool-01', 'MOCKUP', 1, 0),
    (9102, 11, 'e2e-getall-assessment-tool-02', 'MOCKUP', 0, 0),
    -- extra matching rows so a 2nd page exists at page_size=5
    (9103, 11, 'e2e-getall-assessment-tool-03', 'MOCKUP', 1, 0),
    (9104, 11, 'e2e-getall-assessment-tool-04', 'MOCKUP', 1, 0),
    (9105, 11, 'e2e-getall-assessment-tool-05', 'MOCKUP', 1, 0),
    (9106, 11, 'e2e-getall-assessment-tool-06', 'MOCKUP', 1, 0);
