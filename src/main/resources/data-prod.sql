INSERT IGNORE INTO institution VALUES
    (1, 'SEB Server [ROOT]', null, null, null, 1)
    ;

INSERT IGNORE INTO user VALUES 
    (1, 1, 'super-admin', 'super-admin', 'super-admin', '$2a$08$c2GKYEYoUVXH1Yb8GXVXVu66ltPvbZgLMcVSXRH.LgZNF/YeaYB8m', 'super-admin@nomail.nomail', 'en', 'UTC', 1)
    ;
    
INSERT IGNORE INTO user_role VALUES
    (1, 1, 'SEB_SERVER_ADMIN'),
    (2, 1, 'INSTITUTIONAL_ADMIN'),
    (3, 1, 'EXAM_ADMIN'),
    (4, 1, 'EXAM_SUPPORTER')
    ;