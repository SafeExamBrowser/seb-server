/*
 * Copyright (c) 2018 ETH Zürich, Educational Development and Technology (LET)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.gui.service.page.action;

import org.eclipse.rap.rwt.RWT;
import org.eclipse.rap.rwt.template.ImageCell;
import org.eclipse.rap.rwt.template.Template;
import org.eclipse.rap.rwt.template.TextCell;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.swt.widgets.TreeItem;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import ch.ethz.seb.sebserver.gui.service.i18n.LocTextKey;
import ch.ethz.seb.sebserver.gui.service.page.PageContext;
import ch.ethz.seb.sebserver.gui.service.page.TemplateComposer;
import ch.ethz.seb.sebserver.gui.service.page.event.ActionPublishEvent;
import ch.ethz.seb.sebserver.gui.service.page.event.ActionPublishEventListener;
import ch.ethz.seb.sebserver.gui.service.page.event.PageEventListener;
import ch.ethz.seb.sebserver.gui.service.widget.WidgetFactory;
import ch.ethz.seb.sebserver.gui.service.widget.WidgetFactory.CustomVariant;

@Lazy
@Component
public class ActionPane implements TemplateComposer {

    private static final String ACTION_EVENT_CALL_KEY = "ACTION_EVENT_CALL";

    private final WidgetFactory widgetFactory;

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

        final Tree actions = this.widgetFactory.treeLocalized(
                pageContext.getParent(),
                SWT.SINGLE | SWT.FULL_SELECTION);
        actions.setData(RWT.CUSTOM_VARIANT, "actions");
        final GridData gridData = new GridData(SWT.FILL, SWT.FILL, true, true);
        actions.setLayoutData(gridData);
        final Template template = new Template();
        final ImageCell imageCell = new ImageCell(template);
        imageCell.setLeft(0, 0).setWidth(40).setTop(0).setBottom(0, 0).setHorizontalAlignment(SWT.LEFT);
        imageCell.setBindingIndex(0);
        final TextCell textCell = new TextCell(template);
        textCell.setLeft(0, 30).setWidth(150).setTop(7).setBottom(0, 0).setHorizontalAlignment(SWT.LEFT);
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

        actions.setData(
                PageEventListener.LISTENER_ATTRIBUTE_KEY,
                new ActionPublishEventListener() {
                    @Override
                    public void notify(final ActionPublishEvent event) {

                        final TreeItem actionItem = ActionPane.this.widgetFactory.treeItemLocalized(
                                actions,
                                event.action.definition.name);

                        actionItem.setImage(event.action.definition.icon.getImage(
                                pageContext.getParent().getDisplay()));

                        actionItem.setData(ACTION_EVENT_CALL_KEY, event.action);

                    }
                });

    }

}
