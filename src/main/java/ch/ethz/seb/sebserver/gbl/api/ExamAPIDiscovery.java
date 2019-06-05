/*
 * Copyright (c) 2019 ETH Zürich, Educational Development and Technology (LET)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.gbl.api;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public final class ExamAPIDiscovery {

    @JsonProperty("title")
    public final String title;

    @JsonProperty("description")
    public final String description;

    @JsonProperty("server-location")
    public final String serverLocation;

    @JsonProperty("api-versions")
    public final Collection<ExamAPIVersion> versions;

    @JsonCreator
    public ExamAPIDiscovery(
            @JsonProperty("title") final String title,
            @JsonProperty("description") final String description,
            @JsonProperty("server-location") final String serverLocation,
            @JsonProperty("api-versions") final Collection<ExamAPIVersion> versions) {

        this.title = title;
        this.description = description;
        this.serverLocation = serverLocation;
        this.versions = versions;
    }

    public ExamAPIDiscovery(
            final String title,
            final String description,
            final String serverLocation,
            final ExamAPIVersion... versions) {

        this(
                title,
                description,
                serverLocation,
                (versions != null) ? Arrays.asList(versions) : Collections.emptyList());
    }

    public static final class ExamAPIVersion {

        @JsonProperty("name")
        public final String name;

        @JsonProperty("endpoints")
        public final Collection<Endpoint> endpoints;

        @JsonCreator
        public ExamAPIVersion(
                @JsonProperty("name") final String name,
                @JsonProperty("endpoints") final Collection<Endpoint> endpoints) {

            this.name = name;
            this.endpoints = endpoints;
        }

        public ExamAPIVersion(
                final String name,
                final Endpoint... endpoints) {

            this.name = name;
            this.endpoints = (endpoints != null) ? Arrays.asList(endpoints) : Collections.emptyList();
        }
    }

    public static final class Endpoint {

        @JsonProperty("name")
        public final String name;

        @JsonProperty("descripiton")
        public final String descripiton;

        @JsonProperty("location")
        public final String location;

        @JsonProperty("authorization")
        public final String authorization;

        @JsonCreator
        public Endpoint(
                @JsonProperty("name") final String name,
                @JsonProperty("descripiton") final String descripiton,
                @JsonProperty("location") final String location,
                @JsonProperty("authorization") final String authorization) {

            this.name = name;
            this.descripiton = descripiton;
            this.location = location;
            this.authorization = authorization;
        }

    }
}
