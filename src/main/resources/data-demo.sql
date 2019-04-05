INSERT INTO institution VALUES
    (1, 'ETH Zürich', 'ethz', null, null, 1),
    (2, 'Institution 2', 'inst2', null, null, 1),
    (3, 'Institution 3', 'inst3', null, null, 0),
    (4, 'Institution 4', 'inst4', null, null, 0),
    (5, 'Institution 5', 'inst5', null, null, 0),
    (6, 'Institution 6', 'inst6', null, null, 0),
    (7, 'Institution 7', 'inst7', null, null, 0),
    (8, 'Institution 8', 'inst8', null, null, 0)
    ;

INSERT INTO user VALUES 
    (1, 1, 'super-admin', 'super-admin', 'super-admin', '$2a$08$c2GKYEYoUVXH1Yb8GXVXVu66ltPvbZgLMcVSXRH.LgZNF/YeaYB8m', 'super-admin@nomail.nomail', 'en', 'UTC', 1),
    (2, 1, 'internalDemoAdmin', 'Admin1', 'admin', '$2a$08$c2GKYEYoUVXH1Yb8GXVXVu66ltPvbZgLMcVSXRH.LgZNF/YeaYB8m', 'admin@nomail.nomail', 'en', 'UTC', 1),
    (3, 1, 'inst1Admin', 'Institutional1 Admin', 'inst1Admin', '$2a$08$c2GKYEYoUVXH1Yb8GXVXVu66ltPvbZgLMcVSXRH.LgZNF/YeaYB8m', 'admin@nomail.nomail', 'en', 'UTC', 1),
    (4, 2, 'inst2Admin', 'Institutional2 Admin', 'inst2Admin', '$2a$08$c2GKYEYoUVXH1Yb8GXVXVu66ltPvbZgLMcVSXRH.LgZNF/YeaYB8m', 'admin@nomail.nomail', 'en', 'UTC', 1),
    (5, 2, 'examAdminInst2', 'Exam Admin 2', 'examAdmin2', '$2a$08$c2GKYEYoUVXH1Yb8GXVXVu66ltPvbZgLMcVSXRH.LgZNF/YeaYB8m', 'admin@nomail.nomail', 'en', 'UTC', 1)
    ;
    
INSERT INTO user_role VALUES
    (1, 1, 'SEB_SERVER_ADMIN'),
    (2, 1, 'INSTITUTIONAL_ADMIN'),
    (3, 1, 'EXAM_ADMIN'),
    (4, 1, 'EXAM_SUPPORTER'),
    (5, 2, 'SEB_SERVER_ADMIN'),
    (6, 3, 'INSTITUTIONAL_ADMIN'),
    (7, 4, 'INSTITUTIONAL_ADMIN'),
    (8, 5, 'EXAM_ADMIN')
    ;


