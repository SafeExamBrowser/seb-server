/*
 * Copyright (c) 2020 ETH Zürich, Educational Development and Technology (LET)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.gui.content;

import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;

import org.apache.commons.lang3.StringUtils;
import org.eclipse.swt.widgets.Composite;

import ch.ethz.seb.sebserver.gbl.api.API;
import ch.ethz.seb.sebserver.gbl.api.JSONMapper;
import ch.ethz.seb.sebserver.gbl.model.EntityKey;
import ch.ethz.seb.sebserver.gbl.model.exam.OpenEdxSebRestriction;
import ch.ethz.seb.sebserver.gbl.model.exam.SebRestriction;
import ch.ethz.seb.sebserver.gbl.model.institution.LmsSetup.LmsType;
import ch.ethz.seb.sebserver.gbl.util.Utils;
import ch.ethz.seb.sebserver.gui.content.action.ActionDefinition;
import ch.ethz.seb.sebserver.gui.form.FormBuilder;
import ch.ethz.seb.sebserver.gui.form.FormHandle;
import ch.ethz.seb.sebserver.gui.service.ResourceService;
import ch.ethz.seb.sebserver.gui.service.i18n.LocTextKey;
import ch.ethz.seb.sebserver.gui.service.page.ModalInputDialogComposer;
import ch.ethz.seb.sebserver.gui.service.page.PageContext;
import ch.ethz.seb.sebserver.gui.service.page.PageService;
import ch.ethz.seb.sebserver.gui.service.page.impl.ModalInputDialog;
import ch.ethz.seb.sebserver.gui.service.page.impl.PageAction;
import ch.ethz.seb.sebserver.gui.service.remote.webservice.api.RestService;
import ch.ethz.seb.sebserver.gui.service.remote.webservice.api.exam.ActivateSebRestriction;
import ch.ethz.seb.sebserver.gui.service.remote.webservice.api.exam.DeactivateSebRestriction;
import ch.ethz.seb.sebserver.gui.service.remote.webservice.api.exam.GetSebRestriction;
import ch.ethz.seb.sebserver.gui.service.remote.webservice.api.exam.SaveSebRestriction;

public class ExamSebRestrictionSettings {

    private final static LocTextKey SEB_RESTRICTION_ERROR =
            new LocTextKey("sebserver.error.exam.seb.restriction");
    private final static LocTextKey SEB_RESTRICTION_FORM_TITLE =
            new LocTextKey("sebserver.exam.action.sebrestriction.details");

    private final static LocTextKey SEB_RESTRICTION_FORM_CONFIG_KEYS =
            new LocTextKey("sebserver.exam.form.sebrestriction.configKeys");
    private final static LocTextKey SEB_RESTRICTION_FORM_BROWSER_KEYS =
            new LocTextKey("sebserver.exam.form.sebrestriction.browserExamKeys");
    private final static LocTextKey SEB_RESTRICTION_FORM_EDX_WHITE_LIST_PATHS =
            new LocTextKey("sebserver.exam.form.sebrestriction.WHITELIST_PATHS");
    private final static LocTextKey SEB_RESTRICTION_FORM_EDX_PERMISSIONS =
            new LocTextKey("sebserver.exam.form.sebrestriction.PERMISSION_COMPONENTS");

    static final String PAGE_CONTEXT_ATTR_LMS_TYPE = "ATTR_LMS_TYPE";

    static Function<PageAction, PageAction> settingsFunction(final PageService pageService) {

        return action -> {

            final PageContext pageContext = action.pageContext();
            final ModalInputDialog<FormHandle<?>> dialog =
                    new ModalInputDialog<FormHandle<?>>(
                            action.pageContext().getParent().getShell(),
                            pageService.getWidgetFactory())
                                    .setVeryLargeDialogWidth();

            final SebRestrictionPropertiesForm bindFormContext = new SebRestrictionPropertiesForm(
                    pageService,
                    action.pageContext());

            final Predicate<FormHandle<?>> doBind = formHandle -> doCreate(
                    pageService,
                    pageContext,
                    formHandle);

            dialog.open(
                    SEB_RESTRICTION_FORM_TITLE,
                    doBind,
                    Utils.EMPTY_EXECUTION,
                    bindFormContext);

            return action;
        };
    }

    private static final boolean doCreate(
            final PageService pageService,
            final PageContext pageContext,
            final FormHandle<?> formHandle) {

        final EntityKey entityKey = pageContext.getEntityKey();
        final LmsType lmsType = getLmsType(pageContext);
        SebRestriction bodyValue = null;
        try {
            final JSONMapper jsonMapper = pageService.getJSONMapper();
            if (lmsType == LmsType.OPEN_EDX) {
                final OpenEdxSebRestriction edxProperties = jsonMapper.readValue(
                        formHandle.getFormBinding().getFormAsJson(),
                        OpenEdxSebRestriction.class);
                bodyValue = SebRestriction.from(Long.parseLong(entityKey.modelId), edxProperties);
            } else {
                bodyValue = jsonMapper.readValue(
                        formHandle.getFormBinding().getFormAsJson(),
                        SebRestriction.class);
            }
        } catch (final Exception e) {

        }

        return !pageService
                .getRestService()
                .getBuilder(SaveSebRestriction.class)
                .withURIVariable(API.PARAM_MODEL_ID, entityKey.modelId)
                .withBody(bodyValue)
                //.withFormBinding(formHandle.getFormBinding())
                .call()
                .onError(formHandle::handleError)
                .map(mapping -> {
                    pageService.executePageAction(
                            pageService.pageActionBuilder(pageContext.clearEntityKeys())
                                    .newAction(ActionDefinition.EXAM_VIEW_FROM_LIST)
                                    .withEntityKey(pageContext.getParentEntityKey())
                                    .create());
                    return mapping;
                })
                .hasError();
    }

    private static final class SebRestrictionPropertiesForm
            implements ModalInputDialogComposer<FormHandle<?>> {

        private final PageService pageService;
        private final PageContext pageContext;

        protected SebRestrictionPropertiesForm(
                final PageService pageService,
                final PageContext pageContext) {

            this.pageService = pageService;
            this.pageContext = pageContext;

        }

        @Override
        public Supplier<FormHandle<?>> compose(final Composite parent) {
            final RestService restService = this.pageService.getRestService();
            final ResourceService resourceService = this.pageService.getResourceService();
            final EntityKey entityKey = this.pageContext.getEntityKey();
            final LmsType lmsType = getLmsType(this.pageContext);

            final SebRestriction sebRestriction = restService
                    .getBuilder(GetSebRestriction.class)
                    .withURIVariable(API.PARAM_MODEL_ID, entityKey.modelId)
                    .call()
                    .getOrThrow();

            final PageContext formContext = this.pageContext.clearEntityKeys();
            final FormHandle<SebRestriction> formHandle = this.pageService.formBuilder(
                    formContext.copyOf(parent), 3)
                    .withDefaultSpanEmptyCell(0)
                    .withEmptyCellSeparation(false)
                    .readonly(false)

                    .addField(FormBuilder.text(
                            SebRestriction.ATTR_CONFIG_KEYS,
                            SEB_RESTRICTION_FORM_CONFIG_KEYS,
                            StringUtils.join(sebRestriction.getConfigKeys(), '\n'))
                            .asArea(25)
                            .readonly(true))

                    .addField(FormBuilder.text(
                            SebRestriction.ATTR_BROWSER_KEYS,
                            SEB_RESTRICTION_FORM_BROWSER_KEYS,
                            StringUtils.join(sebRestriction.getBrowserExamKeys(), '\n'))
                            .asArea())

                    .addFieldIf(
                            () -> lmsType == LmsType.OPEN_EDX,
                            () -> FormBuilder.multiSelection(
                                    OpenEdxSebRestriction.ATTR_WHITELIST_PATHS,
                                    SEB_RESTRICTION_FORM_EDX_WHITE_LIST_PATHS,
                                    sebRestriction.getAdditionalProperties()
                                            .get(OpenEdxSebRestriction.ATTR_WHITELIST_PATHS),
                                    () -> resourceService.sebRestrictionWhiteListResources()))

                    .addFieldIf(
                            () -> lmsType == LmsType.OPEN_EDX,
                            () -> FormBuilder.multiSelection(
                                    OpenEdxSebRestriction.ATTR_PERMISSION_COMPONENTS,
                                    SEB_RESTRICTION_FORM_EDX_PERMISSIONS,
                                    sebRestriction.getAdditionalProperties()
                                            .get(OpenEdxSebRestriction.ATTR_PERMISSION_COMPONENTS),
                                    () -> resourceService.sebRestrictionPermissionResources()))

                    .build();

            return () -> formHandle;
        }
    }

    private static LmsType getLmsType(final PageContext pageContext) {
        try {
            return LmsType.valueOf(pageContext.getAttribute(PAGE_CONTEXT_ATTR_LMS_TYPE));
        } catch (final Exception e) {
            return null;
        }
    }

    public static PageAction setSebRestriction(
            final PageAction action,
            final boolean activateRestriction,
            final RestService restService) {

        return setSebRestriction(
                action,
                activateRestriction,
                restService,
                error -> action.pageContext().notifyError(SEB_RESTRICTION_ERROR, error));
    }

    public static PageAction setSebRestriction(
            final PageAction action,
            final boolean activateRestriction,
            final RestService restService,
            final Consumer<Exception> errorHandler) {

        restService.getBuilder((activateRestriction)
                ? ActivateSebRestriction.class
                : DeactivateSebRestriction.class)
                .withURIVariable(
                        API.PARAM_MODEL_ID,
                        action.getEntityKey().modelId)
                .call()
                .onError(errorHandler);

        return action;
    }

}
