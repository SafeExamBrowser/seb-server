/*
 * Copyright (c) 2018 ETH Zürich, Educational Development and Technology (LET)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.gui.content.action;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import org.eclipse.rap.rwt.RWT;
import org.eclipse.rap.rwt.template.ImageCell;
import org.eclipse.rap.rwt.template.Template;
import org.eclipse.rap.rwt.template.TextCell;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.swt.widgets.TreeItem;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import ch.ethz.seb.sebserver.gui.service.i18n.LocTextKey;
import ch.ethz.seb.sebserver.gui.service.page.PageContext;
import ch.ethz.seb.sebserver.gui.service.page.TemplateComposer;
import ch.ethz.seb.sebserver.gui.service.page.action.Action;
import ch.ethz.seb.sebserver.gui.service.page.event.ActionPublishEvent;
import ch.ethz.seb.sebserver.gui.service.page.event.ActionPublishEventListener;
import ch.ethz.seb.sebserver.gui.service.page.event.PageEventListener;
import ch.ethz.seb.sebserver.gui.widget.WidgetFactory;
import ch.ethz.seb.sebserver.gui.widget.WidgetFactory.CustomVariant;

@Lazy
@Component
public class ActionPane implements TemplateComposer {

    private static final String ACTION_EVENT_CALL_KEY = "ACTION_EVENT_CALL";

    private final WidgetFactory widgetFactory;

    private final Map<String, Tree> actionTrees = new HashMap<>();

    public ActionPane(final WidgetFactory widgetFactory) {
        super();
        this.widgetFactory = widgetFactory;
    }

    @Override
    public void compose(final PageContext pageContext) {

        final Label label = this.widgetFactory.labelLocalized(
                pageContext.getParent(),
                CustomVariant.TEXT_H2,
                new LocTextKey("sebserver.actionpane.title"));

        final GridData titleLayout = new GridData(SWT.FILL, SWT.TOP, true, false);
        titleLayout.verticalIndent = 10;
        titleLayout.horizontalIndent = 10;
        label.setLayoutData(titleLayout);

        label.setData(
                PageEventListener.LISTENER_ATTRIBUTE_KEY,
                new ActionPublishEventListener() {
                    @Override
                    public void notify(final ActionPublishEvent event) {
                        final Composite parent = pageContext.getParent();

                        final Tree treeForGroup = getTreeForGroup(parent, event.action.definition);

                        final TreeItem actionItem = ActionPane.this.widgetFactory.treeItemLocalized(
                                treeForGroup,
                                event.action.definition.title);

                        actionItem.setImage(event.action.definition.icon.getImage(
                                pageContext.getParent().getDisplay()));

                        actionItem.setData(ACTION_EVENT_CALL_KEY, event.action);
                        parent.layout();
                    }
                });
    }

    private Tree getTreeForGroup(final Composite parent, final ActionDefinition actionDefinition) {
        clearDisposedTrees();

        final ActionCategory category = actionDefinition.category;
        if (!this.actionTrees.containsKey(category.name())) {
            final Tree actionTree = createActionTree(parent, actionDefinition.category);
            this.actionTrees.put(category.name(), actionTree);
        }

        return this.actionTrees.get(category.name());
    }

    private Tree createActionTree(final Composite parent, final ActionCategory category) {

        final Composite composite = new Composite(parent, SWT.NONE);
        final GridData layout = new GridData(SWT.FILL, SWT.TOP, true, false);
        composite.setLayoutData(layout);
        composite.setLayout(new GridLayout());
        composite.setData(RWT.CUSTOM_VARIANT, "actionPane");
        composite.setData("CATEGORY", category);

        final Control[] children = parent.getChildren();
        for (final Control child : children) {
            final ActionCategory c = (ActionCategory) child.getData("CATEGORY");
            if (c != null && c.slotPosition > category.slotPosition) {
                composite.moveAbove(child);
                break;
            }
        }

        final Label labelSeparator = this.widgetFactory.labelSeparator(composite);
        final GridData separatorLayout = new GridData(SWT.FILL, SWT.TOP, true, false);
        labelSeparator.setLayoutData(separatorLayout);

        // title
        if (category.title != null) {
            final Label actionsTitle = this.widgetFactory.labelLocalized(
                    composite,
                    CustomVariant.TEXT_H3,
                    category.title);
            final GridData titleLayout = new GridData(SWT.FILL, SWT.TOP, true, false);
            titleLayout.horizontalIndent = 10;
            titleLayout.verticalIndent = 10;
            actionsTitle.setLayoutData(titleLayout);
        }

        // action tree
        final Tree actions = this.widgetFactory.treeLocalized(
                composite,
                SWT.SINGLE | SWT.FULL_SELECTION);
        actions.setData(RWT.CUSTOM_VARIANT, "actions");
        final GridData gridData = new GridData(SWT.FILL, SWT.FILL, true, true);
        actions.setLayoutData(gridData);
        final Template template = new Template();
        final ImageCell imageCell = new ImageCell(template);
        imageCell.setLeft(0, 0)
                .setWidth(40)
                .setTop(0)
                .setBottom(0, 0)
                .setHorizontalAlignment(SWT.LEFT);
        imageCell.setBindingIndex(0);
        final TextCell textCell = new TextCell(template);
        textCell.setLeft(0, 30)
                .setWidth(150)
                .setTop(7)
                .setBottom(0, 0)
                .setHorizontalAlignment(SWT.LEFT);
        textCell.setBindingIndex(0);
        actions.setData(RWT.ROW_TEMPLATE, template);

        actions.addListener(SWT.Selection, event -> {
            final TreeItem treeItem = (TreeItem) event.item;

            final Action action = (Action) treeItem.getData(ACTION_EVENT_CALL_KEY);
            action.run();

            if (!treeItem.isDisposed()) {
                treeItem.getParent().deselectAll();
            }
        });

        return actions;
    }

    private void clearDisposedTrees() {
        new ArrayList<>(this.actionTrees.entrySet())
                .stream()
                .forEach(entry -> {
                    final Control c = entry.getValue();
                    // of tree is already disposed.. remove it
                    if (c.isDisposed()) {
                        this.actionTrees.remove(entry.getKey());
                    }
                    // check access from current thread
                    try {
                        c.getBounds();
                    } catch (final Exception e) {
                        this.actionTrees.remove(entry.getKey());
                    }
                });
    }

}
