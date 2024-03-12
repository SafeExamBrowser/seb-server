/*
 * Copyright (c) 2019 ETH Zürich, IT Services
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public final class TestUtils {

    private static final Object SOME_OBJECT = new Object();

    public static final <T> void genericObjectTest(final T testObject) {
        assertFalse(testObject.equals(null));
        assertFalse(testObject.equals(SOME_OBJECT));
        assertTrue(testObject.equals(testObject));
    }

    public static final <T> void genericObjectTest(final T testObject, final T other) {
        genericObjectTest(testObject);
        assertFalse(testObject.equals(other));
    }

}
