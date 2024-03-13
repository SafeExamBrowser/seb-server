/*
 * Copyright (c) 2020 ETH Zürich, IT Services
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.gbl.util;

import java.util.concurrent.ArrayBlockingQueue;

public class SizedArrayNonBlockingQueue<T> extends ArrayBlockingQueue<T> {

    private static final long serialVersionUID = -4235702373708064610L;

    private final int size;

    public SizedArrayNonBlockingQueue(final int size) {
        super(size);
        this.size = size;
    }

    @Override
    synchronized public boolean add(final T element) {
        // Check if queue full already?
        if (super.size() == this.size) {
            // remove element from queue if queue is full
            this.remove();
        }
        return super.add(element);
    }

}
