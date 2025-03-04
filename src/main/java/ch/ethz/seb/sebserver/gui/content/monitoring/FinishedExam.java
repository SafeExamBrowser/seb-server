/*
 * Copyright (c) 2022 ETH Zürich, IT Services
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.gui.content.monitoring;

import static ch.ethz.seb.sebserver.gbl.model.user.UserFeatures.Feature.MONITORING_RUNNING_EXAM_SCREEN_PROCTORING;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.function.BooleanSupplier;

import ch.ethz.seb.sebserver.gbl.model.exam.ScreenProctoringSettings;
import ch.ethz.seb.sebserver.gui.service.remote.webservice.api.exam.GetScreenProctoringSettings;
import ch.ethz.seb.sebserver.gui.service.remote.webservice.api.session.GetScreenProctoringGroups;
import ch.ethz.seb.sebserver.gui.service.session.proctoring.MonitoringProctoringService;
import org.apache.commons.lang3.BooleanUtils;
import org.eclipse.rap.rwt.RWT;
import org.eclipse.rap.rwt.client.service.UrlLauncher;
import org.eclipse.swt.widgets.Composite;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import ch.ethz.seb.sebserver.gbl.Constants;
import ch.ethz.seb.sebserver.gbl.api.API;
import ch.ethz.seb.sebserver.gbl.model.Domain;
import ch.ethz.seb.sebserver.gbl.model.EntityKey;
import ch.ethz.seb.sebserver.gbl.model.exam.Exam;
import ch.ethz.seb.sebserver.gbl.model.exam.Indicator;
import ch.ethz.seb.sebserver.gbl.model.exam.Indicator.IndicatorType;
import ch.ethz.seb.sebserver.gbl.model.session.ClientConnection;
import ch.ethz.seb.sebserver.gbl.model.session.ClientConnectionData;
import ch.ethz.seb.sebserver.gbl.model.session.ClientEvent.ExportType;
import ch.ethz.seb.sebserver.gbl.model.session.ExtendedClientEvent;
import ch.ethz.seb.sebserver.gbl.model.user.UserInfo;
import ch.ethz.seb.sebserver.gbl.model.user.UserRole;
import ch.ethz.seb.sebserver.gbl.profile.GuiProfile;
import ch.ethz.seb.sebserver.gbl.util.Utils;
import ch.ethz.seb.sebserver.gui.content.action.ActionDefinition;
import ch.ethz.seb.sebserver.gui.service.i18n.I18nSupport;
import ch.ethz.seb.sebserver.gui.service.i18n.LocTextKey;
import ch.ethz.seb.sebserver.gui.service.page.PageContext;
import ch.ethz.seb.sebserver.gui.service.page.PageService;
import ch.ethz.seb.sebserver.gui.service.page.PageService.PageActionBuilder;
import ch.ethz.seb.sebserver.gui.service.page.TemplateComposer;
import ch.ethz.seb.sebserver.gui.service.page.impl.PageAction;
import ch.ethz.seb.sebserver.gui.service.push.ServerPushService;
import ch.ethz.seb.sebserver.gui.service.remote.download.DownloadService;
import ch.ethz.seb.sebserver.gui.service.remote.download.SEBClientLogExport;
import ch.ethz.seb.sebserver.gui.service.remote.webservice.api.RestService;
import ch.ethz.seb.sebserver.gui.service.remote.webservice.api.exam.GetExam;
import ch.ethz.seb.sebserver.gui.service.remote.webservice.api.exam.indicator.GetIndicators;
import ch.ethz.seb.sebserver.gui.service.remote.webservice.api.session.GetFinishedExamClientConnectionPage;
import ch.ethz.seb.sebserver.gui.service.remote.webservice.auth.CurrentUser;
import ch.ethz.seb.sebserver.gui.table.ColumnDefinition;
import ch.ethz.seb.sebserver.gui.table.ColumnDefinition.TableFilterAttribute;
import ch.ethz.seb.sebserver.gui.table.EntityTable;
import ch.ethz.seb.sebserver.gui.table.TableBuilder;
import ch.ethz.seb.sebserver.gui.table.TableFilter.CriteriaType;

@Lazy
@Component
@GuiProfile
public class FinishedExam implements TemplateComposer {

    private static final Logger log = LoggerFactory.getLogger(FinishedExam.class);

    private static final LocTextKey EMPTY_SELECTION_TEXT_KEY =
            new LocTextKey("sebserver.finished.exam.connection.emptySelection");
    private static final LocTextKey TITLE_TEXT_KEY =
            new LocTextKey("sebserver.finished.exam.connections.title");
    private static final LocTextKey EMPTY_LIST_TEXT_KEY =
            new LocTextKey("sebserver.finished.exam.connections.empty");
    private static final LocTextKey TABLE_COLUMN_NAME =
            new LocTextKey("sebserver.finished.exam.connections.name");
    private static final LocTextKey TABLE_COLUMN_INFO =
            new LocTextKey("sebserver.finished.exam.connections.info");
    private static final LocTextKey TABLE_COLUMN_STATUS =
            new LocTextKey("sebserver.finished.exam.connections.status");

    private final TableFilterAttribute nameFilter = new TableFilterAttribute(
            CriteriaType.TEXT,
            ClientConnection.FILTER_ATTR_SESSION_ID,
            Utils.createFilterTooltipKey(TABLE_COLUMN_NAME));
    private final TableFilterAttribute infoFilter = new TableFilterAttribute(
            CriteriaType.TEXT,
            ClientConnection.ATTR_INFO,
            Utils.createFilterTooltipKey(TABLE_COLUMN_INFO));
    private final TableFilterAttribute statusFilter;

    private final PageService pageService;
    private final RestService restService;
    private final I18nSupport i18nSupport;
    private final DownloadService downloadService;
    private final MonitoringProctoringService monitoringProctoringService;
    private final String exportFileName;
    private final int pageSize;

    public FinishedExam(
            final ServerPushService serverPushService,
            final PageService pageService,
            final DownloadService downloadService,
            final MonitoringProctoringService monitoringProctoringService,
            @Value("${sebserver.gui.seb.client.logs.export.filename:SEBClientLogs}") final String exportFileName,
            @Value("${sebserver.gui.list.page.size:20}") final Integer pageSize) {

        this.pageService = pageService;
        this.restService = pageService.getRestService();
        this.downloadService = downloadService;
        this.monitoringProctoringService = monitoringProctoringService;
        this.exportFileName = exportFileName;
        this.pageSize = pageSize;

        this.i18nSupport = pageService.getI18nSupport();

        this.statusFilter = new TableFilterAttribute(
                CriteriaType.SINGLE_SELECTION,
                ClientConnection.FILTER_ATTR_STATUS,
                pageService.getResourceService()::localizedClientConnectionStatusResources,
                Utils.createFilterTooltipKey(TABLE_COLUMN_STATUS));
    }

    @Override
    public void compose(final PageContext pageContext) {
        final EntityKey examKey = pageContext.getEntityKey();
        final CurrentUser currentUser = this.pageService.getResourceService().getCurrentUser();
        final UserInfo user = currentUser.get();

        final RestService restService = this.pageService
                .getRestService();
        final PageActionBuilder actionBuilder = this.pageService
                .pageActionBuilder(pageContext.clearEntityKeys());
        final Collection<Indicator> indicators = restService
                .getBuilder(GetIndicators.class)
                .withQueryParam(Indicator.FILTER_ATTR_EXAM_ID, examKey.modelId)
                .call()
                .getOrThrow();
        final Exam exam = this.restService.getBuilder(GetExam.class)
                .withURIVariable(API.PARAM_MODEL_ID, examKey.modelId)
                .call()
                .getOrThrow();
        final boolean supporting = user.hasRole(UserRole.EXAM_SUPPORTER) &&
                exam.supporter.contains(user.uuid);
        final BooleanSupplier isExamSupporter = () -> supporting || user.hasRole(UserRole.EXAM_ADMIN);

        final Composite content = this.pageService.getWidgetFactory().defaultPageLayout(
                pageContext.getParent(),
                new LocTextKey(TITLE_TEXT_KEY.name, exam.getName()));

        final TableBuilder<ClientConnectionData> tableBuilder =
                this.pageService.entityTableBuilder(restService.getRestCall(GetFinishedExamClientConnectionPage.class))
                        .withEmptyMessage(EMPTY_LIST_TEXT_KEY)
                        .withPaging(this.pageSize)
                        .withDefaultSort(Domain.CLIENT_CONNECTION.ATTR_EXAM_USER_SESSION_ID)
                        .withStaticFilter(ClientConnection.FILTER_ATTR_EXAM_ID, examKey.modelId)

                        .withColumn(new ColumnDefinition<ClientConnectionData>(
                                Domain.CLIENT_CONNECTION.ATTR_EXAM_USER_SESSION_ID,
                                TABLE_COLUMN_NAME,
                                c -> c.clientConnection.getUserSessionId())
                                        .withFilter(this.nameFilter)
                                        .sortable())

                        .withColumn(new ColumnDefinition<ClientConnectionData>(
                                ClientConnection.ATTR_INFO,
                                TABLE_COLUMN_INFO,
                                c -> c.clientConnection.getInfo())
                                        .withFilter(this.infoFilter))

                        .withColumn(new ColumnDefinition<ClientConnectionData>(
                                Domain.CLIENT_CONNECTION.ATTR_STATUS,
                                TABLE_COLUMN_STATUS,
                                row -> this.pageService.getResourceService()
                                        .localizedClientConnectionStatusName(row.clientConnection.getStatus()))
                                                .withFilter(this.statusFilter)
                                                .sortable())

                        .withDefaultAction(t -> actionBuilder
                                .newAction(ActionDefinition.VIEW_FINISHED_EXAM_CLIENT_CONNECTION)
                                .withParentEntityKey(examKey)
                                .create())
                        .withSelectionListener(this.pageService.getSelectionPublisher(
                                pageContext,
                                ActionDefinition.VIEW_FINISHED_EXAM_CLIENT_CONNECTION));

        indicators.forEach(indicator -> {
            if (indicator.type == IndicatorType.LAST_PING || indicator.type == IndicatorType.NONE) {
                return;
            }
            tableBuilder.withColumn(new ColumnDefinition<ClientConnectionData>(
                    ClientConnectionData.ATTR_INDICATOR_VALUE + Constants.UNDERLINE + indicator.id,
                    new LocTextKey(indicator.name),
                    cc -> cc.getIndicatorDisplayValue(indicator))
                            .sortable());
        });

        final EntityTable<ClientConnectionData> table = tableBuilder.compose(pageContext.copyOf(content));

        actionBuilder

                .newAction(ActionDefinition.VIEW_FINISHED_EXAM_CLIENT_CONNECTION)
                .withParentEntityKey(examKey)
                .withSelect(
                        table::getMultiSelection,
                        PageAction::applySingleSelectionAsEntityKey,
                        EMPTY_SELECTION_TEXT_KEY)
                .publishIf(isExamSupporter, false)

                .newAction(ActionDefinition.FINISHED_EXAM_EXPORT_CSV)
                .withEntityKey(examKey)
                .withExec(this::exportCSV)
                .noEventPropagation()
                .publish();

        // screen proctoring link
        final ScreenProctoringSettings screenProctoringSettings = restService
                .getBuilder(GetScreenProctoringSettings.class)
                .withURIVariable(API.PARAM_MODEL_ID, exam.getModelId())
                .call()
                .getOrThrow();
        final boolean screenProctoringEnabled =
                currentUser.isFeatureEnabled(MONITORING_RUNNING_EXAM_SCREEN_PROCTORING)
                && BooleanUtils.toBoolean(screenProctoringSettings.enableScreenProctoring);
        if (screenProctoringEnabled) {
            this.pageService
                    .getRestService()
                    .getBuilder(GetScreenProctoringGroups.class)
                    .withURIVariable(API.PARAM_MODEL_ID, exam.getModelId())
                    .call()
                    .onError(error -> log.error("Failed to get screen proctoring group data:", error))
                    .getOr(Collections.emptyList())
                    .forEach(group -> {
                        actionBuilder
                                .newAction(ActionDefinition.MONITOR_EXAM_VIEW_SCREEN_PROCTOR_GROUP)
                                .withEntityKey(exam.getEntityKey())
                                .withExec(_action -> monitoringProctoringService.openScreenProctoringTab(
                                        screenProctoringSettings,
                                        group.uuid,
                                        _action))
                                .withNameAttributes(group.name, group.size)
                                .noEventPropagation()
                                .publish();
                    });
        }
    }

    private PageAction exportCSV(final PageAction action) {
        try {

            final UrlLauncher urlLauncher = RWT.getClient().getService(UrlLauncher.class);
            final String fileName = this.exportFileName
                    + Constants.UNDERLINE
                    + this.i18nSupport.formatDisplayDate(Utils.getMillisecondsNow())
                            .replace(" ", "_")
                            .replace(".", "_")
                    + Constants.FILE_EXT_CSV;

            final Map<String, String> queryAttrs = new HashMap<>();
            queryAttrs.put(API.SEB_CLIENT_EVENT_EXPORT_TYPE, ExportType.CSV.name());
            queryAttrs.put(ExtendedClientEvent.FILTER_ATTRIBUTE_EXAM, action.getEntityKey().modelId);
            queryAttrs.put(API.SEB_CLIENT_EVENT_EXPORT_INCLUDE_CONNECTIONS, Constants.TRUE_STRING);
            queryAttrs.put(API.SEB_CLIENT_EVENT_EXPORT_INCLUDE_EXAMS, Constants.TRUE_STRING);

            final String downloadURL = this.downloadService
                    .createDownloadURL(
                            SEBClientLogExport.class,
                            fileName,
                            queryAttrs);

            urlLauncher.openURL(downloadURL);
        } catch (final Exception e) {
            log.error("Failed open export log download: ", e);
        }

        return action;
    }

}
