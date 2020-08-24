INSERT INTO view (name, columns, position, template_id) VALUES 
    ('proctoring', 12, 12, 0);
    
SET @proct_view_id = LAST_INSERT_ID();


INSERT IGNORE INTO configuration_attribute VALUES
    (1100, 'jitsiMeetAudioMuted', 'CHECKBOX', null, null, null, null, 'true'),
    (1101, 'jitsiMeetAudioOnly', 'CHECKBOX', null, null, null, null, 'false'),
    (1102, 'jitsiMeetEnable', 'CHECKBOX', null, null, null, null, 'true'),
    (1103, 'jitsiMeetFeatureFlagChat', 'CHECKBOX', null, null, null, null, 'false'),
    (1104, 'jitsiMeetFeatureFlagCloseCaptions', 'CHECKBOX', null, null, null, null, 'false'),
    (1105, 'jitsiMeetFeatureFlagDisplayMeetingName', 'CHECKBOX', null, null, null, null, 'false'),
    (1106, 'jitsiMeetFeatureFlagRaiseHand', 'CHECKBOX', null, null, null, null, 'false'),
    (1107, 'jitsiMeetFeatureFlagRecording', 'CHECKBOX', null, null, null, null, 'false'),
    (1108, 'jitsiMeetFeatureFlagTileView', 'CHECKBOX', null, null, null, null, 'false'),
    (1109, 'jitsiMeetRoom', 'TEXT_FIELD', null, null, null, null, ''),
    (1110, 'jitsiMeetServerURL', 'TEXT_FIELD', null, null, null, null, ''),
    (1111, 'jitsiMeetSubject', 'TEXT_FIELD', null, null, null, null, ''),
    (1112, 'jitsiMeetToken', 'TEXT_FIELD', null, null, null, null, ''),
    (1113, 'jitsiMeetUserInfoAvatarURL', 'TEXT_FIELD', null, null, null, null, ''),
    (1114, 'jitsiMeetUserInfoDisplayName', 'TEXT_FIELD', null, null, null, null, ''),
    (1115, 'jitsiMeetUserInfoEMail', 'TEXT_FIELD', null, null, null, null, ''),
    (1116, 'jitsiMeetVideoMuted', 'CHECKBOX', null, null, null, null, 'false'),
    
    (1120, 'proctoringAIEnable', 'CHECKBOX', null, null, null, null, 'false'),
    (1121, 'proctoringDetectFaceAngleDisplay', 'CHECKBOX', null, null, null, null, 'true'),
    (1122, 'proctoringDetectFaceCount', 'CHECKBOX', null, null, null, null, 'true'),
    (1123, 'proctoringDetectFaceCountDisplay', 'CHECKBOX', null, null, null, null, 'true'),
    (1124, 'proctoringDetectFacePitch', 'CHECKBOX', null, null, null, null, 'true'),
    (1125, 'proctoringDetectFaceYaw', 'CHECKBOX', null, null, null, null, 'true'),
    (1126, 'proctoringDetectTalking', 'CHECKBOX', null, null, null, null, 'true'),
    (1127, 'proctoringDetectTalkingDisplay', 'CHECKBOX', null, null, null, null, 'true'),
    (1128, 'showProctoringViewButton', 'CHECKBOX', null, null, null, null, 'true'),
    (1129, 'remoteProctoringViewShow', 'SINGLE_SELECTION', null, '0,1,2,3', null, null, '0'),
    
    (1130, 'jitsiMeetReceiveAudio', 'CHECKBOX', null, null, null, null, 'false'),
    (1131, 'jitsiMeetReceiveVideo', 'CHECKBOX', null, null, null, null, 'false'),
    (1132, 'jitsiMeetSendAudio', 'CHECKBOX', null, null, null, null, 'true'),
    (1133, 'jitsiMeetSendVideo', 'CHECKBOX', null, null, null, null, 'true')
    ;
    
INSERT INTO orientation (config_attribute_id, template_id, view_id, group_id, x_position, y_position, width, height, title) VALUES
    (1129, 0,  @proct_view_id, null, 3, 1, 3, 1, 'LEFT_SPAN'),
    (1130, 0,  @proct_view_id, null, 0, 2, 6, 1, 'NONE'),
    (1131, 0,  @proct_view_id, null, 0, 3, 6, 1, 'NONE'),
    (1132, 0,  @proct_view_id, null, 0, 4, 6, 1, 'NONE'),
    (1133, 0,  @proct_view_id, null, 0, 5, 6, 1, 'NONE'),
    
    (1102, 0,  @proct_view_id, null, 6, 1, 6, 1, 'NONE'),
    
    (1103, 0,  @proct_view_id, 'jitsi_features', 6, 2, 6, 1, 'NONE'),
    (1106, 0,  @proct_view_id, 'jitsi_features', 6, 3, 6, 1, 'NONE'),
    (1108, 0,  @proct_view_id, 'jitsi_features', 6, 4, 6, 1, 'NONE'),
    (1104, 0,  @proct_view_id, 'jitsi_features', 6, 5, 6, 1, 'NONE'),
    (1105, 0,  @proct_view_id, 'jitsi_features', 6, 6, 6, 1, 'NONE'),
    
    (1100, 0,  @proct_view_id, 'jitsi_controls', 6, 8, 6, 1, 'NONE'),
    (1116, 0,  @proct_view_id, 'jitsi_controls', 6, 9, 6, 1, 'NONE'),
    (1101, 0,  @proct_view_id, 'jitsi_controls', 6, 10, 6, 1, 'NONE'),
    
    (1120, 0,  @proct_view_id, null, 0, 7, 6, 1, 'NONE'),
    
    (1122, 0,  @proct_view_id, 'ai_faces_number', 0, 8, 6, 1, 'NONE'),
    (1123, 0,  @proct_view_id, 'ai_faces_number', 0, 9, 6, 1, 'NONE'),
    
    (1125, 0,  @proct_view_id, 'ai_faces_angle', 0, 11, 6, 1, 'NONE'),
    (1124, 0,  @proct_view_id, 'ai_faces_angle', 0, 12, 6, 1, 'NONE'),
    (1121, 0,  @proct_view_id, 'ai_faces_angle', 0, 13, 6, 1, 'NONE')
    ;
    
    
    