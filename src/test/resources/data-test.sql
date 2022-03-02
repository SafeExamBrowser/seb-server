INSERT INTO institution VALUES
    (1, 'Institution1', 'inst1', null, null, 1),
    (2, 'Institution2', 'inst2', 'AAA', null, 1),
    (3, 'Institution3', 'inst3', null, null, 0)
    ;

INSERT INTO user VALUES 
    (1, 1, 'user1', '2019-01-01', 'SEBAdmin', '', 'admin', '$2a$08$c2GKYEYoUVXH1Yb8GXVXVu66ltPvbZgLMcVSXRH.LgZNF/YeaYB8m', 'admin1@nomail.nomail', 'en', 'UTC', 1),
    (2, 1, 'user2', '2019-01-01', 'Institutional1 Admin', '', 'inst1Admin', '$2a$08$c2GKYEYoUVXH1Yb8GXVXVu66ltPvbZgLMcVSXRH.LgZNF/YeaYB8m', 'admin2@nomail.nomail', 'en', 'UTC', 1),
    (5, 1, 'user5', '2019-01-01', 'Exam Supporter', '', 'examSupporter', '', 'admin3@nomail.nomail', 'en', 'UTC', 1),
    (3, 2, 'user3', '2019-01-01', 'Institutional2 Admin', '', 'inst2Admin', '$2a$08$c2GKYEYoUVXH1Yb8GXVXVu66ltPvbZgLMcVSXRH.LgZNF/YeaYB8m', 'admin4@nomail.nomail', 'en', 'UTC', 1),
    (4, 2, 'user4', '2019-01-01', 'ExamAdmin1', '', 'examAdmin1', '$2a$08$c2GKYEYoUVXH1Yb8GXVXVu66ltPvbZgLMcVSXRH.LgZNF/YeaYB8m', 'admin5@nomail.nomail', 'en', 'UTC', 1),
    (6, 2, 'user6', '2019-01-01', 'Deactivated', '', 'deactivatedUser', '$2a$08$YICtiLuceLMeY4EE3TyMGuBNt6SXmAE9HZKilzS9RP00nn4OhexBm', 'user1@nomail.nomail', 'en', 'UTC', 0),
    (7, 2, 'user7', '2019-01-01', 'User', '', 'user1', '$2a$08$YICtiLuceLMeY4EE3TyMGuBNt6SXmAE9HZKilzS9RP00nn4OhexBm', 'user2@nomail.nomail', 'en', 'UTC', 1)
    ;
    
INSERT INTO user_role VALUES
    (1, 1, 'SEB_SERVER_ADMIN'),
    (2, 1, 'INSTITUTIONAL_ADMIN'),
    (3, 1, 'EXAM_ADMIN'),
    (4, 1, 'EXAM_SUPPORTER'),
    
    (5, 2, 'INSTITUTIONAL_ADMIN'),
    (6, 3, 'INSTITUTIONAL_ADMIN'),
    (7, 4, 'EXAM_ADMIN'),
    (8, 5, 'EXAM_SUPPORTER'),
    (9, 6, 'EXAM_SUPPORTER'),
    (10, 7, 'EXAM_SUPPORTER')
    ;
    
INSERT INTO user_activity_log VALUES
    (1, 'user1', 1000, 'MODIFY', 'INSTITUTION', '1', 'some message'),
    (2, 'user2', 2000, 'CREATE', 'EXAM', '1', 'some message'),
    (3, 'user3', 3000, 'CREATE', 'EXAM', '2', 'some message'),
    (4, 'user4', 4000, 'CREATE', 'EXAM', '33', 'some message'),
    (5, 'user4', 5000, 'MODIFY', 'EXAM', '33', 'some message')
    ;

