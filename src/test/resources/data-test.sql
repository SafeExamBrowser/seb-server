INSERT INTO institution VALUES
    (1, 'Institution1', 'inst1', null, 1),
    (2, 'Institution2', 'inst2', null, 1)
    ;

INSERT INTO user VALUES 
    (1, 1, 'user1', 'SEBAdmin', 'admin', '$2a$08$c2GKYEYoUVXH1Yb8GXVXVu66ltPvbZgLMcVSXRH.LgZNF/YeaYB8m', 'admin@nomail.nomail', 'en', 'UTC', 1),
    (2, 1, 'user2', 'Institutional1 Admin', 'inst1Admin', '$2a$08$c2GKYEYoUVXH1Yb8GXVXVu66ltPvbZgLMcVSXRH.LgZNF/YeaYB8m', 'admin@nomail.nomail', 'en', 'UTC', 1),
    (5, 1, 'user5', 'Exam Supporter', 'examSupporter', '', 'admin@nomail.nomail', 'en', 'UTC', 1),
    (3, 2, 'user3', 'Institutional2 Admin', 'inst2Admin', '$2a$08$c2GKYEYoUVXH1Yb8GXVXVu66ltPvbZgLMcVSXRH.LgZNF/YeaYB8m', 'admin@nomail.nomail', 'en', 'UTC', 1),
    (4, 2, 'user4', 'ExamAdmin1', 'examAdmin1', '$2a$08$c2GKYEYoUVXH1Yb8GXVXVu66ltPvbZgLMcVSXRH.LgZNF/YeaYB8m', 'admin@nomail.nomail', 'en', 'UTC', 1),
    (6, 2, 'user6', 'Deactivated', 'deactivatedUser', '$2a$08$YICtiLuceLMeY4EE3TyMGuBNt6SXmAE9HZKilzS9RP00nn4OhexBm', 'user@nomail.nomail', 'en', 'UTC', 0),
    (7, 2, 'user7', 'User', 'user1', '$2a$08$YICtiLuceLMeY4EE3TyMGuBNt6SXmAE9HZKilzS9RP00nn4OhexBm', 'user@nomail.nomail', 'en', 'UTC', 1)
    ;
    
INSERT INTO user_role VALUES
    (1, 1, 'SEB_SERVER_ADMIN'),
    (2, 2, 'INSTITUTIONAL_ADMIN'),
    (3, 3, 'INSTITUTIONAL_ADMIN'),
    (4, 4, 'EXAM_ADMIN'),
    (5, 5, 'EXAM_SUPPORTER'),
    (6, 6, 'EXAM_SUPPORTER'),
    (7, 7, 'EXAM_SUPPORTER')
    ;
    
INSERT INTO user_activity_log VALUES
    (1, 'user1', 1, 'MODIFY', 'INSTITUTION', '1', 'some message'),
    (2, 'user2', 2, 'CREATE', 'EXAM', '1', 'some message'),
    (3, 'user3', 3, 'CREATE', 'EXAM', '2', 'some message'),
    (4, 'user4', 4, 'CREATE', 'EXAM', '33', 'some message'),
    (5, 'user4', 5, 'MODIFY', 'EXAM', '33', 'some message')
    ;

