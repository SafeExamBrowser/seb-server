-- Assessment Tool (lms_setup) READ Get-All e2e fixtures.
-- Self-contained: all rows belong to the shared base institution 11 ('sebserv').
-- A distinct 'e2e-getall-assessment-tool' name token keeps these independent of
-- any other seeded LMS setups.

INSERT INTO lms_setup (id, institution_id, name, lms_type, active)
VALUES
    -- active + inactive pair (status filter) sharing the search token
    (9101, 11, 'e2e-getall-assessment-tool-01', 'MOCKUP', 1),
    (9102, 11, 'e2e-getall-assessment-tool-02', 'MOCKUP', 0),
    -- extra matching rows so a 2nd page exists at page_size=5
    (9103, 11, 'e2e-getall-assessment-tool-03', 'MOCKUP', 1),
    (9104, 11, 'e2e-getall-assessment-tool-04', 'MOCKUP', 1),
    (9105, 11, 'e2e-getall-assessment-tool-05', 'MOCKUP', 1),
    (9106, 11, 'e2e-getall-assessment-tool-06', 'MOCKUP', 1);
