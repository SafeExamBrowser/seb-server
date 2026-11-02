/*
 * Copyright (c) 2020 ETH Zürich, IT Services
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.gbl.model.exam;

import java.util.*;

import org.apache.commons.lang3.ObjectUtils;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import ch.ethz.seb.sebserver.gbl.util.Utils;

@JsonIgnoreProperties(ignoreUnknown = true)
public final class Chapters {

    public static final String ATTR_CHAPTERS = "chapters";

    public final List<Chapter> chapter_list;

    @JsonCreator
    public Chapters(@JsonProperty(ATTR_CHAPTERS) final Collection<Chapter> chapters) {
        final List<Chapter> c = (chapters != null) ? new ArrayList<>(chapters) : new ArrayList<>();
        Collections.sort(c);
        this.chapter_list = Utils.immutableListOf(c);
    }

    public Collection<Chapter> getChapters() {
        return this.chapter_list;
    }

    @Override
    public String toString() {
        return "Chapters [chapters=" + this.chapter_list + "]";
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
        public record Chapter(
            @JsonProperty(ATTR_NAME) String name,
            @JsonProperty(ATTR_ID) String id) implements Comparable<Chapter> {
    
            public static final String ATTR_NAME = "name";
            public static final String ATTR_ID = "id";
    
            @JsonCreator
            public Chapter {}
    
            @Override
            public boolean equals(final Object o) {
                if (this == o) return true;
                if (o == null || getClass() != o.getClass()) return false;
                final Chapter chapter = (Chapter) o;
                return Objects.equals(id, chapter.id);
            }
    
            @Override
            public int hashCode() {
                return Objects.hashCode(id);
            }
    
            @Override
            public String toString() {
                return "Chapter [name=" + this.name + ", id=" + this.id + "]";
            }
    
            @Override
            public int compareTo(final Chapter o) {
                if (o == null) {
                    return -1;
                }
    
                return ObjectUtils.compare(this.name, o.name);
            }
        }

}
