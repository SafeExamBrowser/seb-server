/*
 * Copyright (c) 2019 ETH Zürich, Educational Development and Technology (LET)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.gui.service.page.impl;

import org.eclipse.rap.rwt.RWT;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import ch.ethz.seb.sebserver.gbl.profile.GuiProfile;
import ch.ethz.seb.sebserver.gui.service.i18n.LocTextKey;
import ch.ethz.seb.sebserver.gui.service.page.PageContext;
import ch.ethz.seb.sebserver.gui.service.page.TemplateComposer;
import ch.ethz.seb.sebserver.gui.service.page.activity.ActivitiesPane;
import ch.ethz.seb.sebserver.gui.service.page.event.ActivitySelectionEvent;
import ch.ethz.seb.sebserver.gui.service.page.event.ActivitySelectionListener;
import ch.ethz.seb.sebserver.gui.service.page.event.PageEventListener;
import ch.ethz.seb.sebserver.gui.service.widget.WidgetFactory;
import ch.ethz.seb.sebserver.gui.service.widget.WidgetFactory.IconButtonType;

@Lazy
@Component
@GuiProfile
public class SEBMainPage implements TemplateComposer {

    static final Logger log = LoggerFactory.getLogger(SEBMainPage.class);

    public static final String ATTR_MAIN_PAGE_STATE = "MAIN_PAGE_STATE";

    private static final int ACTIVITY_PANE_WEIGHT = 20;
    private static final int CONTENT_PANE_WEIGHT = 65;
    private static final int ACTION_PANE_WEIGHT = 15;
    private static final int[] DEFAULT_SASH_WEIGHTS = new int[] {
            ACTIVITY_PANE_WEIGHT,
            CONTENT_PANE_WEIGHT,
            ACTION_PANE_WEIGHT
    };
    private static final int[] OPENED_SASH_WEIGHTS = new int[] { 0, 100, 0 };

    private final WidgetFactory widgetFactory;

    public SEBMainPage(final WidgetFactory widgetFactory) {
        this.widgetFactory = widgetFactory;
    }

    @Override
    public void compose(final PageContext pageContext) {
        MainPageState.clear();

        final Composite parent = pageContext.getParent();
        parent.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));

        final SashForm mainSash = new SashForm(parent, SWT.HORIZONTAL);
        final GridLayout gridLayout = new GridLayout();

        mainSash.setLayout(gridLayout);
        mainSash.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));

        final Composite nav = new Composite(mainSash, SWT.NONE);
        nav.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
        final GridLayout navLayout = new GridLayout();
        navLayout.marginHeight = 20;
        navLayout.marginWidth = 0;
        nav.setLayout(navLayout);

        final Composite content = new Composite(mainSash, SWT.NONE);
        content.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
        final GridLayout contentOuterlayout = new GridLayout();
        contentOuterlayout.marginHeight = 0;
        contentOuterlayout.marginWidth = 0;
        content.setLayout(contentOuterlayout);

        final Label toggleView = this.widgetFactory.imageButton(
                IconButtonType.MAXIMIZE,
                content,
                new LocTextKey("sebserver.mainpage.maximize.tooltip"),
                event -> {
                    final Label ib = (Label) event.widget;
                    if ((Boolean) ib.getData("fullScreen")) {
                        mainSash.setWeights(DEFAULT_SASH_WEIGHTS);
                        ib.setData("fullScreen", false);
                        ib.setImage(WidgetFactory.IconButtonType.MAXIMIZE.getImage(ib.getDisplay()));
                        this.widgetFactory.injectI18n(
                                ib,
                                null,
                                new LocTextKey("sebserver.mainpage.maximize.tooltip"));
                    } else {
                        mainSash.setWeights(OPENED_SASH_WEIGHTS);
                        ib.setData("fullScreen", true);
                        ib.setImage(WidgetFactory.IconButtonType.MINIMIZE.getImage(ib.getDisplay()));
                        this.widgetFactory.injectI18n(
                                ib,
                                null,
                                new LocTextKey("sebserver.mainpage.minimize.tooltip"));
                    }
                });
        final GridData gridData = new GridData(SWT.RIGHT, SWT.TOP, true, false);
        toggleView.setLayoutData(gridData);
        toggleView.setData("fullScreen", false);

        final Composite contentObjects = new Composite(content, SWT.NONE);
        contentObjects.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
        final GridLayout contentObjectslayout = new GridLayout();
        contentObjectslayout.marginHeight = 0;
        contentObjectslayout.marginWidth = 0;
        contentObjects.setLayout(contentObjectslayout);
        contentObjects.setData(PageEventListener.LISTENER_ATTRIBUTE_KEY,
                new ActivitySelectionListener() {
                    @Override
                    public int priority() {
                        return 2;
                    }

                    @Override
                    public void notify(final ActivitySelectionEvent event) {
                        pageContext.composerService().compose(
                                event.selection.activity.contentPaneComposer,
                                pageContext
                                        .copyOf(contentObjects)
                                        .withSelection(event.selection));
                    }
                });

        final Composite actionPane = new Composite(mainSash, SWT.NONE);
        actionPane.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
        final GridLayout actionPaneGrid = new GridLayout();
        actionPane.setLayout(actionPaneGrid);
        actionPane.setData(RWT.CUSTOM_VARIANT, "actionPane");
        actionPane.setData(PageEventListener.LISTENER_ATTRIBUTE_KEY,
                new ActivitySelectionListener() {
                    @Override
                    public int priority() {
                        return 1;
                    }

                    @Override
                    public void notify(final ActivitySelectionEvent event) {
                        pageContext.composerService().compose(
                                event.selection.activity.actionPaneComposer,
                                pageContext
                                        .copyOf(actionPane)
                                        .withSelection(event.selection));
                    }
                });

        pageContext.composerService().compose(
                ActivitiesPane.class,
                pageContext.copyOf(nav));

        mainSash.setWeights(DEFAULT_SASH_WEIGHTS);
    }

}
