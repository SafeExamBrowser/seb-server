/*
 * Copyright (c) 2021 ETH Zürich, Educational Development and Technology (LET)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.webservice.servicelayer.session.impl.proctoring;

import java.util.Collection;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

public interface ZoomRoomRequestResponse {
    // @formatter:off

    //https://marketplace.zoom.us/docs/api-reference/zoom-api/users/users
    @JsonIgnoreProperties(ignoreUnknown = true)
    static class UserPageResponse {
        final int page_count;
        final int page_number;
        final int page_size;
        final int total_records;
        final Collection<ZoomUser> users;

        @JsonCreator
        public UserPageResponse(
                @JsonProperty("page_count") final int page_count,
                @JsonProperty("page_number") final int page_number,
                @JsonProperty("page_size") final int page_size,
                @JsonProperty("total_records") final int total_records,
                @JsonProperty("users") final Collection<ZoomUser> users) {
            this.page_count = page_count;
            this.page_number = page_number;
            this.page_size = page_size;
            this.total_records = total_records;
            this.users = users;
        }

        @JsonIgnoreProperties(ignoreUnknown = true)
        static class ZoomUser {
            final String id;
            final String first_name;
            final String last_name;
            final String email;

            @JsonCreator
            public ZoomUser(
                    @JsonProperty("id") final String id,
                    @JsonProperty("first_name") final String first_name,
                    @JsonProperty("last_name") final String last_name,
                    @JsonProperty("email") final String email) {
                this.id = id;
                this.first_name = first_name;
                this.last_name = last_name;
                this.email = email;
            }
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    static class CreateUserRequest {
        @JsonProperty final String action;
        @JsonProperty final UserInfo user_info;
        public CreateUserRequest(final String action, final UserInfo user_info) {
            this.action = action;
            this.user_info = user_info;
        }

        static class UserInfo {
            @JsonProperty final String email;
            @JsonProperty final int type;
            @JsonProperty final String first_name;
            @JsonProperty final String last_name;
            public UserInfo(final String email, final int type, final String first_name, final String last_name) {
                this.email = email;
                this.type = type;
                this.first_name = first_name;
                this.last_name = last_name;
            }
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    static class ApplyUserSettingsRequest {
        @JsonProperty final InMeetingSettings in_meeting;

        // NOTE: This seems to need a special Zoom Account Plan to work
        //@JsonProperty final FeaturesSettings feature;

        public ApplyUserSettingsRequest() {
            this.in_meeting = new InMeetingSettings(true, 3, 3);
            //this.feature = new FeaturesSettings("Basic");
        }

        public ApplyUserSettingsRequest(
                final InMeetingSettings in_meeting,
                final FeaturesSettings feature) {
            this.in_meeting = in_meeting;
            //this.feature = feature;
        }

        static class InMeetingSettings {

            @JsonProperty final boolean auto_saving_chat;
            @JsonProperty final int allow_users_save_chats;
            @JsonProperty final int allow_participants_chat_with;

            public InMeetingSettings(
                    final boolean auto_saving_chat,
                    final int allow_users_save_chats,
                    final int allow_participants_chat_with) {

                this.auto_saving_chat = auto_saving_chat;
                this.allow_users_save_chats = allow_users_save_chats;
                this.allow_participants_chat_with = allow_participants_chat_with;
            }
        }

        static class FeaturesSettings {
            @JsonProperty final String concurrent_meeting;
            public FeaturesSettings(final String concurrent_meeting) {
                this.concurrent_meeting = concurrent_meeting;
            }
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    static class UserResponse {
        final String id;
        final String email;
        final int type;
        final String first_name;
        final String lasr_name;
        @JsonCreator
        public UserResponse(
                @JsonProperty("id") final String id,
                @JsonProperty("email") final String email,
                @JsonProperty("type") final int type,
                @JsonProperty("first_name") final String first_name,
                @JsonProperty("lasr_name") final String lasr_name) {
            this.id = id;
            this.email = email;
            this.type = type;
            this.first_name = first_name;
            this.lasr_name = lasr_name;
        }
    }

    // https://marketplace.zoom.us/docs/api-reference/zoom-api/meetings/meetingcreate
    @JsonIgnoreProperties(ignoreUnknown = true)
    static class CreateMeetingRequest {
        @JsonProperty final String topic;
        @JsonProperty final int type;
        @JsonProperty final String start_time;
        @JsonProperty final String timezone;
        @JsonProperty final int duration;
        @JsonProperty final CharSequence password;
        @JsonProperty final Settings settings;

        public CreateMeetingRequest(
                final String topic,
                final int duration,
                final CharSequence password,
                final boolean waitingRoom) {

            this.type = 2; // Scheduled Meeting
            this.start_time = DateTime.now(DateTimeZone.UTC).toString("yyyy-MM-dd'T'HH:mm:ss");
            this.duration = duration;
            this.timezone = DateTimeZone.UTC.getID();
            this.topic = topic;
            this.password = password;
            this.settings = new Settings(waitingRoom);
        }

        @JsonIgnoreProperties(ignoreUnknown = true)
        static class Settings {
            @JsonProperty final boolean host_video = false;
            @JsonProperty final boolean mute_upon_entry = false;
            @JsonProperty final boolean join_before_host;
            @JsonProperty final int jbh_time = 0;
            @JsonProperty final boolean use_pmi = false;
            @JsonProperty final String audio = "voip";
            @JsonProperty final boolean waiting_room;
            @JsonProperty final boolean allow_multiple_devices = false;

            public Settings(final boolean waitingRoom) {
                this.join_before_host = !waitingRoom;
                this.waiting_room = waitingRoom;
            }
        }
    }

    // https://marketplace.zoom.us/docs/api-reference/zoom-api/meetings/meetingcreate
    @JsonIgnoreProperties(ignoreUnknown = true)
    static class MeetingResponse {
        final Long id;
        final String join_url;
        final String start_url;
        final String start_time;
        final Integer duration;
        final String status;
        final String uuid;
        final String host_id;
        final CharSequence meetingPwd;
        final CharSequence encryptedMeetingPwd;

        @JsonCreator
        public MeetingResponse(
                @JsonProperty("id") final Long id,
                @JsonProperty("join_url") final String join_url,
                @JsonProperty("start_url") final String start_url,
                @JsonProperty("start_time") final String start_time,
                @JsonProperty("duration") final Integer duration,
                @JsonProperty("status") final String status,
                @JsonProperty("uuid") final String uuid,
                @JsonProperty("host_id") final String host_id,
                @JsonProperty("password") final CharSequence meetingPwd,
                @JsonProperty("encrypted_password") final CharSequence encryptedMeetingPwd) {

            this.id = id;
            this.join_url = join_url;
            this.start_url = start_url;
            this.start_time = start_time;
            this.duration = duration;
            this.status = status;
            this.uuid = uuid;
            this.host_id = host_id;
            this.meetingPwd = meetingPwd;
            this.encryptedMeetingPwd = encryptedMeetingPwd;
        }
    }

    // @formatter:on
}
