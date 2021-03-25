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
        @JsonProperty final int type = 2; // Scheduled Meeting
        @JsonProperty final String start_time = DateTime.now(DateTimeZone.UTC).toString("yyyy-MM-dd'T'HH:mm:ss");
        @JsonProperty final int duration = 60;
        @JsonProperty final CharSequence password;
        @JsonProperty final Settings settings;

        public CreateMeetingRequest(final String topic, final CharSequence password) {
            this.topic = topic;
            this.password = password;
            this.settings = new Settings();
        }

        @JsonIgnoreProperties(ignoreUnknown = true)
        static class Settings {
            @JsonProperty final boolean host_video = false;
            @JsonProperty final boolean participant_video = true;
            @JsonProperty final boolean join_before_host = true;
            @JsonProperty final int jbh_time = 0;
            @JsonProperty final boolean use_pmi = false;
            @JsonProperty final String audio = "voip";
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
        final CharSequence encryptedPwd;

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
                @JsonProperty("encrypted_password") final CharSequence encryptedPwd) {

            this.id = id;
            this.join_url = join_url;
            this.start_url = start_url;
            this.start_time = start_time;
            this.duration = duration;
            this.status = status;
            this.uuid = uuid;
            this.host_id = host_id;
            this.meetingPwd = meetingPwd;
            this.encryptedPwd = encryptedPwd;
        }
    }

    // @formatter:on
}
