/*
 * Copyright (c) 2019 ETH Zürich, Educational Development and Technology (LET)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.gui.content.configs;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.function.Function;

import org.apache.commons.lang3.StringUtils;
import org.eclipse.rap.rwt.RWT;
import org.eclipse.rap.rwt.client.service.UrlLauncher;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.TabFolder;
import org.eclipse.swt.widgets.TabItem;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import ch.ethz.seb.sebserver.gbl.Constants;
import ch.ethz.seb.sebserver.gbl.api.API;
import ch.ethz.seb.sebserver.gbl.api.APIMessage;
import ch.ethz.seb.sebserver.gbl.api.APIMessageError;
import ch.ethz.seb.sebserver.gbl.api.EntityType;
import ch.ethz.seb.sebserver.gbl.model.EntityKey;
import ch.ethz.seb.sebserver.gbl.model.sebconfig.Configuration;
import ch.ethz.seb.sebserver.gbl.model.sebconfig.ConfigurationNode;
import ch.ethz.seb.sebserver.gbl.model.sebconfig.ConfigurationNode.ConfigurationStatus;
import ch.ethz.seb.sebserver.gbl.model.sebconfig.View;
import ch.ethz.seb.sebserver.gbl.profile.GuiProfile;
import ch.ethz.seb.sebserver.gbl.util.Utils;
import ch.ethz.seb.sebserver.gui.content.action.ActionDefinition;
import ch.ethz.seb.sebserver.gui.service.examconfig.ExamConfigurationService;
import ch.ethz.seb.sebserver.gui.service.examconfig.impl.AttributeMapping;
import ch.ethz.seb.sebserver.gui.service.examconfig.impl.ViewContext;
import ch.ethz.seb.sebserver.gui.service.i18n.LocTextKey;
import ch.ethz.seb.sebserver.gui.service.page.PageContext;
import ch.ethz.seb.sebserver.gui.service.page.PageMessageException;
import ch.ethz.seb.sebserver.gui.service.page.PageService;
import ch.ethz.seb.sebserver.gui.service.page.TemplateComposer;
import ch.ethz.seb.sebserver.gui.service.remote.download.DownloadService;
import ch.ethz.seb.sebserver.gui.service.remote.download.SEBExamSettingsDownload;
import ch.ethz.seb.sebserver.gui.service.remote.webservice.api.RestService;
import ch.ethz.seb.sebserver.gui.service.remote.webservice.api.seb.examconfig.GetConfigurations;
import ch.ethz.seb.sebserver.gui.service.remote.webservice.api.seb.examconfig.GetExamConfigNode;
import ch.ethz.seb.sebserver.gui.service.remote.webservice.api.seb.examconfig.GetSettingsPublished;
import ch.ethz.seb.sebserver.gui.service.remote.webservice.api.seb.examconfig.SEBExamConfigUndo;
import ch.ethz.seb.sebserver.gui.service.remote.webservice.api.seb.examconfig.SaveExamConfigHistory;
import ch.ethz.seb.sebserver.gui.service.remote.webservice.auth.CurrentUser;
import ch.ethz.seb.sebserver.gui.service.remote.webservice.auth.CurrentUser.GrantCheck;
import ch.ethz.seb.sebserver.gui.widget.WidgetFactory;
import ch.ethz.seb.sebserver.gui.widget.WidgetFactory.CustomVariant;

@Lazy
@Component
@GuiProfile
public class SEBSettingsForm implements TemplateComposer {

    private static final Logger log = LoggerFactory.getLogger(SEBSettingsForm.class);

    public static final String ATTR_VIEW_INDEX = "VIEW_INDEX";

    private static final String VIEW_TEXT_KEY_PREFIX =
            "sebserver.examconfig.props.form.views.";
    private static final String KEY_SAVE_TO_HISTORY_SUCCESS =
            "sebserver.examconfig.action.saveToHistory.success";
    private static final String KEY_UNDO_SUCCESS =
            "sebserver.examconfig.action.undo.success";
    private static final LocTextKey TITLE_TEXT_KEY =
            new LocTextKey("sebserver.examconfig.props.from.title");
    private static final LocTextKey UNPUBLISHED_MESSAGE_KEY =
            new LocTextKey("sebserver.examconfig.props.from.unpublished.message");

    private static final LocTextKey MESSAGE_SAVE_INTEGRITY_VIOLATION =
            new LocTextKey("sebserver.examconfig.action.saveToHistory.integrity-violation");

    private final PageService pageService;
    private final RestService restService;
    private final CurrentUser currentUser;
    private final ExamConfigurationService examConfigurationService;
    private final SEBExamConfigImportPopup sebExamConfigImportPopup;
    private final DownloadService downloadService;
    private final String downloadFileName;

    protected SEBSettingsForm(
            final PageService pageService,
            final ExamConfigurationService examConfigurationService,
            final SEBExamConfigImportPopup sebExamConfigImportPopup,
            final DownloadService downloadService,
            @Value("${sebserver.gui.seb.exam.config.download.filename}") final String downloadFileName) {

        this.pageService = pageService;
        this.restService = pageService.getRestService();
        this.currentUser = pageService.getCurrentUser();
        this.examConfigurationService = examConfigurationService;
        this.sebExamConfigImportPopup = sebExamConfigImportPopup;
        this.downloadService = downloadService;
        this.downloadFileName = downloadFileName;
    }

    @Override
    public void compose(final PageContext pageContext) {
        final WidgetFactory widgetFactory = this.pageService.getWidgetFactory();

        final EntityKey entityKey = pageContext.getEntityKey();

        final ConfigurationNode configNode = this.restService.getBuilder(GetExamConfigNode.class)
                .withURIVariable(API.PARAM_MODEL_ID, entityKey.modelId)
                .call()
                .onError(error -> pageContext.notifyLoadError(EntityType.CONFIGURATION_NODE, error))
                .getOrThrow();

        final boolean readonly = pageContext.isReadonly() || configNode.status == ConfigurationStatus.IN_USE;
        final Composite warningPanelAnchor = new Composite(pageContext.getParent(), SWT.NONE);
        final GridData gridData = new GridData(SWT.FILL, SWT.FILL, true, false);
        warningPanelAnchor.setLayoutData(gridData);
        final GridLayout gridLayout = new GridLayout(1, true);
        gridLayout.marginHeight = 0;
        gridLayout.marginWidth = 0;
        warningPanelAnchor.setLayout(gridLayout);
        final PublishedMessagePanelViewCallback publishedMessagePanelViewCallback =
                new PublishedMessagePanelViewCallback(
                        this.pageService,
                        warningPanelAnchor,
                        entityKey.modelId);

        final Composite content = widgetFactory.defaultPageLayout(
                pageContext.getParent(),
                new LocTextKey(TITLE_TEXT_KEY.name, Utils.truncateText(configNode.name, 30)));

        try {

            final Configuration configuration = this.restService.getBuilder(GetConfigurations.class)
                    .withQueryParam(Configuration.FILTER_ATTR_CONFIGURATION_NODE_ID, configNode.getModelId())
                    .withQueryParam(Configuration.FILTER_ATTR_FOLLOWUP, Constants.TRUE_STRING)
                    .call()
                    .map(Utils::toSingleton)
                    .onError(error -> pageContext.notifyLoadError(EntityType.CONFIGURATION, error))
                    .getOrThrow();

            final AttributeMapping attributes = this.examConfigurationService
                    .getAttributes(configNode.templateId)
                    .onError(error -> pageContext.notifyLoadError(EntityType.CONFIGURATION_ATTRIBUTE, error))
                    .getOrThrow();

            final List<View> views = this.examConfigurationService.getViews(attributes);
            final TabFolder tabFolder = widgetFactory.tabFolderLocalized(content);
            tabFolder.setLayoutData(new GridData(SWT.FILL, SWT.TOP, true, false));

            final List<ViewContext> viewContexts = new ArrayList<>();
            final Function<String, ViewContext> viewContextSupplier = viewName -> viewContexts
                    .stream()
                    .filter(v -> viewName.equals(v.getName()))
                    .findFirst()
                    .orElse(null);
            for (final View view : views) {
                final ViewContext viewContext = this.examConfigurationService.createViewContext(
                        pageContext,
                        configuration,
                        view,
                        viewContextSupplier,
                        attributes,
                        20,
                        readonly,
                        publishedMessagePanelViewCallback);
                viewContexts.add(viewContext);

                final Composite viewGrid = this.examConfigurationService.createViewGrid(
                        tabFolder,
                        viewContext);

                final TabItem tabItem = widgetFactory.tabItemLocalized(
                        tabFolder,
                        new LocTextKey(VIEW_TEXT_KEY_PREFIX + view.name));
                tabItem.setControl(viewGrid);
            }

            // set selection if available
            final String viewIndex = pageContext.getAttribute(ATTR_VIEW_INDEX);
            if (StringUtils.isNotBlank(viewIndex)) {
                try {
                    tabFolder.setSelection(Integer.parseInt(viewIndex));
                } catch (final NumberFormatException e) {
                    log.warn("Failed to initialize view selection");
                }
            }

            this.examConfigurationService.initInputFieldValues(configuration.id, viewContexts);

            final UrlLauncher urlLauncher = RWT.getClient().getService(UrlLauncher.class);
            final GrantCheck examConfigGrant = this.currentUser.grantCheck(EntityType.CONFIGURATION_NODE);
            this.pageService.pageActionBuilder(pageContext.clearEntityKeys())

                    .newAction(ActionDefinition.SEB_EXAM_CONFIG_SAVE_TO_HISTORY)
                    .withEntityKey(entityKey)
                    .withExec(action -> {
                        this.restService.getBuilder(SaveExamConfigHistory.class)
                                .withURIVariable(API.PARAM_MODEL_ID, configuration.getModelId())
                                .call()
                                .onError(t -> notifyErrorOnSave(t, pageContext));
                        return action.withAttribute(
                                ATTR_VIEW_INDEX,
                                String.valueOf(tabFolder.getSelectionIndex()));
                    })
                    .withSuccess(KEY_SAVE_TO_HISTORY_SUCCESS)
                    .ignoreMoveAwayFromEdit()
                    .publishIf(() -> examConfigGrant.iw() && !readonly)

                    .newAction(ActionDefinition.SEB_EXAM_CONFIG_UNDO)
                    .withEntityKey(entityKey)
                    .withExec(action -> {
                        this.restService.getBuilder(SEBExamConfigUndo.class)
                                .withURIVariable(API.PARAM_MODEL_ID, configuration.getModelId())
                                .call()
                                .getOrThrow();
                        return action.withAttribute(
                                ATTR_VIEW_INDEX,
                                String.valueOf(tabFolder.getSelectionIndex()));
                    })
                    .withSuccess(KEY_UNDO_SUCCESS)
                    .ignoreMoveAwayFromEdit()
                    .publishIf(() -> examConfigGrant.iw() && !readonly)

                    .newAction(ActionDefinition.SEB_EXAM_CONFIG_EXPORT_PLAIN_XML)
                    .withEntityKey(entityKey)
                    .withExec(action -> {
                        final String downloadURL = this.downloadService.createDownloadURL(
                                entityKey.modelId,
                                SEBExamSettingsDownload.class,
                                this.downloadFileName);
                        urlLauncher.openURL(downloadURL);
                        return action;
                    })
                    .noEventPropagation()
                    .publishIf(() -> examConfigGrant.im())

                    .newAction(ActionDefinition.SEB_EXAM_CONFIG_IMPORT_TO_EXISTING_CONFIG)
                    .withEntityKey(entityKey)
                    .withExec(this.sebExamConfigImportPopup.importFunction(
                            () -> String.valueOf(tabFolder.getSelectionIndex())))
                    .noEventPropagation()
                    .publishIf(() -> examConfigGrant.iw() && !readonly)

                    .newAction(ActionDefinition.SEB_EXAM_CONFIG_VIEW_PROP)
                    .withEntityKey(entityKey)
                    .ignoreMoveAwayFromEdit()
                    .publish();

            publishedMessagePanelViewCallback.activate();
            publishedMessagePanelViewCallback.run();

        } catch (final RuntimeException e) {
            pageContext.notifyUnexpectedError(e);
            throw e;
        } catch (final Exception e) {
            log.error("Unexpected error while trying to fetch exam configuration data and create views", e);
            pageContext.notifyError(SEBExamConfigForm.FORM_TITLE, e);
        }
    }

    private static class PublishedMessagePanelViewCallback implements Runnable {

        private final PageService pageService;
        private final Composite parent;
        private final String nodeId;

        private boolean active = false;

        public PublishedMessagePanelViewCallback(
                final PageService pageService,
                final Composite parent,
                final String nodeId) {

            this.pageService = pageService;
            this.parent = parent;
            this.nodeId = nodeId;
        }

        public void activate() {
            this.active = true;
        }

        @Override
        public void run() {
            if (!this.active) {
                return;
            }

            final boolean settingsPublished = this.pageService
                    .getRestService()
                    .getBuilder(GetSettingsPublished.class)
                    .withURIVariable(API.PARAM_MODEL_ID, this.nodeId)
                    .call()
                    .onError(error -> log.warn("Failed to verify published settings. Cause: ", error.getMessage()))
                    .map(result -> result.settingsPublished)
                    .getOr(false);

            if (!settingsPublished) {
                if (this.parent.getChildren() != null && this.parent.getChildren().length == 0) {
                    final WidgetFactory widgetFactory = this.pageService.getWidgetFactory();
                    final Composite warningPanel = widgetFactory.createWarningPanel(this.parent);
                    widgetFactory.labelLocalized(
                            warningPanel,
                            CustomVariant.MESSAGE,
                            UNPUBLISHED_MESSAGE_KEY);
                }
            } else if (this.parent.getChildren() != null && this.parent.getChildren().length > 0) {
                this.parent.getChildren()[0].dispose();
            }
            this.parent.getParent().layout();
        }
    }

    public void notifyErrorOnSave(final Exception error, final PageContext context) {
        if (error instanceof APIMessageError) {
            try {
                final Collection<APIMessage> errorMessages = ((APIMessageError) error).getAPIMessages();
                final APIMessage apiMessage = errorMessages.iterator().next();
                if (APIMessage.ErrorMessage.INTEGRITY_VALIDATION.isOf(apiMessage)) {
                    throw new PageMessageException(MESSAGE_SAVE_INTEGRITY_VIOLATION);
                } else {
                    throw error;
                }
            } catch (final PageMessageException e) {
                throw e;
            } catch (final Exception e) {
                throw new RuntimeException(error);
            }
        }
    }
}
