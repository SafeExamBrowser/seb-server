/*
 * Copyright (c) 2026 ETH Zürich, IT Services
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.gbl.util;

import static org.junit.Assert.*;

import java.io.ByteArrayInputStream;
import java.io.InputStream;

import org.junit.Test;

import ch.ethz.seb.sebserver.gbl.util.SizeLimitedInputStream.SizeLimitExceededException;

public class SizeLimitedInputStreamTest {

    @Test
    public void readsUpToTheLimit() throws Exception {
        try (InputStream in = new SizeLimitedInputStream(new ByteArrayInputStream(new byte[100]), 100)) {
            assertEquals(100, in.readAllBytes().length);
        }
    }

    @Test(expected = SizeLimitExceededException.class)
    public void failsAboveTheLimit() throws Exception {
        try (InputStream in = new SizeLimitedInputStream(new ByteArrayInputStream(new byte[101]), 100)) {
            in.readAllBytes();
        }
    }

    @Test(expected = SizeLimitExceededException.class)
    public void singleByteReadsAreCounted() throws Exception {
        try (InputStream in = new SizeLimitedInputStream(new ByteArrayInputStream(new byte[2]), 1)) {
            assertEquals(0, in.read());
            in.read();
        }
    }

    @Test(expected = SizeLimitExceededException.class)
    public void skippedBytesAreCounted() throws Exception {
        try (InputStream in = new SizeLimitedInputStream(new ByteArrayInputStream(new byte[10]), 5)) {
            in.skip(6);
        }
    }

    @Test
    public void markIsNotSupported() throws Exception {
        try (InputStream in = new SizeLimitedInputStream(new ByteArrayInputStream(new byte[10]), 5)) {
            assertFalse(in.markSupported());
        }
    }
}
