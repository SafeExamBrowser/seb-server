/*
 * Copyright (c) 2019 ETH Zürich, Educational Development and Technology (LET)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.gui.service.page.impl;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.StringUtils;
import org.eclipse.rap.rwt.widgets.DialogCallback;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.MessageBox;
import org.eclipse.swt.widgets.Shell;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ch.ethz.seb.sebserver.gbl.Constants;
import ch.ethz.seb.sebserver.gbl.api.APIMessage;
import ch.ethz.seb.sebserver.gbl.api.APIMessageError;
import ch.ethz.seb.sebserver.gbl.api.EntityType;
import ch.ethz.seb.sebserver.gbl.model.EntityKey;
import ch.ethz.seb.sebserver.gbl.util.Utils;
import ch.ethz.seb.sebserver.gui.service.i18n.I18nSupport;
import ch.ethz.seb.sebserver.gui.service.i18n.LocTextKey;
import ch.ethz.seb.sebserver.gui.service.page.ComposerService;
import ch.ethz.seb.sebserver.gui.service.page.PageContext;
import ch.ethz.seb.sebserver.gui.service.page.PageDefinition;
import ch.ethz.seb.sebserver.gui.service.page.PageMessageException;
import ch.ethz.seb.sebserver.gui.widget.Message;

public class PageContextImpl implements PageContext {

    private static final Logger log = LoggerFactory.getLogger(PageContextImpl.class);

    private static final LocTextKey UNEXPECTED_ERROR = new LocTextKey("sebserver.error.unexpected");
    private static final String ENTITY_LIST_TYPE = null;

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
    public I18nSupport getI18nSupport() {
        return this.i18nSupport;
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
        final Map<String, String> attrs = new HashMap<>(this.attributes);
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
        return this.attributes.getOrDefault(name, def);
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
    public List<EntityKey> getEntityKeyList() {
        if (hasAttribute(AttributeKeys.ENTITY_ID_LIST) && hasAttribute(AttributeKeys.ENTITY_LIST_TYPE)) {
            final EntityType type = EntityType.valueOf(getAttribute(ENTITY_LIST_TYPE));
            Arrays.asList(StringUtils.split(getAttribute(AttributeKeys.ENTITY_ID_LIST), Constants.COMMA))
                    .stream()
                    .map(id -> new EntityKey(id, type))
                    .collect(Collectors.toList());
        }

        return Collections.emptyList();
    }

    @Override
    public PageContext withEntityKeys(final List<EntityKey> entityKeys) {
        if (entityKeys == null || entityKeys.isEmpty()) {
            return removeAttribute(AttributeKeys.ENTITY_ID_LIST)
                    .removeAttribute(AttributeKeys.ENTITY_LIST_TYPE);
        }
        final List<String> ids = entityKeys
                .stream()
                .map(EntityKey::getModelId)
                .collect(Collectors.toList());
        final String joinedIds = StringUtils.join(ids, Constants.COMMA);
        return withAttribute(AttributeKeys.ENTITY_ID_LIST, joinedIds)
                .withAttribute(AttributeKeys.ENTITY_LIST_TYPE, entityKeys.get(0).entityType.name());
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
        final Map<String, String> attrs = new HashMap<>(this.attributes);
        attrs.remove(name);
        return new PageContextImpl(
                this.i18nSupport,
                this.composerService,
                this.root,
                this.parent,
                attrs);
    }

    @Override
    public PageContext clearAttributes() {
        return new PageContextImpl(
                this.i18nSupport,
                this.composerService,
                this.root,
                this.parent,
                null);
    }

    @Override
    public void applyConfirmDialog(final LocTextKey confirmMessage, final Consumer<Boolean> callback) {
        final Message messageBox = new Message(
                this.root.getShell(),
                this.i18nSupport.getText("sebserver.dialog.confirm.title"),
                this.i18nSupport.getText(confirmMessage),
                SWT.OK | SWT.CANCEL,
                this.i18nSupport);
        messageBox.setMarkupEnabled(true);
        messageBox.open(new ConfirmDialogCallback(callback));
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
        this.clearAttributes()
                .forwardToPage(this.composerService.loginPage());
    }

    @Override
    public void publishPageMessage(final LocTextKey title, final LocTextKey message) {
        final MessageBox messageBox = new Message(
                getShell(),
                (title != null)
                        ? this.i18nSupport.getText(title)
                        : "",
                this.i18nSupport.getText(message),
                SWT.NONE,
                this.i18nSupport);
        messageBox.setMarkupEnabled(true);
        messageBox.open(null);
    }

    @Override
    public void publishPageMessage(final PageMessageException pme) {
        final MessageBox messageBox = new Message(
                getShell(),
                this.i18nSupport.getText("sebserver.page.message"),
                this.i18nSupport.getText(pme.getMessageKey()),
                SWT.NONE,
                this.i18nSupport);
        messageBox.setMarkupEnabled(true);
        messageBox.open(null);
    }

    @Override
    public void notifyError(final LocTextKey message, final Exception error) {

        if (error == null) {
            final MessageBox messageBox = new Message(
                    getShell(),
                    this.i18nSupport.getText(UNEXPECTED_ERROR),
                    this.i18nSupport.getText(message),
                    SWT.ERROR,
                    this.i18nSupport);
            messageBox.setMarkupEnabled(true);
            messageBox.open(null);
            return;
        }

        log.error("Unexpected GUI error notified: {}", error.getMessage());

        final String errorMessage = message != null
                ? this.i18nSupport.getText(message)
                : error.getMessage();

        if (error instanceof APIMessageError) {
            final Collection<APIMessage> errorMessages = ((APIMessageError) error).getAPIMessages();
            final MessageBox messageBox = new Message(
                    getShell(),
                    this.i18nSupport.getText("sebserver.error.unexpected"),
                    APIMessage.toHTML(errorMessage, errorMessages),
                    SWT.ERROR,
                    this.i18nSupport);
            messageBox.setMarkupEnabled(true);
            messageBox.open(null);
            return;
        }

        final MessageBox messageBox = new Message(
                getShell(),
                this.i18nSupport.getText("sebserver.error.unexpected"),
                Utils.formatHTMLLines(errorMessage + "<br/><br/> Cause: " + error.getMessage()),
                SWT.ERROR,
                this.i18nSupport);
        messageBox.setMarkupEnabled(true);
        messageBox.open(null);
        log.error("Unexpected error on GUI: ", error);
    }

    @Override
    public String toString() {
        return "PageContextImpl [root=" + this.root + ", parent=" + this.parent + ", attributes=" + this.attributes
                + "]";
    }

    private static final class ConfirmDialogCallback implements DialogCallback {
        private static final long serialVersionUID = 1491270214433492441L;
        private final Consumer<Boolean> onOK;

        private ConfirmDialogCallback(final Consumer<Boolean> onOK) {
            this.onOK = onOK;
        }

        @Override
        public void dialogClosed(final int returnCode) {
            if (returnCode == SWT.OK) {
                try {
                    this.onOK.accept(true);
                } catch (final Exception e) {
                    log.error(
                            "Unexpected on confirm callback execution. This should not happen, please secure the given onOK Runnable",
                            e);
                    this.onOK.accept(false);
                }
            } else {
                this.onOK.accept(false);
            }
        }
    }

}
