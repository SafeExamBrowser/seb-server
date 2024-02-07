package ch.ethz.seb.sebserver.gui.content.configs;

import ch.ethz.seb.sebserver.gbl.Constants;
import ch.ethz.seb.sebserver.gbl.api.API;
import ch.ethz.seb.sebserver.gbl.api.API.BatchActionType;
import ch.ethz.seb.sebserver.gbl.api.EntityType;
import ch.ethz.seb.sebserver.gbl.model.BatchAction;
import ch.ethz.seb.sebserver.gbl.model.Domain;
import ch.ethz.seb.sebserver.gbl.model.EntityKey;
import ch.ethz.seb.sebserver.gbl.model.sebconfig.ConfigurationNode;
import ch.ethz.seb.sebserver.gbl.profile.GuiProfile;
import ch.ethz.seb.sebserver.gui.content.action.ActionDefinition;
import ch.ethz.seb.sebserver.gui.form.FormBuilder;
import ch.ethz.seb.sebserver.gui.form.FormHandle;
import ch.ethz.seb.sebserver.gui.service.ResourceService;
import ch.ethz.seb.sebserver.gui.service.i18n.LocTextKey;
import ch.ethz.seb.sebserver.gui.service.page.AbstractBatchActionWizard;
import ch.ethz.seb.sebserver.gui.service.page.PageContext;
import ch.ethz.seb.sebserver.gui.service.page.PageService;
import ch.ethz.seb.sebserver.gui.service.push.ServerPushService;
import ch.ethz.seb.sebserver.gui.service.remote.webservice.api.RestCall;
import ch.ethz.seb.sebserver.gui.service.remote.webservice.api.RestService;
import ch.ethz.seb.sebserver.gui.service.remote.webservice.api.seb.examconfig.GetExamConfigNodesByIds;
import ch.ethz.seb.sebserver.gui.table.ColumnDefinition;
import org.apache.tomcat.util.buf.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.function.Supplier;
import java.util.stream.Collectors;

@Lazy
@Component
@GuiProfile
public class SEBExamConfigBatchDeletePopup extends AbstractBatchActionWizard {

    private final static LocTextKey FORM_TITLE = new LocTextKey("sebserver.examconfig.list.batch.delete.title");
    private final static LocTextKey FORM_INFO = new LocTextKey("sebserver.examconfig.list.batch.delete.info");
    private final static LocTextKey ACTION_DO_DELETE = new LocTextKey("sebserver.examconfig.list.batch.action.delete.button");

    protected SEBExamConfigBatchDeletePopup(
            final PageService pageService,
            final ServerPushService serverPushService) {

        super(pageService, serverPushService);
    }

    @Override
    protected LocTextKey getTitle() {
        return FORM_TITLE;
    }

    @Override
    protected LocTextKey getBatchActionInfo() {
        return FORM_INFO;
    }

    @Override
    protected LocTextKey getBatchActionTitle() {
        return ACTION_DO_DELETE;
    }

    @Override
    protected BatchActionType getBatchActionType() {
        return BatchActionType.EXAM_CONFIG_DELETE;
    }

    @Override
    protected Supplier<PageContext> createResultPageSupplier(PageContext pageContext, FormHandle<ConfigurationNode> formHandle) {
        return () -> pageContext;
    }

    @Override
    protected void extendBatchActionRequest(PageContext pageContext, RestCall<BatchAction>.RestCallBuilder batchActionRequestBuilder) {
        // Nothing to do here
    }

    @Override
    protected FormBuilder buildSpecificFormFields(PageContext formContext, FormBuilder formHead, boolean readonly) {
        return formHead;
    }

    @Override
    protected void processUpdateListAction(PageContext formContext) {
        this.pageService.executePageAction(this.pageService.pageActionBuilder(formContext)
                .newAction(ActionDefinition.SEB_EXAM_CONFIG_LIST)
                .create());
    }

    @Override
    protected void applySelectionList(
            final PageContext formContext,
            final Set<EntityKey> multiSelection) {

        final ResourceService resourceService = this.pageService.getResourceService();

        final String ids = StringUtils.join(
                multiSelection.stream().map(EntityKey::getModelId).collect(Collectors.toList()),
                Constants.LIST_SEPARATOR_CHAR);

        final RestService restService = this.pageService.getRestService();
        final List<ConfigurationNode> selected = new ArrayList<>(restService.getBuilder(GetExamConfigNodesByIds.class)
                .withQueryParam(API.PARAM_MODEL_ID_LIST, ids)
                .call()
                .getOr(Collections.emptyList()));

        selected.sort((examConfig1, examConfig2) -> examConfig1.name.compareTo(examConfig2.name));

        this.pageService.staticListTableBuilder(selected, EntityType.CONFIGURATION_NODE)
                .withPaging(10)
                .withColumn(new ColumnDefinition<>(
                        Domain.CONFIGURATION_NODE.ATTR_NAME,
                        SEBExamConfigList.NAME_TEXT_KEY,
                        ConfigurationNode::getName))

                .withColumn(new ColumnDefinition<>(
                        Domain.CONFIGURATION_NODE.ATTR_STATUS,
                        SEBExamConfigList.STATUS_TEXT_KEY,
                        resourceService::localizedExamConfigStatusName)
                )

                .compose(formContext);
    }
}