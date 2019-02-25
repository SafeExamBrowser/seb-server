/*
 * Copyright (c) 2018 ETH Zürich, Educational Development and Technology (LET)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.gui.service.page.event;

public interface ActionEventListener extends PageEventListener<ActionEvent> {

    @Override
    default boolean match(final Class<? extends PageEvent> type) {
        return type == ActionEvent.class;
    }

//    static ActionEventListener of(final Consumer<ActionEvent> eventConsumer) {
//        return new ActionEventListener() {
//            @Override
//            public void notify(final ActionEvent event) {
//                eventConsumer.accept(event);
//            }
//        };
//    }
//

//    static ActionEventListener of(
//            final Predicate<ActionEvent> predicate,
//            final Consumer<ActionEvent> eventConsumer) {
//
//        return new ActionEventListener() {
//            @Override
//            public void notify(final ActionEvent event) {
//                if (predicate.test(event)) {
//                    eventConsumer.accept(event);
//                }
//            }
//        };
//    }

//    static ActionEventListener of(
//            final ActionDefinition actionDefinition,
//            final Consumer<ActionEvent> eventConsumer) {
//
//        return new ActionEventListener() {
//            @Override
//            public void notify(final ActionEvent event) {
//                if (event.actionDefinition == actionDefinition) {
//                    eventConsumer.accept(event);
//                }
//            }
//        };
//    }
//
//    static void injectListener(
//            final Widget widget,
//            final ActionDefinition actionDefinition,
//            final Consumer<ActionEvent> eventConsumer) {
//
//        widget.setData(
//                PageEventListener.LISTENER_ATTRIBUTE_KEY,
//                of(actionDefinition, eventConsumer));
//    }

}
