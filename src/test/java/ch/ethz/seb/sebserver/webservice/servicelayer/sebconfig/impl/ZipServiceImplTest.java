/*
 * Copyright (c) 2026 ETH Zürich, IT Services
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.webservice.servicelayer.sebconfig.impl;

import static org.junit.Assert.*;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.zip.GZIPOutputStream;

import org.junit.Test;

public class ZipServiceImplTest {

    @Test
    public void readUnzipsDataWithinTheLimit() throws Exception {
        final byte[] plain = "<plist version=\"1.0\"><dict/></plist>".getBytes(StandardCharsets.UTF_8);
        final ByteArrayOutputStream out = new ByteArrayOutputStream();
        new ZipServiceImpl(1024).read(out, new ByteArrayInputStream(gzip(plain)));
        assertArrayEquals(plain, out.toByteArray());
    }

    @Test
    public void readStopsAtTheLimit() throws Exception {
        // 1 MB of zeros compresses to about 1 KB: a small decompression bomb
        final byte[] plain = new byte[1024 * 1024];
        final ByteArrayOutputStream out = new ByteArrayOutputStream();
        new ZipServiceImpl(64 * 1024).read(out, new ByteArrayInputStream(gzip(plain)));
        assertTrue("unzipped " + out.size() + " bytes", out.size() <= 64 * 1024);
    }

    private static byte[] gzip(final byte[] data) throws IOException {
        final ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (GZIPOutputStream zip = new GZIPOutputStream(bytes)) {
            zip.write(data);
        }
        return bytes.toByteArray();
    }
}
