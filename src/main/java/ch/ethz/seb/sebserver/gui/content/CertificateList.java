/*
 * Copyright (c) 2021 ETH Zürich, Educational Development and Technology (LET)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.gui.content;

import org.eclipse.swt.widgets.Composite;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import ch.ethz.seb.sebserver.gbl.model.sebconfig.CertificateInfo;
import ch.ethz.seb.sebserver.gbl.profile.GuiProfile;
import ch.ethz.seb.sebserver.gui.content.action.ActionDefinition;
import ch.ethz.seb.sebserver.gui.service.i18n.I18nSupport;
import ch.ethz.seb.sebserver.gui.service.i18n.LocTextKey;
import ch.ethz.seb.sebserver.gui.service.page.PageContext;
import ch.ethz.seb.sebserver.gui.service.page.PageService;
import ch.ethz.seb.sebserver.gui.service.page.TemplateComposer;
import ch.ethz.seb.sebserver.gui.service.remote.webservice.api.RestService;
import ch.ethz.seb.sebserver.gui.service.remote.webservice.api.seb.cert.GetCertificatePage;
import ch.ethz.seb.sebserver.gui.service.remote.webservice.auth.CurrentUser;
import ch.ethz.seb.sebserver.gui.table.ColumnDefinition;
import ch.ethz.seb.sebserver.gui.table.ColumnDefinition.TableFilterAttribute;
import ch.ethz.seb.sebserver.gui.table.EntityTable;
import ch.ethz.seb.sebserver.gui.table.TableFilter.CriteriaType;

@Lazy
@Component
@GuiProfile
public class CertificateList implements TemplateComposer {

    private static final LocTextKey EMPTY_LIST_TEXT_KEY =
            new LocTextKey("sebserver.certificate.list.empty");
    private static final LocTextKey TITLE_TEXT_KEY =
            new LocTextKey("sebserver.certificate.list.title");
    private static final LocTextKey ALIAS_TEXT_KEY =
            new LocTextKey("sebserver.certificate.list.column.alias");
    private static final LocTextKey VALID_FROM_KEY =
            new LocTextKey("sebserver.certificate.list.column.validFrom");
    private static final LocTextKey VALID_TO_KEY =
            new LocTextKey("sebserver.certificate.list.column.validTo");
    private static final LocTextKey TYPE_TEXT_KEY =
            new LocTextKey("sebserver.certificate.list.column.type");

    private final TableFilterAttribute aliasFilter = new TableFilterAttribute(
            CriteriaType.TEXT,
            CertificateInfo.FILTER_ATTR_ALIAS);

    private final PageService pageService;
    private final RestService restService;
    private final CurrentUser currentUser;
    private final int pageSize;

    protected CertificateList(
            final PageService pageService,
            @Value("${sebserver.gui.list.page.size:20}") final Integer pageSize) {

        this.pageService = pageService;
        this.restService = pageService.getRestService();
        this.currentUser = pageService.getCurrentUser();
        this.pageSize = pageSize;
    }

    @Override
    public void compose(final PageContext pageContext) {
        final Composite content = this.pageService
                .getWidgetFactory()
                .defaultPageLayout(
                        pageContext.getParent(),
                        TITLE_TEXT_KEY);

        // table
        final EntityTable<CertificateInfo> table =
                this.pageService.entityTableBuilder(this.restService.getRestCall(GetCertificatePage.class))
                        .withEmptyMessage(EMPTY_LIST_TEXT_KEY)
                        .withPaging(this.pageSize)

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
    }

    private String getTypeInfo(final CertificateInfo certificateInfo) {
        final I18nSupport i18nSupport = this.pageService.getI18nSupport();
        //i18nSupport.getText("")

        // TODO

        return "";
    }

}
