INSERT IGNORE INTO configuration_attribute VALUES
    (318, 'sebServiceIgnore', 'CHECKBOX', null, null, null, null, 'true'),
    (319, 'allowApplicationLog', 'CHECKBOX', null, null, null, null, 'false'),
    (320, 'showApplicationLogButton', 'CHECKBOX', null, null, null, null, 'false'),
    (321, 'enableWindowsUpdate', 'CHECKBOX', null, null, null, null, 'false'),
    (322, 'enableChromeNotifications', 'CHECKBOX', null, null, null, null, 'false')
    ;


UPDATE orientation SET y_position='13', width='12' WHERE id='305';
UPDATE orientation SET y_position='16', width='10' WHERE id='306';
UPDATE orientation SET y_position='17', width='10' WHERE id='307';
UPDATE orientation SET y_position='18' WHERE id='317';
UPDATE orientation SET x_position='0', y_position='9' WHERE id='301';
UPDATE orientation SET x_position='3', y_position='10', width='4' WHERE id='501';
UPDATE orientation SET x_position='3', y_position='11', width='4' WHERE id='304';
UPDATE orientation SET x_position='3', y_position='12', width='4' WHERE id='302';
UPDATE orientation SET group_id='sebService', x_position='4', y_position='6', width='3' WHERE id='303';
UPDATE orientation SET group_id='sebService', y_position='2', width='7', title='TOP' WHERE id='300';
UPDATE orientation SET y_position='3' WHERE id='309';
UPDATE orientation SET y_position='4' WHERE id='310';
UPDATE orientation SET y_position='5' WHERE id='311';
UPDATE orientation SET y_position='6' WHERE id='312';
UPDATE orientation SET y_position='7' WHERE id='313';
UPDATE orientation SET y_position='8' WHERE id='314';
UPDATE orientation SET y_position='11' WHERE id='315';
UPDATE orientation SET y_position='12' WHERE id='316';


INSERT IGNORE INTO orientation VALUES
    (318, 318, 0, 9, 'sebService', 0, 0, 7, 1, 'NONE'),
    (319, 319, 0, 9, 'logging', 0, 14, 5, 1, 'NONE'),
    (320, 320, 0, 9, 'logging', 0, 15, 5, 1, 'NONE'),
    (321, 321, 0, 9, 'sebService', 0, 6, 4, 1, 'NONE'),
    (322, 322, 0, 9, 'sebService', 0, 7, 4, 1, 'NONE')
    ;