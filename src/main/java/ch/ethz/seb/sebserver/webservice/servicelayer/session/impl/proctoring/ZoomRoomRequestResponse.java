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
            @JsonProperty final String lasr_name;
            public UserInfo(final String email, final int type, final String first_name, final String lasr_name) {
                this.email = email;
                this.type = type;
                this.first_name = first_name;
                this.lasr_name = lasr_name;
            }
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    static class CreateUserResponse {
        final String id;
        final String email;
        final int type;
        final String first_name;
        final String lasr_name;
        @JsonCreator
        public CreateUserResponse(
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
    static class NewRoomRequest {
        @JsonProperty final String topic;
        @JsonProperty final int type = 1; // Instant meeting
        @JsonProperty final String start_time = DateTime.now(DateTimeZone.UTC).toString("yyyy-MM-dd`T`HH:mm:ssZ");
        @JsonProperty final String password;
        @JsonProperty final Settings settings;

        public NewRoomRequest(final String topic, final String password, final Settings settings) {
            this.topic = topic;
            this.password = password;
            this.settings = settings;
        }

        @JsonIgnoreProperties(ignoreUnknown = true)
        static class Settings {
            final boolean host_video = false;
            final boolean participant_video = true;
            final boolean join_before_host = true;
            final int jbh_time = 0;
            final boolean use_pmi = false;
            final String audio = "voip";
        }
    }

    // https://marketplace.zoom.us/docs/api-reference/zoom-api/meetings/meetingcreate
    @JsonIgnoreProperties(ignoreUnknown = true)
    static class NewRoomResponse {
        final Integer id;
        final String join_url;
        final String start_url;
        final String start_time;
        final Integer duration;
        final String status;
        final String uuid;
        final String host_id;

        @JsonCreator
        public NewRoomResponse(
                @JsonProperty("id") final Integer id,
                @JsonProperty("join_url") final String join_url,
                @JsonProperty("start_url") final String start_url,
                @JsonProperty("start_time") final String start_time,
                @JsonProperty("duration") final Integer duration,
                @JsonProperty("status") final String status,
                @JsonProperty("uuid") final String uuid,
                @JsonProperty("host_id") final String host_id) {
            this.id = id;
            this.join_url = join_url;
            this.start_url = start_url;
            this.start_time = start_time;
            this.duration = duration;
            this.status = status;
            this.uuid = uuid;
            this.host_id = host_id;
        }
    }

    // @formatter:on
}
