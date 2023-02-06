/*
 * Copyright (c) 2023 ETH Zürich, Educational Development and Technology (LET)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.gbl.model.exam;

import java.util.Objects;

import org.apache.commons.lang3.StringUtils;

import ch.ethz.seb.sebserver.gbl.Constants;

public class AllowedSEBVersion {

    public static final String OS_WINDOWS_IDENTIFIER = "Win";
    public static final String OS_MAC_IDENTIFIER = "Mac";
    public static final String OS_IOS_IDENTIFIER = "iOS";
    public static final String ALIANCE_EDITION_IDENTIFIER = "AE";
    public static final String MINIMAL_IDENTIFIER = "min";

    public final String wholeVersionString;
    public final String osTypeString;
    public final int major;
    public final int minor;
    public final int patch;
    public final boolean alianceEdition;
    public final boolean minimal;
    public final boolean isValidFormat;

    public AllowedSEBVersion(final String wholeVersionString) {
        this.wholeVersionString = wholeVersionString;

        boolean valid = true;
        final String[] split = StringUtils.split(wholeVersionString, Constants.DOT);
        if (OS_WINDOWS_IDENTIFIER.equalsIgnoreCase(split[0])) {
            this.osTypeString = OS_WINDOWS_IDENTIFIER;
        } else if (OS_MAC_IDENTIFIER.equalsIgnoreCase(split[0])) {
            this.osTypeString = OS_MAC_IDENTIFIER;
        } else if (OS_IOS_IDENTIFIER.equalsIgnoreCase(split[0])) {
            this.osTypeString = OS_IOS_IDENTIFIER;
        } else {
            this.osTypeString = null;
            valid = false;
        }

        int num = -1;
        try {
            num = Integer.valueOf(split[1]);
        } catch (final Exception e) {
            valid = false;
        }
        this.major = num;

        try {
            num = Integer.valueOf(split[2]);
        } catch (final Exception e) {
            valid = false;
        }
        this.minor = num;
        try {
            num = Integer.valueOf(split[3]);
        } catch (final Exception e) {
            valid = false;
        }
        this.patch = num;

        if (split.length > 4 && ALIANCE_EDITION_IDENTIFIER.equalsIgnoreCase(split[4])) {
            this.alianceEdition = true;
            if (split.length > 5 && MINIMAL_IDENTIFIER.equalsIgnoreCase(split[5])) {
                this.minimal = true;
            } else {
                this.minimal = false;
            }
        } else {
            this.alianceEdition = false;
            if (split.length > 4 && MINIMAL_IDENTIFIER.equalsIgnoreCase(split[4])) {
                this.minimal = true;
            } else {
                this.minimal = false;
            }
        }

        this.isValidFormat = valid;
    }

    public boolean match(final ClientVersion clientVersion) {
        if (Objects.equals(this.osTypeString, clientVersion.osTypeString)) {
            if (this.minimal) {
                // check greater or equals minimum version
                return this.major <= clientVersion.major ||
                        this.minor <= clientVersion.minor ||
                        this.patch <= clientVersion.patch;
            } else {
                // check exact match
                return this.major == clientVersion.major &&
                        this.minor == clientVersion.minor &&
                        this.patch == clientVersion.patch;
            }
        }
        return false;
    }

    public static final class ClientVersion {

        public String osTypeString;
        public final int major;
        public final int minor;
        public final int patch;

        public ClientVersion(final String osTypeString, final int major, final int minor, final int patch) {
            this.osTypeString = osTypeString;
            this.major = major;
            this.minor = minor;
            this.patch = patch;
        }
    }

}
