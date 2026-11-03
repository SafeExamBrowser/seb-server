/*
 * Copyright (c) 2026 ETH Zürich, IT Services
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.gbl.util;

import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;

/** An InputStream decorator that fails with a {@link SizeLimitExceededException} as soon as more than a given
 * number of bytes has been read from the underlying stream.
 * <p>
 * Unlike a silently truncating stream this makes oversized uploads and decompression bombs fail fast and with a
 * clear error instead of producing truncated data. */
public class SizeLimitedInputStream extends FilterInputStream {

    private final long maxBytes;
    private long count = 0;

    /** @param in the underlying input stream
     * @param maxBytes the maximum number of bytes that may be read from the underlying stream */
    public SizeLimitedInputStream(final InputStream in, final long maxBytes) {
        super(in);
        if (maxBytes < 0) {
            throw new IllegalArgumentException("maxBytes must not be negative");
        }
        this.maxBytes = maxBytes;
    }

    @Override
    public int read() throws IOException {
        final int b = super.read();
        if (b != -1) {
            count(1);
        }
        return b;
    }

    @Override
    public int read(final byte[] b, final int off, final int len) throws IOException {
        final int n = super.read(b, off, len);
        if (n > 0) {
            count(n);
        }
        return n;
    }

    @Override
    public long skip(final long n) throws IOException {
        final long skipped = super.skip(n);
        if (skipped > 0) {
            count(skipped);
        }
        return skipped;
    }

    @Override
    public boolean markSupported() {
        return false;
    }

    @Override
    public synchronized void mark(final int readlimit) {
        // mark/reset is not supported since it would bypass the byte count
    }

    @Override
    public synchronized void reset() throws IOException {
        throw new IOException("mark/reset not supported");
    }

    private void count(final long n) throws SizeLimitExceededException {
        this.count += n;
        if (this.count > this.maxBytes) {
            throw new SizeLimitExceededException(this.maxBytes);
        }
    }

    /** Signals that the size limit of a {@link SizeLimitedInputStream} has been exceeded. */
    public static final class SizeLimitExceededException extends IOException {

        private static final long serialVersionUID = 4637528395713420183L;

        public SizeLimitExceededException(final long maxBytes) {
            super("Input exceeds the maximum allowed size of " + maxBytes + " bytes");
        }
    }
}
