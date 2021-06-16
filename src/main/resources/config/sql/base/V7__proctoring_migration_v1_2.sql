UPDATE orientation SET group_id='[proctoring|jitsi]' WHERE config_attribute_id='1102';
UPDATE orientation SET group_id='jitsi_features[proctoring|jitsi]' WHERE config_attribute_id='1103';
UPDATE orientation SET group_id='jitsi_features[proctoring|jitsi]' WHERE config_attribute_id='1106';
UPDATE orientation SET group_id='jitsi_features[proctoring|jitsi]' WHERE config_attribute_id='1108';
UPDATE orientation SET group_id='jitsi_features[proctoring|jitsi]' WHERE config_attribute_id='1104';
UPDATE orientation SET group_id='jitsi_features[proctoring|jitsi]' WHERE config_attribute_id='1105';
UPDATE orientation SET group_id='jitsi_controls[proctoring|jitsi]' WHERE config_attribute_id='1100';
UPDATE orientation SET group_id='jitsi_controls[proctoring|jitsi]' WHERE config_attribute_id='1116';
UPDATE orientation SET group_id='jitsi_controls[proctoring|jitsi]' WHERE config_attribute_id='1101';
UPDATE orientation SET group_id='jitsi_audio_video[proctoring|jitsi]' WHERE config_attribute_id='1130';
UPDATE orientation SET group_id='jitsi_audio_video[proctoring|jitsi]' WHERE config_attribute_id='1131';
UPDATE orientation SET group_id='jitsi_audio_video[proctoring|jitsi]' WHERE config_attribute_id='1132';
UPDATE orientation SET group_id='jitsi_audio_video[proctoring|jitsi]' WHERE config_attribute_id='1133';

INSERT IGNORE INTO configuration_attribute VALUES
    (1500, 'zoomEnable', 'CHECKBOX', null, null, null, null, 'false'),
    (1501, 'zoomAudioOnly', 'CHECKBOX', null, null, null, null, 'false'),
    (1502, 'zoomAudioMuted', 'CHECKBOX', null, null, null, null, 'true'),
    (1503, 'zoomFeatureFlagChat', 'CHECKBOX', null, null, null, null, 'false'),
    (1504, 'zoomFeatureFlagCloseCaptions', 'CHECKBOX', null, null, null, null, 'false'),
    (1505, 'zoomFeatureFlagDisplayMeetingName', 'CHECKBOX', null, null, null, null, 'false'),
    (1506, 'zoomFeatureFlagRaiseHand', 'CHECKBOX', null, null, null, null, 'false'),
    (1507, 'zoomFeatureFlagRecording', 'CHECKBOX', null, null, null, null, 'false'),
    (1508, 'zoomFeatureFlagTileView', 'CHECKBOX', null, null, null, null, 'false'),
    (1509, 'zoomRoom', 'TEXT_FIELD', null, null, null, null, ''),
    (1510, 'zoomServerURL', 'TEXT_FIELD', null, null, null, null, ''),
    (1511, 'zoomSubject', 'TEXT_FIELD', null, null, null, null, ''),
    (1512, 'zoomToken', 'TEXT_FIELD', null, null, null, null, ''),
    (1513, 'zoomUserInfoAvatarURL', 'TEXT_FIELD', null, null, null, null, ''),
    (1514, 'zoomUserInfoDisplayName', 'TEXT_FIELD', null, null, null, null, ''),
    (1515, 'zoomUserInfoEMail', 'TEXT_FIELD', null, null, null, null, ''),
    (1516, 'zoomVideoMuted', 'CHECKBOX', null, null, null, null, 'false'),
    
    (1530, 'zoomReceiveAudio', 'CHECKBOX', null, null, null, null, 'false'),
    (1531, 'zoomReceiveVideo', 'CHECKBOX', null, null, null, null, 'false'),
    (1532, 'zoomSendAudio', 'CHECKBOX', null, null, null, null, 'true'),
    (1533, 'zoomSendVideo', 'CHECKBOX', null, null, null, null, 'true')
    ;

SET @proct_view_id = (SELECT id FROM view WHERE name='proctoring' AND template_id=0 LIMIT 1);

INSERT IGNORE INTO orientation (config_attribute_id, template_id, view_id, group_id, x_position, y_position, width, height, title) VALUES
    (1500, 0,  @proct_view_id, '[proctoring|Zoom]', 6, 1, 6, 1, 'NONE'),
    
    (1503, 0,  @proct_view_id, 'zoom_features[proctoring|Zoom]', 6, 7, 6, 1, 'NONE'),
    (1506, 0,  @proct_view_id, 'zoom_features[proctoring|Zoom]', 6, 8, 6, 1, 'NONE'),
    (1508, 0,  @proct_view_id, 'zoom_features[proctoring|Zoom]', 6, 9, 6, 1, 'NONE'),
    (1504, 0,  @proct_view_id, 'zoom_features[proctoring|Zoom]', 6, 10, 6, 1, 'NONE'),
    (1505, 0,  @proct_view_id, 'zoom_features[proctoring|Zoom]', 6, 11, 6, 1, 'NONE'),
    
    (1502, 0,  @proct_view_id, 'zoom_controls[proctoring|Zoom]', 6, 13, 6, 1, 'NONE'),
    (1516, 0,  @proct_view_id, 'zoom_controls[proctoring|Zoom]', 6, 14, 6, 1, 'NONE'),
    (1501, 0,  @proct_view_id, 'zoom_controls[proctoring|Zoom]', 6, 15, 6, 1, 'NONE'),
    
    (1530, 0,  @proct_view_id, 'zoom_audio_video[proctoring|Zoom]', 6, 2, 6, 1, 'NONE'),
    (1531, 0,  @proct_view_id, 'zoom_audio_video[proctoring|Zoom]', 6, 3, 6, 1, 'NONE'),
    (1532, 0,  @proct_view_id, 'zoom_audio_video[proctoring|Zoom]', 6, 4, 6, 1, 'NONE'),
    (1533, 0,  @proct_view_id, 'zoom_audio_video[proctoring|Zoom]', 6, 5, 6, 1, 'NONE')
    ;