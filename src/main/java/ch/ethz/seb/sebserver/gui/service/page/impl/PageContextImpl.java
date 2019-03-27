/*
 * Copyright (c) 2019 ETH Zürich, Educational Development and Technology (LET)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.gui.service.page.impl;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.BooleanUtils;
import org.eclipse.rap.rwt.widgets.DialogCallback;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.MessageBox;
import org.eclipse.swt.widgets.Shell;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ch.ethz.seb.sebserver.gbl.api.APIMessage;
import ch.ethz.seb.sebserver.gbl.api.APIMessageError;
import ch.ethz.seb.sebserver.gbl.api.EntityType;
import ch.ethz.seb.sebserver.gbl.model.EntityKey;
import ch.ethz.seb.sebserver.gbl.util.Utils;
import ch.ethz.seb.sebserver.gui.content.action.ActionDefinition;
import ch.ethz.seb.sebserver.gui.service.i18n.I18nSupport;
import ch.ethz.seb.sebserver.gui.service.i18n.LocTextKey;
import ch.ethz.seb.sebserver.gui.service.page.ComposerService;
import ch.ethz.seb.sebserver.gui.service.page.PageContext;
import ch.ethz.seb.sebserver.gui.service.page.PageDefinition;
import ch.ethz.seb.sebserver.gui.service.page.PageMessageException;
import ch.ethz.seb.sebserver.gui.service.page.action.Action;
import ch.ethz.seb.sebserver.gui.service.page.event.PageEvent;
import ch.ethz.seb.sebserver.gui.service.page.event.PageEventListener;
import ch.ethz.seb.sebserver.gui.widget.Message;

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
    public PageContext copy() {
        return copyOf(this.parent);
    }

    @Override
    public PageContext copyOf(final Composite parent) {
        return new PageContextImpl(
                this.i18nSupport,
                this.composerService,
                this.root,
                parent,
                new HashMap<>(this.attributes));
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
    public PageContext withAttribute(final String key, final String value) {
        final Map<String, String> attrs = new HashMap<>();
        attrs.putAll(this.attributes);
        attrs.put(key, value);
        return new PageContextImpl(
                this.i18nSupport,
                this.composerService,
                this.root,
                this.parent,
                attrs);
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
    public boolean isReadonly() {
        return BooleanUtils.toBoolean(getAttribute(AttributeKeys.READ_ONLY, "true"));
    }

    @Override
    public EntityKey getEntityKey() {
        if (hasAttribute(AttributeKeys.ENTITY_ID) && hasAttribute(AttributeKeys.ENTITY_TYPE)) {
            return new EntityKey(
                    getAttribute(AttributeKeys.ENTITY_ID),
                    EntityType.valueOf(getAttribute(AttributeKeys.ENTITY_TYPE)));
        }

        return null;
    }

    @Override
    public EntityKey getParentEntityKey() {
        if (hasAttribute(AttributeKeys.PARENT_ENTITY_ID) && hasAttribute(AttributeKeys.PARENT_ENTITY_TYPE)) {
            return new EntityKey(
                    getAttribute(AttributeKeys.PARENT_ENTITY_ID),
                    EntityType.valueOf(getAttribute(AttributeKeys.PARENT_ENTITY_TYPE)));
        }

        return null;
    }

    @Override
    public PageContext withEntityKey(final EntityKey entityKey) {
        if (entityKey == null) {
            return removeAttribute(AttributeKeys.ENTITY_ID)
                    .removeAttribute(AttributeKeys.ENTITY_TYPE);
        }
        return withAttribute(AttributeKeys.ENTITY_ID, entityKey.modelId)
                .withAttribute(AttributeKeys.ENTITY_TYPE, entityKey.entityType.name());
    }

    @Override
    public PageContext withParentEntityKey(final EntityKey entityKey) {
        if (entityKey == null) {
            return removeAttribute(AttributeKeys.PARENT_ENTITY_ID)
                    .removeAttribute(AttributeKeys.PARENT_ENTITY_TYPE);
        }
        return withAttribute(AttributeKeys.PARENT_ENTITY_ID, entityKey.modelId)
                .withAttribute(AttributeKeys.PARENT_ENTITY_TYPE, entityKey.entityType.name());
    }

    @Override
    public PageContext clearEntityKeys() {
        return withEntityKey(null)
                .withParentEntityKey(null);
    }

    @Override
    public boolean hasAttribute(final String name) {
        return this.attributes.containsKey(name);
    }

    @Override
    public PageContext removeAttribute(final String name) {
        final Map<String, String> attrs = new HashMap<>();
        attrs.putAll(this.attributes);
        attrs.remove(name);
        return new PageContextImpl(
                this.i18nSupport,
                this.composerService,
                this.root,
                this.parent,
                attrs);
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T extends PageEvent> void firePageEvent(final T event) {
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
    public Action createAction(final ActionDefinition actionDefinition) {
        return new Action(actionDefinition, this);
    }

    @Override
    public void applyConfirmDialog(final LocTextKey confirmMessage, final Runnable onOK) {
        final Message messageBox = new Message(
                this.root.getShell(),
                this.i18nSupport.getText("sebserver.dialog.confirm.title"),
                this.i18nSupport.getText(confirmMessage),
                SWT.OK | SWT.CANCEL);
        messageBox.setMarkupEnabled(true);
        messageBox.open(new ConfirmDialogCallback(onOK));
    }

    @Override
    public void forwardToPage(final PageDefinition pageDefinition) {

        this.composerService.compose(
                pageDefinition.composer(),
                pageDefinition.applyPageContext(copyOf(this.root)));
    }

    @Override
    public void forwardToMainPage() {
        forwardToPage(this.composerService.mainPage());
    }

    @Override
    public void forwardToLoginPage() {
        forwardToPage(this.composerService.loginPage());
    }

    @Override
    public void publishPageMessage(final LocTextKey title, final LocTextKey message) {
        final MessageBox messageBox = new Message(
                getShell(),
                this.i18nSupport.getText(title),
                this.i18nSupport.getText(message),
                SWT.NONE);
        messageBox.setMarkupEnabled(true);
        messageBox.open(null);
    }

    @Override
    public void publishPageMessage(final PageMessageException pme) {
        final MessageBox messageBox = new Message(
                getShell(),
                this.i18nSupport.getText("sebserver.page.message"),
                this.i18nSupport.getText(pme.getMessageKey()),
                SWT.NONE);
        messageBox.setMarkupEnabled(true);
        messageBox.open(null);
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
                Utils.formatHTMLLines(errorMessage),
                SWT.ERROR);
        messageBox.open(null);
    }

    @Override
    public <T> T notifyError(final Throwable error) {
        notifyError(error.getMessage(), error);
        return null;
    }

    @Override
    public void logout() {
        // just to be sure we leave a clean and proper authorizationContext
        try {
            ((ComposerServiceImpl) this.composerService).authorizationContextHolder
                    .getAuthorizationContext()
                    .logout();
        } catch (final Exception e) {
            log.info("Cleanup logout failed: {}", e.getMessage());
        }

        MainPageState.clear();
        forwardToLoginPage();
    }

    @Override
    public String toString() {
        return "PageContextImpl [root=" + this.root + ", parent=" + this.parent + ", attributes=" + this.attributes
                + "]";
    }

    private static final class ConfirmDialogCallback implements DialogCallback {
        private static final long serialVersionUID = 1491270214433492441L;
        private final Runnable onOK;

        private ConfirmDialogCallback(final Runnable onOK) {
            this.onOK = onOK;
        }

        @Override
        public void dialogClosed(final int returnCode) {
            if (returnCode == SWT.OK) {
                try {
                    this.onOK.run();
                } catch (final Throwable t) {
                    log.error(
                            "Unexpected on confirm callback execution. This should not happen, plase secure the given onOK Runnable",
                            t);
                }
            }
        }
    }

    private static final class ListenerComparator implements Comparator<PageEventListener<?>>, Serializable {

        private static final long serialVersionUID = 2571739214439340404L;

        @Override
        public int compare(final PageEventListener<?> o1, final PageEventListener<?> o2) {
            final int x = o1.priority();
            final int y = o2.priority();
            return (x < y) ? -1 : ((x == y) ? 0 : 1);
        }
    }

}
