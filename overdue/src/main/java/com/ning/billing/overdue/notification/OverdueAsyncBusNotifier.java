/*
 * Copyright 2010-2013 Ning, Inc.
 *
 * Ning licenses this file to you under the Apache License, version 2.0
 * (the "License"); you may not use this file except in compliance with the
 * License.  You may obtain a copy of the License at:
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  See the
 * License for the specific language governing permissions and limitations
 * under the License.
 */

package com.ning.billing.overdue.notification;

import java.util.UUID;

import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ning.billing.callcontext.InternalCallContext;
import com.ning.billing.notificationq.api.NotificationEvent;
import com.ning.billing.notificationq.api.NotificationQueueService;
import com.ning.billing.overdue.OverdueProperties;
import com.ning.billing.overdue.listener.OverdueDispatcher;
import com.ning.billing.overdue.listener.OverdueListener;
import com.ning.billing.util.callcontext.CallOrigin;
import com.ning.billing.util.callcontext.InternalCallContextFactory;
import com.ning.billing.util.callcontext.UserType;

import com.google.inject.Inject;

public class OverdueAsyncBusNotifier extends DefaultOverdueNotifierBase implements OverdueNotifier {

    private static final Logger log = LoggerFactory.getLogger(OverdueCheckNotifier.class);

    public static final String OVERDUE_ASYNC_BUS_NOTIFIER_QUEUE = "overdue-async-bus-queue";


    @Inject
    public OverdueAsyncBusNotifier(final NotificationQueueService notificationQueueService, final OverdueProperties config,
                                   final InternalCallContextFactory internalCallContextFactory,
                                   final OverdueDispatcher dispatcher) {
        super(notificationQueueService, config, internalCallContextFactory, dispatcher);
    }

    @Override
    public String getQueueName() {
        return OVERDUE_ASYNC_BUS_NOTIFIER_QUEUE;
    }

    @Override
    public void handleReadyNotification(final NotificationEvent notificationKey, final DateTime eventDate, final UUID userToken, final Long accountRecordId, final Long tenantRecordId) {
        try {
            if (!(notificationKey instanceof OverdueAsyncBusNotificationKey)) {
                log.error("Overdue service received Unexpected notificationKey {}", notificationKey.getClass().getName());
                return;
            }

            final OverdueAsyncBusNotificationKey key = (OverdueAsyncBusNotificationKey) notificationKey;
            switch (key.getAction()) {
                case CLEAR:
                    dispatcher.clearOverdueForAccount(key.getUuidKey(), createCallContext(userToken, accountRecordId, tenantRecordId));
                    break;
                case REFRESH:
                    dispatcher.processOverdueForAccount(key.getUuidKey(), createCallContext(userToken, accountRecordId, tenantRecordId));
                    break;
                default:
                    throw new RuntimeException("Unexpected action " + key.getAction() + " for account " + key.getUuidKey());
            }
        } catch (IllegalArgumentException e) {
            log.error("The key returned from the queue " + OVERDUE_ASYNC_BUS_NOTIFIER_QUEUE + " does not contain a valid UUID", e);
        }
    }


}
