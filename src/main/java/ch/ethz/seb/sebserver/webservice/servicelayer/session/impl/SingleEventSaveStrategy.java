/*
 * Copyright (c) 2018 ETH Zürich, Educational Development and Technology (LET)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.webservice.servicelayer.session.impl;

import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import ch.ethz.seb.sebserver.gbl.profile.WebServiceProfile;
import ch.ethz.seb.sebserver.webservice.datalayer.batis.mapper.ClientEventRecordMapper;
import ch.ethz.seb.sebserver.webservice.datalayer.batis.model.ClientEventRecord;
import ch.ethz.seb.sebserver.webservice.servicelayer.session.EventHandlingStrategy;

/** Approach 1 to handle/save client events internally
 *
 * This saves one on one client event to persistence within separated transaction.
 * NOTE: if there are a lot of clients connected, firing events at small intervals like 100ms,
 * this is blocking to much because every event is saved within its own SQL commit and also
 * in its own transaction.
 *
 * An advantage of this approach is minimal data loss on server fail. **/
@Lazy
@Component(EventHandlingStrategy.EVENT_CONSUMER_STRATEGY_SINGLE_EVENT_STORE)
@WebServiceProfile
public class SingleEventSaveStrategy implements EventHandlingStrategy {

    private final ClientEventRecordMapper clientEventRecordMapper;
    private boolean enabled = false;

    public SingleEventSaveStrategy(final ClientEventRecordMapper clientEventRecordMapper) {
        this.clientEventRecordMapper = clientEventRecordMapper;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void accept(final ClientEventRecord record) {
        if (record.getId() == null) {
            this.clientEventRecordMapper.insert(record);
        } else {
            this.clientEventRecordMapper.updateByPrimaryKeySelective(record);
        }
    }

    @Override
    public void enable() {
        this.enabled = true;

    }

    public boolean isEnabled() {
        return this.enabled;
    }

}
