INSERT IGNORE INTO institution VALUES
    (1, 'ETH Zürich', 'ethz', null, null, 1)
    ;

INSERT IGNORE INTO user VALUES 
    (1, 1, 'super-admin', '2019-01-01', 'super-admin', '', 'super-admin', '$2a$08$c2GKYEYoUVXH1Yb8GXVXVu66ltPvbZgLMcVSXRH.LgZNF/YeaYB8m', 'super-admin@nomail.nomail', 'en', 'UTC', 1)
    ;
    
INSERT IGNORE INTO user_role VALUES
    (1, 1, 'SEB_SERVER_ADMIN'),
    (2, 1, 'INSTITUTIONAL_ADMIN'),
    (3, 1, 'EXAM_ADMIN'),
    (4, 1, 'EXAM_SUPPORTER')
    ;
    
INSERT IGNORE INTO seb_client_configuration VALUES
    (1, 1, 'test', '2019-07-02 09:22:50', 'test', '20faa63098beb54053d228d078d9471a6b35bde8cf2e91a879789e721e54c6535660d8d69e4670484a87977a', null, 1)
    ;
    
INSERT IGNORE INTO lms_setup VALUES
    (1, 1, 'test', 'MOCKUP', 'http://', 'test-user', 'test-user', null, null, null, null, null, 1),
    (2,1,'Open edX Ralph','OPEN_EDX','http://edx.devstack.lms:18000','2c3584ef258d1fdd98de','39d0bc82eda66a84a18d54c1f084596bc3c9435f0dbd1170b85fce706264dbb6894694c524be85ebf65d3eaa66f20856b4aac7ec355d74a363c7129dacfaa3366dbe9a98de5fd4959bb5d70621c9b728',NULL,NULL,NULL,NULL,NULL,1),
    (3,1,'Moodle','MOODLE','https://safeexambrowser.moodle.ch/lms','sebserver','7465728b701f34bbf3ed29a48f7961686bc69e2845416bddb1ac8fb14977aef41b9dc5a6c2bca18be38ba2b81c964f6abb3909054ba5f3f9e09d75ef',NULL,NULL,NULL,NULL,NULL,1)
    ;
    
    
INSERT IGNORE INTO seb_client_configuration VALUES
    (1, 1, 'test', '2019-07-02 09:22:50', 'test', '98ac3c953abf5948d9d13c81cab580819ee2624c76d6d4147d4896a5b79f49956d382c08c93cb3b9ae350b32', null, 1)
    ;
    
INSERT IGNORE INTO exam VALUES
    (1, 1, 1, 'quiz1', 'super-admin', 'super-admin', 'MANAGED', null, null, 'UP_COMING', 0, 0, null, 1),
    (2, 1, 1, 'quiz6', 'super-admin', 'super-admin', 'MANAGED', null, null, 'RUNNING', 0, 0, null, 1)
    ;
    
INSERT IGNORE INTO indicator VALUES
    (1, 2, 'LAST_PING', 'Ping', 'dcdcdc')
    ;
    
INSERT IGNORE INTO threshold VALUES
    (1, 1, 0.0000, '22b14c'),
    (2, 1, 2000.0000, 'ff7e00'),
    (3, 1, 5000.0000, 'ed1c24')
    ;