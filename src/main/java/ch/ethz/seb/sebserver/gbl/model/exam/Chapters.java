/*
 * Copyright (c) 2020 ETH Zürich, Educational Development and Technology (LET)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.gbl.model.exam;

import java.util.Collection;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public final class Chapters {

    public static final String ATTR_CHAPTERS = "chapters";

    public final Collection<Chapter> chapters;

    @JsonCreator
    public Chapters(@JsonProperty(ATTR_CHAPTERS) final Collection<Chapter> chapters) {
        this.chapters = chapters;
    }

    public Collection<Chapter> getChapters() {
        return this.chapters;
    }

    @Override
    public String toString() {
        final StringBuilder builder = new StringBuilder();
        builder.append("Chapters [chapters=");
        builder.append(this.chapters);
        builder.append("]");
        return builder.toString();
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static final class Chapter {

        public static final String ATTR_NAME = "name";
        public static final String ATTR_ID = "id";

        public final String name;
        public final String id;

        @JsonCreator
        public Chapter(
                @JsonProperty(ATTR_NAME) final String name,
                @JsonProperty(ATTR_ID) final String id) {

            this.name = name;
            this.id = id;
        }

        public String getName() {
            return this.name;
        }

        public String getId() {
            return this.id;
        }

        @Override
        public String toString() {
            final StringBuilder builder = new StringBuilder();
            builder.append("Chapter [name=");
            builder.append(this.name);
            builder.append(", id=");
            builder.append(this.id);
            builder.append("]");
            return builder.toString();
        }

    }

}
