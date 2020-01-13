/*
 * Copyright (c) 2020 ETH Zürich, Educational Development and Technology (LET)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.gui.content;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;

import org.apache.commons.lang3.StringUtils;
import org.eclipse.swt.widgets.Composite;

import ch.ethz.seb.sebserver.gbl.Constants;
import ch.ethz.seb.sebserver.gbl.api.API;
import ch.ethz.seb.sebserver.gbl.model.EntityKey;
import ch.ethz.seb.sebserver.gbl.model.exam.OpenEdxSebRestriction;
import ch.ethz.seb.sebserver.gbl.model.exam.SebRestriction;
import ch.ethz.seb.sebserver.gbl.model.institution.LmsSetup.LmsType;
import ch.ethz.seb.sebserver.gbl.util.Utils;
import ch.ethz.seb.sebserver.gui.form.Form;
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
    private final static LocTextKey SEB_RESTRICTION_FORM_EDX_BLACKLIST_CHAPTERS =
            new LocTextKey("sebserver.exam.form.sebrestriction.BLACKLIST_CHAPTERS");
    private final static LocTextKey SEB_RESTRICTION_FORM_EDX_USER_BANNING_ENABLED =
            new LocTextKey("sebserver.exam.form.sebrestriction.USER_BANNING_ENABLED");

    static final String PAGE_CONTEXT_ATTR_LMS_TYPE = "ATTR_LMS_TYPE";

    static Function<PageAction, PageAction> settingsFunction(final PageService pageService) {

        return action -> {

            final PageContext pageContext = action.pageContext();
            final ModalInputDialog<FormHandle<?>> dialog =
                    new ModalInputDialog<FormHandle<?>>(
                            action.pageContext().getParent().getShell(),
                            pageService.getWidgetFactory())
                                    .setDialogWidth(740)
                                    .setDialogHeight(400);

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
            final Form form = formHandle.getForm();
            final Collection<String> browserKeys = Utils.getListOfLines(
                    form.getFieldValue(SebRestriction.ATTR_BROWSER_KEYS));

            final Map<String, String> additionalAttributes = new HashMap<>();
            if (lmsType == LmsType.OPEN_EDX) {
                additionalAttributes.put(
                        OpenEdxSebRestriction.ATTR_PERMISSION_COMPONENTS,
                        form.getFieldValue(OpenEdxSebRestriction.ATTR_PERMISSION_COMPONENTS));
                additionalAttributes.put(
                        OpenEdxSebRestriction.ATTR_WHITELIST_PATHS,
                        form.getFieldValue(OpenEdxSebRestriction.ATTR_WHITELIST_PATHS));
                additionalAttributes.put(
                        OpenEdxSebRestriction.ATTR_USER_BANNING_ENABLED,
                        form.getFieldValue(OpenEdxSebRestriction.ATTR_USER_BANNING_ENABLED));
                additionalAttributes.put(
                        OpenEdxSebRestriction.ATTR_BLACKLIST_CHAPTERS,
                        Utils.convertCarriageReturnToListSeparator(
                                form.getFieldValue(OpenEdxSebRestriction.ATTR_BLACKLIST_CHAPTERS)));
            }

            bodyValue = new SebRestriction(
                    Long.parseLong(entityKey.modelId),
                    null,
                    browserKeys,
                    additionalAttributes);

        } catch (final Exception e) {
            e.printStackTrace();
        }

        return !pageService
                .getRestService()
                .getBuilder(SaveSebRestriction.class)
                .withURIVariable(API.PARAM_MODEL_ID, entityKey.modelId)
                .withBody(bodyValue)
                .call()
                .onError(formHandle::handleError)
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

            final Composite content = this.pageService
                    .getWidgetFactory()
                    .createPopupScrollComposite(parent);

            final SebRestriction sebRestriction = restService
                    .getBuilder(GetSebRestriction.class)
                    .withURIVariable(API.PARAM_MODEL_ID, entityKey.modelId)
                    .call()
                    .getOrThrow();

            final PageContext formContext = this.pageContext
                    .copyOf(content)
                    .clearEntityKeys();

            final FormHandle<SebRestriction> formHandle = this.pageService.formBuilder(
                    formContext, 3)
                    .withDefaultSpanEmptyCell(0)
                    .withEmptyCellSeparation(false)
                    .readonly(false)

                    .addField(FormBuilder.text(
                            SebRestriction.ATTR_CONFIG_KEYS,
                            SEB_RESTRICTION_FORM_CONFIG_KEYS,
                            StringUtils.join(sebRestriction.getConfigKeys(), Constants.CARRIAGE_RETURN))
                            .asArea(50)
                            .readonly(true))

                    .addField(FormBuilder.text(
                            SebRestriction.ATTR_BROWSER_KEYS,
                            SEB_RESTRICTION_FORM_BROWSER_KEYS,
                            StringUtils.join(sebRestriction.getBrowserExamKeys(), Constants.CARRIAGE_RETURN))
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

                    .addFieldIf(
                            () -> lmsType == LmsType.OPEN_EDX,
                            () -> FormBuilder.text(
                                    OpenEdxSebRestriction.ATTR_BLACKLIST_CHAPTERS,
                                    SEB_RESTRICTION_FORM_EDX_BLACKLIST_CHAPTERS,
                                    Utils.convertListSeparatorToCarriageReturn(
                                            sebRestriction
                                                    .getAdditionalProperties()
                                                    .get(OpenEdxSebRestriction.ATTR_BLACKLIST_CHAPTERS)))
                                    .asArea())

                    .addFieldIf(
                            () -> lmsType == LmsType.OPEN_EDX,
                            () -> FormBuilder.checkbox(
                                    OpenEdxSebRestriction.ATTR_USER_BANNING_ENABLED,
                                    SEB_RESTRICTION_FORM_EDX_USER_BANNING_ENABLED,
                                    sebRestriction
                                            .getAdditionalProperties()
                                            .get(OpenEdxSebRestriction.ATTR_USER_BANNING_ENABLED)))

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
