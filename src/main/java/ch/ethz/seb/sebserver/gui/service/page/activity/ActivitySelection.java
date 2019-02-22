/*
 * Copyright (c) 2018 ETH Zürich, Educational Development and Technology (LET)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.gui.service.page.activity;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

import org.eclipse.swt.widgets.TreeItem;

import ch.ethz.seb.sebserver.gbl.model.EntityKey;
import ch.ethz.seb.sebserver.gui.content.activity.Activity;
import ch.ethz.seb.sebserver.gui.service.page.PageContext.AttributeKeys;

public class ActivitySelection {

    public static final Consumer<TreeItem> EMPTY_FUNCTION = ti -> {
    };
    public static final Consumer<TreeItem> COLLAPSE_NONE_EMPTY = ti -> {
        ti.removeAll();
        ti.setItemCount(1);
    };

    public final Activity activity;
    final Map<String, String> attributes;
    Consumer<TreeItem> expandFunction = EMPTY_FUNCTION;

    public ActivitySelection(final Activity activity) {
        this.activity = activity;
        this.attributes = new HashMap<>();
    }

    public ActivitySelection withEntity(final EntityKey entityKey) {
        if (entityKey != null) {
            this.attributes.put(AttributeKeys.ENTITY_ID, entityKey.modelId);
            this.attributes.put(AttributeKeys.ENTITY_TYPE, entityKey.entityType.name());
        }

        return this;
    }

    public ActivitySelection withParentEntity(final EntityKey parentEntityKey) {
        if (parentEntityKey != null) {
            this.attributes.put(AttributeKeys.PARENT_ENTITY_ID, parentEntityKey.modelId);
            this.attributes.put(AttributeKeys.PARENT_ENTITY_TYPE, parentEntityKey.entityType.name());
        }

        return this;
    }

    public ActivitySelection withAttribute(final String name, final String value) {
        this.attributes.put(name, value);
        return this;
    }

    public Map<String, String> getAttributes() {
        return Collections.unmodifiableMap(this.attributes);
    }

    public ActivitySelection withExpandFunction(final Consumer<TreeItem> expandFunction) {
        if (expandFunction == null) {
            this.expandFunction = EMPTY_FUNCTION;
        }
        this.expandFunction = expandFunction;
        return this;
    }

    public void processExpand(final TreeItem item) {
        this.expandFunction.accept(item);
    }

    public String getEntityId() {
        return this.attributes.get(AttributeKeys.ENTITY_ID);
    }

}
