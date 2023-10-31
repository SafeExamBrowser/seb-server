/*
 * Copyright (c) 2023 ETH Zürich, Educational Development and Technology (LET)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.webservice.servicelayer.session;

public interface SEBClientPingService {

    static enum PingServiceType {
        BLOCKING,
        BATCH
    }

    PingServiceType pingServiceType();

    String notifyPing(String connectionToken, String instructionConfirm);

}
