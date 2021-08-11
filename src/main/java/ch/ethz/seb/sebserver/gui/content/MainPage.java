/*
 * Copyright (c) 2019 ETH Zürich, Educational Development and Technology (LET)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.gui.content;

import java.util.function.Consumer;

import org.eclipse.rap.rwt.RWT;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import ch.ethz.seb.sebserver.gbl.profile.GuiProfile;
import ch.ethz.seb.sebserver.gui.content.activity.ActivitiesPane;
import ch.ethz.seb.sebserver.gui.service.i18n.LocTextKey;
import ch.ethz.seb.sebserver.gui.service.i18n.PolyglotPageService;
import ch.ethz.seb.sebserver.gui.service.page.PageContext;
import ch.ethz.seb.sebserver.gui.service.page.PageService;
import ch.ethz.seb.sebserver.gui.service.page.TemplateComposer;
import ch.ethz.seb.sebserver.gui.service.page.event.ActionEvent;
import ch.ethz.seb.sebserver.gui.service.page.event.ActionEventListener;
import ch.ethz.seb.sebserver.gui.service.page.event.PageEventListener;
import ch.ethz.seb.sebserver.gui.widget.WidgetFactory;
import ch.ethz.seb.sebserver.gui.widget.WidgetFactory.ImageIcon;

@Lazy
@Component
@GuiProfile
public class MainPage implements TemplateComposer {

    static final Logger log = LoggerFactory.getLogger(MainPage.class);

    private static final int ACTIVITY_PANE_WEIGHT = 18;
    private static final int CONTENT_PANE_WEIGHT = 65;
    private static final int ACTION_PANE_WEIGHT = 20;
    private static final int[] DEFAULT_SASH_WEIGHTS = new int[] {
            ACTIVITY_PANE_WEIGHT,
            CONTENT_PANE_WEIGHT,
            ACTION_PANE_WEIGHT
    };
    private static final int[] OPENED_SASH_WEIGHTS = new int[] { 0, 100, 0 };

    private final WidgetFactory widgetFactory;
    private final PolyglotPageService polyglotPageService;

    public MainPage(
            final WidgetFactory widgetFactory,
            final PolyglotPageService polyglotPageService) {

        this.widgetFactory = widgetFactory;
        this.polyglotPageService = polyglotPageService;
    }

    @Override
    public void compose(final PageContext pageContext) {

        final Composite parent = pageContext.getParent();
        parent.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
        WidgetFactory.setTestId(parent, "main-page");

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

        final Composite content = PageService.createManagedVScrolledComposite(
                mainSash,
                scrolledComposite -> {
                    final Composite result = new Composite(scrolledComposite, SWT.NONE);
                    result.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
                    final GridLayout contentOuterLayout = new GridLayout();
                    contentOuterLayout.marginHeight = 0;
                    contentOuterLayout.marginWidth = 0;
                    result.setLayout(contentOuterLayout);
                    return result;
                },
                false);

        final Button toggleView = this.widgetFactory.imageButton(
                ImageIcon.MAXIMIZE,
                content,
                new LocTextKey("sebserver.mainpage.maximize.tooltip"),
                event -> {
                    final Button ib = (Button) event.widget;
                    if ((Boolean) ib.getData("fullScreen")) {
                        mainSash.setWeights(DEFAULT_SASH_WEIGHTS);
                        ib.setData("fullScreen", false);
                        ib.setImage(WidgetFactory.ImageIcon.MAXIMIZE.getImage(ib.getDisplay()));
                        this.polyglotPageService.injectI18n(
                                ib,
                                null,
                                new LocTextKey("sebserver.mainpage.maximize.tooltip"));
                    } else {
                        mainSash.setWeights(OPENED_SASH_WEIGHTS);
                        ib.setData("fullScreen", true);
                        ib.setImage(WidgetFactory.ImageIcon.MINIMIZE.getImage(ib.getDisplay()));
                        this.polyglotPageService.injectI18n(
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
        final GridLayout contentObjectsLayout = new GridLayout();
        contentObjectsLayout.marginHeight = 0;
        contentObjectsLayout.marginWidth = 0;
        contentObjects.setLayout(contentObjectsLayout);
        contentObjects.setData(
                PageEventListener.LISTENER_ATTRIBUTE_KEY,
                new ContentActionEventListener(event -> pageContext
                        .composerService()
                        .compose(
                                event.action.definition.targetState.contentPaneComposer(),
                                event.action.pageContext().copyOf(contentObjects)),
                        2));

        final Composite actionPane = new Composite(mainSash, SWT.NONE);
        actionPane.setLayoutData(new GridData(SWT.FILL, SWT.TOP, true, false));
        final GridLayout actionPaneGrid = new GridLayout();
        actionPane.setLayout(actionPaneGrid);
        actionPane.setData(RWT.CUSTOM_VARIANT, "actionPane");
        actionPane.setData(
                PageEventListener.LISTENER_ATTRIBUTE_KEY,
                new ContentActionEventListener(event -> pageContext
                        .composerService()
                        .compose(
                                event.action.definition.targetState.actionPaneComposer(),
                                event.action.pageContext().copyOf(actionPane)),
                        1));

        pageContext.composerService().compose(
                ActivitiesPane.class,
                pageContext.copyOf(nav));

        mainSash.setWeights(DEFAULT_SASH_WEIGHTS);
    }

    private static final class ContentActionEventListener implements ActionEventListener {

        private final int priority;
        private final Consumer<ActionEvent> apply;

        protected ContentActionEventListener(final Consumer<ActionEvent> apply, final int priority) {
            this.apply = apply;
            this.priority = priority;
        }

        @Override
        public int priority() {
            return this.priority;
        }

        @Override
        public void notify(final ActionEvent event) {
            this.apply.accept(event);
        }
    }

}
