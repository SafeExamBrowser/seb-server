/*
 * Copyright (c) 2019 ETH Zürich, Educational Development and Technology (LET)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.gbl.model.session;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class PingResponse {

    public enum Type {
        SEB_INSTRUCTION,
        CONFIG_CHANGE
    }

    @JsonProperty("type")
    public final Type type;
    @JsonProperty("name")
    public final String name;
    @JsonProperty("param1")
    public final String param1;
    @JsonProperty("param2")
    public final String param2;
    @JsonProperty("param3")
    public final String param3;

    @JsonCreator
    protected PingResponse(
            @JsonProperty("type") final Type type,
            @JsonProperty("name") final String name,
            @JsonProperty("param1") final String param1,
            @JsonProperty("param2") final String param2,
            @JsonProperty("param3") final String param3) {

        this.type = type;
        this.name = name;
        this.param1 = param1;
        this.param2 = param2;
        this.param3 = param3;
    }

    public Type getType() {
        return this.type;
    }

    public String getName() {
        return this.name;
    }

    public String getParam1() {
        return this.param1;
    }

    public String getParam2() {
        return this.param2;
    }

    public String getParam3() {
        return this.param3;
    }

    @Override
    public String toString() {
        final StringBuilder builder = new StringBuilder();
        builder.append("PingResponse [type=");
        builder.append(this.type);
        builder.append(", name=");
        builder.append(this.name);
        builder.append(", param1=");
        builder.append(this.param1);
        builder.append(", param2=");
        builder.append(this.param2);
        builder.append(", param3=");
        builder.append(this.param3);
        builder.append("]");
        return builder.toString();
    }
}
