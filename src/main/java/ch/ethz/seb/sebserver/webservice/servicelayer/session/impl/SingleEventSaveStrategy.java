/*
 * Copyright (c) 2018 ETH Zürich, Educational Development and Technology (LET)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.webservice.servicelayer.session.impl;

import org.springframework.context.annotation.Lazy;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import ch.ethz.seb.sebserver.SEBServerInit;
import ch.ethz.seb.sebserver.SEBServerInitEvent;
import ch.ethz.seb.sebserver.gbl.model.session.ClientEvent.EventType;
import ch.ethz.seb.sebserver.gbl.model.session.ClientNotification;
import ch.ethz.seb.sebserver.gbl.model.session.ClientNotification.NotificationType;
import ch.ethz.seb.sebserver.gbl.profile.WebServiceProfile;
import ch.ethz.seb.sebserver.gbl.util.Pair;
import ch.ethz.seb.sebserver.webservice.datalayer.batis.mapper.ClientEventRecordMapper;
import ch.ethz.seb.sebserver.webservice.datalayer.batis.model.ClientEventRecord;
import ch.ethz.seb.sebserver.webservice.servicelayer.session.EventHandlingStrategy;
import ch.ethz.seb.sebserver.webservice.servicelayer.session.SEBClientNotificationService;

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

    private final SEBClientNotificationService sebClientNotificationService;
    private final ClientEventRecordMapper clientEventRecordMapper;
    private boolean enabled = false;

    public SingleEventSaveStrategy(
            final SEBClientNotificationService sebClientNotificationService,
            final ClientEventRecordMapper clientEventRecordMapper) {

        this.sebClientNotificationService = sebClientNotificationService;
        this.clientEventRecordMapper = clientEventRecordMapper;
    }

    @EventListener(SEBServerInitEvent.class)
    public void init() {
        if (this.enabled) {
            SEBServerInit.INIT_LOGGER.info("------>");
            SEBServerInit.INIT_LOGGER.info("------> Run SingleEventSaveStrategy for SEB event handling");
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void accept(final ClientEventRecord record) {

        if (EventType.isNotificationEvent(record.getType())) {
            final Pair<NotificationType, String> typeAndPlainText =
                    ClientNotification.extractTypeAndPlainText(record.getText());

            final ClientNotification clientNotification = new ClientNotification(
                    record.getId(),
                    record.getClientConnectionId(),
                    EventType.byId(record.getType()),
                    record.getClientTime(),
                    record.getServerTime(),
                    (record.getNumericValue() != null) ? record.getNumericValue().doubleValue() : null,
                    typeAndPlainText.b,
                    typeAndPlainText.a);

            switch (clientNotification.eventType) {
                case NOTIFICATION: {
                    this.sebClientNotificationService.newNotification(clientNotification);
                    break;
                }
                case NOTIFICATION_CONFIRMED: {
                    this.sebClientNotificationService.confirmPendingNotification(clientNotification);
                    break;
                }
                default:
            }

        } else {
            if (record.getId() == null) {
                this.clientEventRecordMapper.insert(record);
            } else {
                this.clientEventRecordMapper.updateByPrimaryKeySelective(record);
            }
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
