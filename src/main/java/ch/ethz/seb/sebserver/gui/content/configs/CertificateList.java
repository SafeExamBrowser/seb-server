/*
 * Copyright (c) 2021 ETH Zürich, Educational Development and Technology (LET)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.gui.content.configs;

import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.eclipse.swt.widgets.Composite;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import ch.ethz.seb.sebserver.gbl.Constants;
import ch.ethz.seb.sebserver.gbl.api.API;
import ch.ethz.seb.sebserver.gbl.api.APIMessage;
import ch.ethz.seb.sebserver.gbl.api.EntityType;
import ch.ethz.seb.sebserver.gbl.model.EntityKey;
import ch.ethz.seb.sebserver.gbl.model.sebconfig.CertificateInfo;
import ch.ethz.seb.sebserver.gbl.profile.GuiProfile;
import ch.ethz.seb.sebserver.gui.content.action.ActionDefinition;
import ch.ethz.seb.sebserver.gui.service.i18n.I18nSupport;
import ch.ethz.seb.sebserver.gui.service.i18n.LocTextKey;
import ch.ethz.seb.sebserver.gui.service.page.PageContext;
import ch.ethz.seb.sebserver.gui.service.page.PageService;
import ch.ethz.seb.sebserver.gui.service.page.TemplateComposer;
import ch.ethz.seb.sebserver.gui.service.page.impl.PageAction;
import ch.ethz.seb.sebserver.gui.service.remote.webservice.api.RestService;
import ch.ethz.seb.sebserver.gui.service.remote.webservice.api.seb.cert.GetCertificatePage;
import ch.ethz.seb.sebserver.gui.service.remote.webservice.api.seb.cert.RemoveCertificate;
import ch.ethz.seb.sebserver.gui.service.remote.webservice.auth.CurrentUser;
import ch.ethz.seb.sebserver.gui.service.remote.webservice.auth.CurrentUser.GrantCheck;
import ch.ethz.seb.sebserver.gui.table.ColumnDefinition;
import ch.ethz.seb.sebserver.gui.table.ColumnDefinition.TableFilterAttribute;
import ch.ethz.seb.sebserver.gui.table.EntityTable;
import ch.ethz.seb.sebserver.gui.table.TableFilter.CriteriaType;

@Lazy
@Component
@GuiProfile
public class CertificateList implements TemplateComposer {

    static final LocTextKey EMPTY_LIST_TEXT_KEY =
            new LocTextKey("sebserver.certificate.list.empty");
    private static final LocTextKey EMPTY_SELECTION_TEXT_KEY =
            new LocTextKey("sebserver.certificate.info.pleaseSelect");
    static final LocTextKey TITLE_TEXT_KEY =
            new LocTextKey("sebserver.certificate.list.title");
    static final LocTextKey ALIAS_TEXT_KEY =
            new LocTextKey("sebserver.certificate.list.column.alias");
    static final LocTextKey VALID_FROM_KEY =
            new LocTextKey("sebserver.certificate.list.column.validFrom");
    static final LocTextKey VALID_TO_KEY =
            new LocTextKey("sebserver.certificate.list.column.validTo");
    static final LocTextKey TYPE_TEXT_KEY =
            new LocTextKey("sebserver.certificate.list.column.type");
    static final LocTextKey FORM_IMPORT_SELECT_TEXT_KEY =
            new LocTextKey("sebserver.certificate.action.import-file-select");
    static final LocTextKey FORM_IMPORT_NO_SELECT_TEXT_KEY =
            new LocTextKey("sebserver.certificate.action.import-file-select.no");
    static final LocTextKey FORM_ALIAS_TEXT_KEY =
            new LocTextKey("sebserver.certificate.form.alias");
    static final LocTextKey FORM_IMPORT_PASSWORD_TEXT_KEY =
            new LocTextKey("sebserver.certificate.action.import-file-password");
    static final LocTextKey FORM_IMPORT_ERRPR_TITLE =
            new LocTextKey("sebserver.error.unexpected");
    static final LocTextKey FORM_IMPORT_ERROR_FILE_SELECTION =
            new LocTextKey("sebserver.certificate.message.error.file");
    static final LocTextKey FORM_IMPORT_CONFIRM_TEXT_KEY =
            new LocTextKey("sebserver.certificate.action.import-config.confirm");
    static final LocTextKey FORM_ACTION_MESSAGE_IN_USE_TEXT_KEY =
            new LocTextKey("sebserver.certificate.action.remove.in-use");
    static final LocTextKey FORM_ACTION_MESSAGE_REMOVE_CONFIRM_TEXT_KEY =
            new LocTextKey("sebserver.certificate.action.remove.confirm");

    private final TableFilterAttribute aliasFilter = new TableFilterAttribute(
            CriteriaType.TEXT,
            CertificateInfo.FILTER_ATTR_ALIAS);

    private final PageService pageService;
    private final RestService restService;
    private final CurrentUser currentUser;
    private final CertificateImportPopup certificateImportPopup;
    private final int pageSize;

    protected CertificateList(
            final PageService pageService,
            final CertificateImportPopup certificateImportPopup,
            @Value("${sebserver.gui.list.page.size:20}") final Integer pageSize) {

        this.pageService = pageService;
        this.restService = pageService.getRestService();
        this.currentUser = pageService.getCurrentUser();
        this.certificateImportPopup = certificateImportPopup;
        this.pageSize = pageSize;
    }

    @Override
    public void compose(final PageContext pageContext) {

        final GrantCheck grantCheck = this.currentUser.grantCheck(EntityType.CERTIFICATE);

        final Composite content = this.pageService
                .getWidgetFactory()
                .defaultPageLayout(
                        pageContext.getParent(),
                        TITLE_TEXT_KEY);

        // table
        final EntityTable<CertificateInfo> table =
                this.pageService.entityTableBuilder(this.restService.getRestCall(GetCertificatePage.class))
                        .withMultiSelection()
                        .withEmptyMessage(EMPTY_LIST_TEXT_KEY)
                        .withPaging(this.pageSize)
                        .withDefaultSort(CertificateInfo.ATTR_ALIAS)

                        .withColumn(new ColumnDefinition<>(
                                CertificateInfo.ATTR_ALIAS,
                                ALIAS_TEXT_KEY,
                                CertificateInfo::getAlias)
                                        .sortable()
                                        .withFilter(this.aliasFilter))

                        .withColumn(new ColumnDefinition<>(
                                CertificateInfo.ATTR_VALIDITY_FROM,
                                VALID_FROM_KEY,
                                CertificateInfo::getValidityFrom)
                                        .sortable())

                        .withColumn(new ColumnDefinition<>(
                                CertificateInfo.ATTR_VALIDITY_TO,
                                VALID_TO_KEY,
                                CertificateInfo::getValidityTo)
                                        .sortable())

                        .withColumn(new ColumnDefinition<>(
                                CertificateInfo.ATTR_CERT_TYPE,
                                TYPE_TEXT_KEY,
                                this::getTypeInfo))

                        .withSelectionListener(this.pageService.getSelectionPublisher(
                                pageContext,
                                ActionDefinition.SEB_CERTIFICATE_REMOVE))

                        .compose(pageContext.copyOf(content));

        this.pageService.pageActionBuilder(pageContext.clearEntityKeys())

                .newAction(ActionDefinition.SEB_CERTIFICATE_IMPORT)
                .withExec(this.certificateImportPopup.importFunction())
                .noEventPropagation()
                .publishIf(() -> grantCheck.iw())

                .newAction(ActionDefinition.SEB_CERTIFICATE_REMOVE)
                .withConfirm(() -> FORM_ACTION_MESSAGE_REMOVE_CONFIRM_TEXT_KEY)
                .withSelect(
                        table::getSelection,
                        this::removeCertificate,
                        EMPTY_SELECTION_TEXT_KEY)
                .publishIf(() -> grantCheck.iw(), false);
    }

    private PageAction removeCertificate(final PageAction action) {
        final String ids = StringUtils.join(
                action.getMultiSelection().stream()
                        .map(EntityKey::getModelId)
                        .collect(Collectors.toList()),
                Constants.LIST_SEPARATOR);

        this.restService.getBuilder(RemoveCertificate.class)
                .withFormParam(API.CERTIFICATE_ALIAS, ids)
                .call()
                .onError(error -> {
                    if (APIMessage.checkError(error, APIMessage.ErrorMessage.INTEGRITY_VALIDATION)) {
                        action.pageContext().publishInfo(FORM_ACTION_MESSAGE_IN_USE_TEXT_KEY);
                    } else {
                        action.pageContext().notifyRemoveError(EntityType.CERTIFICATE, error);
                    }
                });

        return action;
    }

    private String getTypeInfo(final CertificateInfo certificateInfo) {
        final I18nSupport i18nSupport = this.pageService.getI18nSupport();

        return StringUtils.join(
                certificateInfo.types.stream()
                        .map(type -> new LocTextKey("sebserver.certificate.list.column.type." + type.name()))
                        .map(i18nSupport::getText)
                        .collect(Collectors.toList()),
                " | ");
    }

}
