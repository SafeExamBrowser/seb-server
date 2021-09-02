/*
 * Copyright (c) 2020 ETH Zürich, Educational Development and Technology (LET)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.gui.content.exam;

import java.util.List;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.eclipse.rap.rwt.RWT;
import org.eclipse.rap.rwt.client.service.UrlLauncher;
import org.eclipse.swt.widgets.Composite;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import ch.ethz.seb.sebserver.gbl.Constants;
import ch.ethz.seb.sebserver.gbl.model.Domain;
import ch.ethz.seb.sebserver.gbl.model.Entity;
import ch.ethz.seb.sebserver.gbl.model.EntityKey;
import ch.ethz.seb.sebserver.gbl.model.sebconfig.ConfigCreationInfo;
import ch.ethz.seb.sebserver.gbl.model.sebconfig.SEBClientConfig;
import ch.ethz.seb.sebserver.gbl.model.sebconfig.SEBClientConfig.ConfigPurpose;
import ch.ethz.seb.sebserver.gbl.profile.GuiProfile;
import ch.ethz.seb.sebserver.gbl.util.Tuple;
import ch.ethz.seb.sebserver.gbl.util.Utils;
import ch.ethz.seb.sebserver.gui.form.FormBuilder;
import ch.ethz.seb.sebserver.gui.form.FormHandle;
import ch.ethz.seb.sebserver.gui.service.i18n.LocTextKey;
import ch.ethz.seb.sebserver.gui.service.page.ModalInputDialogComposer;
import ch.ethz.seb.sebserver.gui.service.page.PageContext;
import ch.ethz.seb.sebserver.gui.service.page.PageService;
import ch.ethz.seb.sebserver.gui.service.page.impl.ModalInputDialog;
import ch.ethz.seb.sebserver.gui.service.page.impl.PageAction;
import ch.ethz.seb.sebserver.gui.service.remote.download.DownloadService;
import ch.ethz.seb.sebserver.gui.service.remote.download.SEBClientConfigDownload;
import ch.ethz.seb.sebserver.gui.service.remote.webservice.api.seb.clientconfig.GetClientConfigs;

@Lazy
@Component
@GuiProfile
public class ExamCreateClientConfigPopup {

    private static final LocTextKey TITLE_KEY = new LocTextKey("sebserver.exam.form.export.config.popup.title");
    private static final LocTextKey CONFIG_NAME_KEY = new LocTextKey("sebserver.exam.form.export.config.name");
    private static final LocTextKey CONFIG_TEXT_KEY = new LocTextKey("sebserver.exam.form.export.config.popup.text");
    private static final LocTextKey NO_CONFIG_TEXT_KEY =
            new LocTextKey("sebserver.exam.form.export.config.popup.noconfig");

    private final PageService pageService;
    private final DownloadService downloadService;

    public ExamCreateClientConfigPopup(
            final PageService pageService,
            final DownloadService downloadService) {

        this.pageService = pageService;
        this.downloadService = downloadService;
    }

    public Function<PageAction, PageAction> exportFunction(
            final Long examInstitutionId,
            final String examName) {

        return action -> {

            final DateTime now = DateTime.now(DateTimeZone.UTC);
            final String downloadFileName = StringUtils.remove(examName, " ") + "_" + now.getYear() + "-"
                    + now.getMonthOfYear() + "-" + now.getDayOfMonth() + ".seb";

            final ModalInputDialog<FormHandle<?>> dialog =
                    new ModalInputDialog<FormHandle<?>>(
                            action.pageContext().getParent().getShell(),
                            this.pageService.getWidgetFactory())
                                    .setLargeDialogWidth();

            final CreationFormContext creationFormContext = new CreationFormContext(
                    this.pageService,
                    action.pageContext(),
                    String.valueOf(examInstitutionId));

            final Predicate<FormHandle<?>> doCreate = formHandle -> doCreate(
                    this.pageService,
                    action.pageContext(),
                    action.getEntityKey(),
                    formHandle,
                    downloadFileName);

            dialog.open(
                    TITLE_KEY,
                    doCreate,
                    Utils.EMPTY_EXECUTION,
                    creationFormContext);

            return action;
        };
    }

    private boolean doCreate(
            final PageService pageService,
            final PageContext pageContext,
            final EntityKey examKey,
            final FormHandle<?> formHandle,
            final String downloadFileName) {

        if (formHandle == null) {
            return true;
        }

        final UrlLauncher urlLauncher = RWT.getClient().getService(UrlLauncher.class);
        final String modelId = formHandle.getForm().getFieldValue(Domain.SEB_CLIENT_CONFIGURATION.ATTR_ID);
        final String downloadURL = this.downloadService.createDownloadURL(
                modelId,
                examKey.modelId,
                SEBClientConfigDownload.class,
                downloadFileName);
        urlLauncher.openURL(downloadURL);

        return true;
    }

    private final class CreationFormContext implements ModalInputDialogComposer<FormHandle<?>> {

        private final PageService pageService;
        private final PageContext pageContext;
        private final String examInstitutionId;

        protected CreationFormContext(
                final PageService pageService,
                final PageContext pageContext,
                final String examInstitutionId) {

            this.pageService = pageService;
            this.pageContext = pageContext;
            this.examInstitutionId = examInstitutionId;
        }

        @Override
        public Supplier<FormHandle<?>> compose(final Composite parent) {

            final List<Tuple<String>> configs = this.pageService.getRestService().getBuilder(GetClientConfigs.class)
                    .withQueryParam(SEBClientConfig.FILTER_ATTR_ACTIVE, Constants.TRUE_STRING)
                    .withQueryParam(Entity.FILTER_ATTR_INSTITUTION, this.examInstitutionId)
                    .call()
                    .getOrThrow()
                    .stream()
                    .filter(config -> config.configPurpose == ConfigPurpose.START_EXAM)
                    .map(config -> new Tuple<>(config.getModelId(), config.name))
                    .collect(Collectors.toList());

            if (configs.isEmpty()) {
                this.pageService
                        .getWidgetFactory()
                        .labelLocalized(parent, NO_CONFIG_TEXT_KEY, true);
                return null;
            } else {

                this.pageService
                        .getWidgetFactory()
                        .labelLocalized(parent, CONFIG_TEXT_KEY, true);

                final FormHandle<ConfigCreationInfo> formHandle = this.pageService.formBuilder(
                        this.pageContext.copyOf(parent))
                        .readonly(false)
                        .addField(FormBuilder.singleSelection(
                                Domain.SEB_CLIENT_CONFIGURATION.ATTR_ID,
                                CONFIG_NAME_KEY,
                                configs.get(0)._1,
                                () -> configs))

                        .build();
                return () -> formHandle;
            }
        }
    }

}
