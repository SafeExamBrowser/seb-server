/*
 * Copyright (c) 2019 ETH Zürich, Educational Development and Technology (LET)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.gui.service.page.impl;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.rap.rwt.widgets.DialogCallback;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.MessageBox;
import org.eclipse.swt.widgets.Shell;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ch.ethz.seb.sebserver.gbl.api.APIMessage;
import ch.ethz.seb.sebserver.gbl.api.APIMessageError;
import ch.ethz.seb.sebserver.gbl.util.Utils;
import ch.ethz.seb.sebserver.gui.service.i18n.I18nSupport;
import ch.ethz.seb.sebserver.gui.service.page.ComposerService;
import ch.ethz.seb.sebserver.gui.service.page.PageContext;
import ch.ethz.seb.sebserver.gui.service.page.PageDefinition;
import ch.ethz.seb.sebserver.gui.service.page.PageEventListener;
import ch.ethz.seb.sebserver.gui.service.page.event.PageEvent;
import ch.ethz.seb.sebserver.gui.service.widget.Message;

public class PageContextImpl implements PageContext {

    private static final Logger log = LoggerFactory.getLogger(PageContextImpl.class);

    private static final ListenerComparator LIST_COMPARATOR = new ListenerComparator();

    private final I18nSupport i18nSupport;
    private final ComposerService composerService;
    private final Composite root;
    private final Composite parent;
    private final Map<String, String> attributes;

    PageContextImpl(
            final I18nSupport i18nSupport,
            final ComposerService composerService,
            final Composite root,
            final Composite parent,
            final Map<String, String> attributes) {

        this.i18nSupport = i18nSupport;
        this.composerService = composerService;
        this.root = root;
        this.parent = parent;
        this.attributes = Utils.immutableMapOf(attributes);
    }

    @Override
    public Shell getShell() {
        if (this.root == null) {
            return null;
        }

        return this.root.getShell();
    }

    @Override
    public ComposerService composerService() {
        return this.composerService;
    }

    @Override
    public Composite getRoot() {
        return this.root;
    }

    @Override
    public Composite getParent() {
        return this.parent;
    }

    @Override
    public PageContext copyOf(final Composite parent) {
        return new PageContextImpl(
                this.i18nSupport,
                this.composerService,
                this.root,
                parent,
                this.attributes);
    }

    @Override
    public PageContext copyOfAttributes(final PageContext otherContext) {
        final Map<String, String> attrs = new HashMap<>();
        attrs.putAll(this.attributes);
        attrs.putAll(((PageContextImpl) otherContext).attributes);
        return new PageContextImpl(
                this.i18nSupport,
                this.composerService,
                this.root,
                this.parent,
                attrs);
    }

    @Override
    public PageContext withAttr(final String key, final String value) {
        final Map<String, String> attrs = new HashMap<>();
        attrs.putAll(this.attributes);
        attrs.put(key, value);
        return new PageContextImpl(
                this.i18nSupport,
                this.composerService,
                this.root,
                this.parent, attrs);
    }

    @Override
    public String getAttribute(final String name) {
        return this.attributes.get(name);
    }

    @Override
    public String getAttribute(final String name, final String def) {
        if (this.attributes.containsKey(name)) {
            return this.attributes.get(name);
        } else {
            return def;
        }
    }

    @Override
    public boolean hasAttribute(final String name) {
        return this.attributes.containsKey(name);
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T extends PageEvent> void publishPageEvent(final T event) {
        final Class<? extends PageEvent> typeClass = event.getClass();
        final List<PageEventListener<T>> listeners = new ArrayList<>();
        ComposerService.traversePageTree(
                this.root,
                c -> {
                    final PageEventListener<?> listener =
                            (PageEventListener<?>) c.getData(PageEventListener.LISTENER_ATTRIBUTE_KEY);
                    return listener != null && listener.match(typeClass);
                },
                c -> listeners.add(((PageEventListener<T>) c.getData(PageEventListener.LISTENER_ATTRIBUTE_KEY))));

        if (listeners.isEmpty()) {
            return;
        }

        listeners.stream()
                .sorted(LIST_COMPARATOR)
                .forEach(listener -> listener.notify(event));
    }

    @Override
    @SuppressWarnings("serial")
    public void applyConfirmDialog(final String confirmMessage, final Runnable onOK) {
        final Message messageBox = new Message(
                this.root.getShell(),
                this.i18nSupport.getText("org.sebserver.dialog.confirm.title"),
                this.i18nSupport.getText(confirmMessage),
                SWT.OK | SWT.CANCEL);
        messageBox.open(new DialogCallback() {
            @Override
            public void dialogClosed(final int returnCode) {
                if (returnCode == SWT.OK) {
                    try {
                        onOK.run();
                    } catch (final Throwable t) {
                        log.error(
                                "Unexpected on confirm callback execution. This should not happen, plase secure the given onOK Runnable",
                                t);
                    }
                }
            }
        });
    }

//    public void applyValidationErrorDialog(final Collection<FieldValidationError> validationErrors) {
//        final Message messageBox = new Message(
//                this.root.getShell(),
//                this.i18nSupport.getText("org.sebserver.dialog.validationErrors.title"),
//                this.i18nSupport.getText(confirmMessage),
//                SWT.OK);
//    }

    @Override
    public void forwardToPage(
            final PageDefinition pageDefinition,
            final PageContext pageContext) {

        this.composerService.compose(
                pageDefinition.composer(),
                pageDefinition.applyPageContext(pageContext.copyOf(pageContext.getRoot())));
    }

    @Override
    public void forwardToMainPage(final PageContext pageContext) {
        forwardToPage(this.composerService.mainPage(), pageContext);
    }

    @Override
    public void forwardToLoginPage(final PageContext pageContext) {
        forwardToPage(this.composerService.loginPage(), pageContext);
    }

    @Override
    public void notifyError(final String errorMessage, final Throwable error) {
        if (error instanceof APIMessageError) {
            final List<APIMessage> errorMessages = ((APIMessageError) error).getErrorMessages();
            final MessageBox messageBox = new Message(
                    getShell(),
                    this.i18nSupport.getText("sebserver.error.unexpected"),
                    APIMessage.toHTML(errorMessages),
                    SWT.ERROR);
            messageBox.setMarkupEnabled(true);
            messageBox.open(null);
            return;
        }

        final MessageBox messageBox = new Message(
                getShell(),
                this.i18nSupport.getText("sebserver.error.unexpected"),
                error.toString(),
                SWT.ERROR);
        messageBox.open(null);
    }

    @Override
    public void notifyError(final Throwable error) {
        notifyError(error.getMessage(), error);
    }

    @Override
    public <T> T logoutOnError(final Throwable error) {
        // just to be sure we leave a clean and proper authorizationContext
        try {
            ((ComposerServiceImpl) this.composerService).authorizationContextHolder
                    .getAuthorizationContext()
                    .logout();
        } catch (final Exception e) {
            log.info("Cleanup logout failed: {}", e.getMessage());
        }

        MainPageState.clear();
        forwardToLoginPage(this.withAttr(
                AttributeKeys.AUTHORIZATION_FAILURE,
                error.getMessage()));

        return null;
    }

    @Override
    public String toString() {
        return "PageContextImpl [root=" + this.root + ", parent=" + this.parent + ", attributes=" + this.attributes
                + "]";
    }

    private static final class ListenerComparator implements Comparator<PageEventListener<?>> {
        @Override
        public int compare(final PageEventListener<?> o1, final PageEventListener<?> o2) {
            final int x = o1.priority();
            final int y = o2.priority();
            return (x < y) ? -1 : ((x == y) ? 0 : 1);
        }
    }

}
