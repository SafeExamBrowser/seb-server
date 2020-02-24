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

import ch.ethz.seb.sebserver.gbl.util.Utils;

/** Data object implementation to generate the SEB Server API discovery JSON. */
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

    public String getTitle() {
        return this.title;
    }

    public String getDescription() {
        return this.description;
    }

    public String getServerLocation() {
        return this.serverLocation;
    }

    public Collection<ExamAPIVersion> getVersions() {
        return this.versions;
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
            this.endpoints = Utils.immutableCollectionOf(endpoints);
        }

        public ExamAPIVersion(
                final String name,
                final Endpoint... endpoints) {

            this.name = name;
            this.endpoints = (endpoints != null)
                    ? Utils.immutableCollectionOf(Arrays.asList(endpoints))
                    : Collections.emptyList();
        }

        public String getName() {
            return this.name;
        }

        public Collection<Endpoint> getEndpoints() {
            return this.endpoints;
        }

    }

    public static final class Endpoint {

        @JsonProperty("name")
        public final String name;

        @JsonProperty("description")
        public final String description;

        @JsonProperty("location")
        public final String location;

        @JsonProperty("authorization")
        public final String authorization;

        @JsonCreator
        public Endpoint(
                @JsonProperty("name") final String name,
                @JsonProperty("description") final String description,
                @JsonProperty("location") final String location,
                @JsonProperty("authorization") final String authorization) {

            this.name = name;
            this.description = description;
            this.location = location;
            this.authorization = authorization;
        }

        public String getName() {
            return this.name;
        }

        public String getDescription() {
            return this.description;
        }

        public String getLocation() {
            return this.location;
        }

        public String getAuthorization() {
            return this.authorization;
        }
    }
}
