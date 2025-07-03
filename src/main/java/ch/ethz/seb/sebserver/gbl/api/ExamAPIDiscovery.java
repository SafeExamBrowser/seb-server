/*
 * Copyright (c) 2019 ETH Zürich, IT Services
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

/** Data object implementation to generate the SEB Server API discovery JSON. */
public record ExamAPIDiscovery (
        @JsonProperty("title") String title,
        @JsonProperty("description") String description,
        @JsonProperty("server-location") String serverLocation,
        @JsonProperty("api-versions") Collection<ExamAPIVersion> versions) {
    

    @JsonCreator
    public ExamAPIDiscovery {}

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

    public record ExamAPIVersion (
            @JsonProperty("name")  String name,
            @JsonProperty("endpoints")  Collection<Endpoint> endpoints) {

        @JsonCreator
        public ExamAPIVersion {}
    }

    public record Endpoint(
            @JsonProperty("name") String name, 
            @JsonProperty("description") String description,
            @JsonProperty("location") String location,
            @JsonProperty("authorization") String authorization) {

            @JsonCreator
            public Endpoint {}
    
            @Override
            public String name() {
                return this.name;
            }
    
            @Override
            public String description() {
                return this.description;
            }
    
            @Override
            public String location() {
                return this.location;
            }
    
            @Override
            public String authorization() {
                return this.authorization;
            }
        }
}
