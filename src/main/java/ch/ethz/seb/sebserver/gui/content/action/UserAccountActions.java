/*
 * Copyright (c) 2019 ETH Zürich, Educational Development and Technology (LET)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.gui.content.action;

public final class UserAccountActions {

//    public static Action newUserAccount(final Action action) {
//        return goToUserAccount(action, null, true);
//    }
//
//    public static Action viewUserAccountFromList(final Action action) {
//        return fromSelection(action, false);
//    }
//
//    public static Action editUserAccountFromList(final Action action) {
//        return fromSelection(action, true);
//    }
//
//    public static Action editUserAccount(final Action action) {
//        return goToUserAccount(action, null, true);
//    }
//
//    public static Action cancelEditUserAccount(final Action action) {
//        if (action.pageContext().getEntityKey() == null) {
//            final Action toList = action.pageContext().createAction(ActionDefinition.USER_ACCOUNT_VIEW_LIST);
//            action.pageContext().publishPageEvent(new ActionEvent(toList, false));
//            return toList;
//        } else {
//            return goToUserAccount(action, null, false);
//        }
//    }
//
//    private static Action fromSelection(final Action action, final boolean edit) {
//        final Collection<String> selection = action.getSelectionSupplier().get();
//        if (selection.isEmpty()) {
//            throw new PageMessageException("sebserver.useraccount.info.pleaseSelect");
//        }
//
//        return goToUserAccount(action, selection.iterator().next(), edit);
//    }
//
//    private static Action goToUserAccount(
//            final Action action,
//            final String modelId,
//            final boolean edit) {
//
//        action.withAttribute(AttributeKeys.READ_ONLY, String.valueOf(!edit));
//        if (modelId != null) {
//            action.withEntity(new EntityKey(modelId, EntityType.USER));
//        }
//
//        return action;
//    }

}
