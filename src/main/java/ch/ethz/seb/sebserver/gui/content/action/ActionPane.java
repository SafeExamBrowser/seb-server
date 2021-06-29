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

import org.apache.commons.lang3.StringUtils;
import org.eclipse.rap.rwt.RWT;
import org.eclipse.rap.rwt.template.ImageCell;
import org.eclipse.rap.rwt.template.Template;
import org.eclipse.rap.rwt.template.TextCell;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.RGBA;
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
import ch.ethz.seb.sebserver.gui.service.i18n.PolyglotPageService;
import ch.ethz.seb.sebserver.gui.service.page.PageContext;
import ch.ethz.seb.sebserver.gui.service.page.PageService;
import ch.ethz.seb.sebserver.gui.service.page.TemplateComposer;
import ch.ethz.seb.sebserver.gui.service.page.event.ActionActivationEvent;
import ch.ethz.seb.sebserver.gui.service.page.event.ActionActivationEventListener;
import ch.ethz.seb.sebserver.gui.service.page.event.ActionPublishEventListener;
import ch.ethz.seb.sebserver.gui.service.page.event.PageEventListener;
import ch.ethz.seb.sebserver.gui.service.page.impl.PageAction;
import ch.ethz.seb.sebserver.gui.widget.WidgetFactory;
import ch.ethz.seb.sebserver.gui.widget.WidgetFactory.CustomVariant;

@Lazy
@Component
public class ActionPane implements TemplateComposer {

    public static final String ACTION_EVENT_CALL_KEY = "ACTION_EVENT_CALL";
    private static final LocTextKey TITLE_KEY = new LocTextKey("sebserver.actionpane.title");

    private final PageService pageService;
    private final WidgetFactory widgetFactory;

    protected ActionPane(final PageService pageService) {
        this.pageService = pageService;
        this.widgetFactory = pageService.getWidgetFactory();
    }

    @Override
    public void compose(final PageContext pageContext) {
        final Composite root = pageContext.getParent();
        final Map<String, Tree> actionTrees = new HashMap<>();
        final Label label = this.widgetFactory.labelLocalized(
                root,
                CustomVariant.TEXT_H2,
                TITLE_KEY);

        final GridData titleLayout = new GridData(SWT.FILL, SWT.TOP, true, false);
        titleLayout.verticalIndent = 10;
        titleLayout.horizontalIndent = 10;
        if (StringUtils.isBlank(label.getText())) {
            titleLayout.heightHint = 0;
        }
        label.setLayoutData(titleLayout);

        final Composite scroll = PageService.createManagedVScrolledComposite(
                root,
                scrolledComposite -> {
                    final Composite composite = new Composite(scrolledComposite, SWT.NONE);
                    final GridData gridData = new GridData(SWT.FILL, SWT.TOP, true, false);
                    final GridLayout gridLayout = new GridLayout();
                    gridLayout.horizontalSpacing = 0;
                    gridData.heightHint = 0;
                    composite.setLayoutData(gridData);
                    composite.setLayout(gridLayout);
                    return composite;
                },
                false, false, true);

        label.setData(
                PageEventListener.LISTENER_ATTRIBUTE_KEY,
                (ActionPublishEventListener) event -> {
                    final Composite parent = scroll;
                    final Tree treeForGroup = getTreeForGroup(actionTrees, parent, event.action.definition, true);
                    final TreeItem actionItem = ActionPane.this.widgetFactory.treeItemLocalized(
                            treeForGroup,
                            event.action.getTitle());

                    if (event.action.definition.icon != null) {
                        final Image image = event.active
                                ? event.action.definition.icon.getImage(parent.getDisplay())
                                : event.action.definition.icon.getGreyedImage(parent.getDisplay());
                        actionItem.setImage(image);
                    }

                    if (!event.active) {
                        actionItem.setForeground(new Color(parent.getDisplay(), new RGBA(150, 150, 150, 50)));
                    }
                    actionItem.setData(ACTION_EVENT_CALL_KEY, event.action);
                    if (event.actionConsumer != null) {
                        event.actionConsumer.accept(actionItem);
                    }
                    parent.layout();
                });

        scroll.setData(
                PageEventListener.LISTENER_ATTRIBUTE_KEY,
                (ActionActivationEventListener) event -> {
                    final Composite parent = scroll;
                    for (final ActionDefinition ad : event.actions) {
                        final TreeItem actionItem = findAction(actionTrees, parent, ad);
                        if (actionItem == null) {
                            continue;
                        }

                        de_activate_action_icon(event, parent, actionItem, ad);
                    }

                    if (event.decoration != null) {
                        final TreeItem actionItemToDecorate = findAction(actionTrees, parent, event.decoration._1);
                        if (actionItemToDecorate != null && event.decoration._2 != null) {
                            actionItemToDecorate.setImage(0,
                                    event.decoration._2.icon.getImage(parent.getDisplay()));
                            ActionPane.this.pageService.getPolyglotPageService().injectI18n(
                                    actionItemToDecorate, event.decoration._2.title);
                        }

                        de_activate_action_icon(event, parent, actionItemToDecorate, event.decoration._1);
                    }
                });
    }

    private void de_activate_action_icon(
            final ActionActivationEvent event,
            final Composite parent,
            final TreeItem actionItemToDecorate,
            final ActionDefinition actionDef) {

        final PageAction action = (PageAction) actionItemToDecorate.getData(ACTION_EVENT_CALL_KEY);
        final Image image = event.activation
                ? actionDef.icon.getImage(parent.getDisplay())
                : actionDef.icon.getGreyedImage(parent.getDisplay());
        actionItemToDecorate.setImage(image);
        if (event.activation) {
            actionItemToDecorate.setForeground(null);
        } else {
            actionItemToDecorate
                    .setForeground(new Color(parent.getDisplay(), new RGBA(150, 150, 150, 50)));
            ActionPane.this.pageService.getPolyglotPageService().injectI18n(
                    actionItemToDecorate,
                    (action != null) ? action.getTitle() : actionDef.title);
        }
    }

    private TreeItem findAction(
            final Map<String, Tree> actionTrees,
            final Composite parent,
            final ActionDefinition actionDefinition) {
        final Tree treeForGroup = getTreeForGroup(actionTrees, parent, actionDefinition, false);
        if (treeForGroup == null) {
            return null;
        }

        for (int i = 0; i < treeForGroup.getItemCount(); i++) {
            final TreeItem item = treeForGroup.getItem(i);
            if (item == null) {
                continue;
            }

            final PageAction action = (PageAction) item.getData(ACTION_EVENT_CALL_KEY);
            if (action == null) {
                continue;
            }

            if (action.definition == actionDefinition) {
                return item;
            }
        }

        return null;
    }

    private Tree getTreeForGroup(
            final Map<String, Tree> actionTrees,
            final Composite parent,
            final ActionDefinition actionDefinition,
            final boolean create) {

        clearDisposedTrees(actionTrees);

        final ActionCategory category = actionDefinition.category;
        if (!actionTrees.containsKey(category.name()) && create) {
            final Tree actionTree = createActionTree(parent, actionDefinition.category);
            actionTrees.put(category.name(), actionTree);
        }

        return actionTrees.get(category.name());
    }

    private Tree createActionTree(final Composite parent, final ActionCategory category) {

        final Composite composite = new Composite(parent, SWT.NONE);
        final GridData layout = new GridData(SWT.FILL, SWT.TOP, true, false);
        composite.setLayoutData(layout);
        final GridLayout gridLayout = new GridLayout();
        gridLayout.marginHeight = 0;
        gridLayout.marginWidth = 0;
        composite.setLayout(gridLayout);
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

        // title
        if (this.pageService.getI18nSupport().hasText(category.title)) {
            final Label actionsTitle = this.widgetFactory.labelLocalized(
                    composite,
                    CustomVariant.TEXT_H3,
                    category.title);
            final GridData titleLayout = new GridData(SWT.FILL, SWT.TOP, true, false);
            actionsTitle.setLayoutData(titleLayout);
        }

        // action tree
        final Tree actions = this.widgetFactory.treeLocalized(
                composite,
                SWT.SINGLE | SWT.FULL_SELECTION | SWT.NO_SCROLL);
        actions.setData(RWT.CUSTOM_VARIANT, CustomVariant.ACTIONS.key);
        final GridData gridData = new GridData(SWT.FILL, SWT.TOP, true, false);
        actions.setLayoutData(gridData);
        final Template template = new Template();
        final ImageCell imageCell = new ImageCell(template);
        imageCell.setLeft(0, -8)
                .setWidth(20)
                .setTop(0)
                .setBottom(0, 0)
                .setHorizontalAlignment(SWT.LEFT)
                .setBackground(null);
        imageCell.setBindingIndex(0);
        final TextCell textCell = new TextCell(template);
        textCell.setLeft(0, 20)
                .setWidth(SWT.DEFAULT)
                .setTop(7)
                .setBottom(0, 0)
                .setHorizontalAlignment(SWT.LEFT);
        textCell.setBindingIndex(0);
        actions.setData(RWT.ROW_TEMPLATE, template);

        actions.addListener(SWT.Selection, event -> {
            final TreeItem treeItem = (TreeItem) event.item;
            treeItem.getParent().deselectAll();
            if (event.button == 1) {
                final PageAction action = (PageAction) treeItem.getData(ACTION_EVENT_CALL_KEY);
                this.pageService.executePageAction(action);
                if (!treeItem.isDisposed()) {

                    final PageAction switchAction = action.getSwitchAction();
                    if (switchAction != null) {
                        final PolyglotPageService polyglotPageService = this.pageService.getPolyglotPageService();
                        polyglotPageService.injectI18n(treeItem, switchAction.getTitle());
                        treeItem.setImage(switchAction.definition.icon.getImage(treeItem.getDisplay()));
                        treeItem.setData(ACTION_EVENT_CALL_KEY, switchAction);
                    }
                }
            }
        });

        return actions;
    }

    private void clearDisposedTrees(final Map<String, Tree> actionTrees) {
        new ArrayList<>(actionTrees.entrySet())
                .forEach(entry -> {
                    final Control c = entry.getValue();
                    // of tree is already disposed.. remove it
                    if (c.isDisposed()) {
                        actionTrees.remove(entry.getKey());
                    }
                    // check access from current thread
                    try {
                        c.getBounds();
                    } catch (final Exception e) {
                        actionTrees.remove(entry.getKey());
                    }
                });
    }

}
