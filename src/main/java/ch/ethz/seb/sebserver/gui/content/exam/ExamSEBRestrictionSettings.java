/*
 * Copyright (c) 2020 ETH Zürich, IT Services
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.gui.content.exam;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.StringUtils;
import org.eclipse.swt.widgets.Composite;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import ch.ethz.seb.sebserver.gbl.Constants;
import ch.ethz.seb.sebserver.gbl.api.API;
import ch.ethz.seb.sebserver.gbl.model.EntityKey;
import ch.ethz.seb.sebserver.gbl.model.exam.Chapters;
import ch.ethz.seb.sebserver.gbl.model.exam.MoodleSEBRestriction;
import ch.ethz.seb.sebserver.gbl.model.exam.OpenEdxSEBRestriction;
import ch.ethz.seb.sebserver.gbl.model.exam.SEBRestriction;
import ch.ethz.seb.sebserver.gbl.model.institution.LmsSetup.LmsType;
import ch.ethz.seb.sebserver.gbl.profile.GuiProfile;
import ch.ethz.seb.sebserver.gbl.util.Tuple;
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
import ch.ethz.seb.sebserver.gui.service.remote.webservice.api.exam.ActivateSEBRestriction;
import ch.ethz.seb.sebserver.gui.service.remote.webservice.api.exam.DeactivateSEBRestriction;
import ch.ethz.seb.sebserver.gui.service.remote.webservice.api.exam.GetCourseChapters;
import ch.ethz.seb.sebserver.gui.service.remote.webservice.api.exam.GetSEBRestrictionSettings;
import ch.ethz.seb.sebserver.gui.service.remote.webservice.api.exam.SaveSEBRestriction;
import ch.ethz.seb.sebserver.gui.service.remote.webservice.api.lmssetup.GetLmsSetup;

@Lazy
@Component
@GuiProfile
public class ExamSEBRestrictionSettings {

    private static final Logger log = LoggerFactory.getLogger(ExamSEBRestrictionSettings.class);

    private final static LocTextKey SEB_RESTRICTION_ERROR =
            new LocTextKey("sebserver.error.exam.seb.restriction");
    private final static LocTextKey SEB_RESTRICTION_FORM_TITLE =
            new LocTextKey("sebserver.exam.action.sebrestriction.details");

    private final static LocTextKey SEB_RESTRICTION_FORM_INFO =
            new LocTextKey("sebserver.exam.form.sebrestriction.info");
    private final static LocTextKey SEB_RESTRICTION_FORM_INFO_TEXT =
            new LocTextKey("sebserver.exam.form.sebrestriction.info-text");
    private final static LocTextKey SEB_RESTRICTION_FORM_CONFIG_KEYS =
            new LocTextKey("sebserver.exam.form.sebrestriction.configKeys");
    private final static LocTextKey SEB_RESTRICTION_FORM_BROWSER_KEYS =
            new LocTextKey("sebserver.exam.form.sebrestriction.browserExamKeys");
    private final static LocTextKey SEB_RESTRICTION_FORM_MOODLE_ALT_BEK_KEY =
            new LocTextKey("sebserver.exam.form.sebrestriction.MOODLE_ALT_BEK_KEY");
    private final static LocTextKey SEB_RESTRICTION_FORM_MOODLE_BEK_KEY =
            new LocTextKey("sebserver.exam.form.sebrestriction.MOODLE_BEK_KEY");
    private final static LocTextKey SEB_RESTRICTION_FORM_EDX_WHITE_LIST_PATHS =
            new LocTextKey("sebserver.exam.form.sebrestriction.WHITELIST_PATHS");
    private final static LocTextKey SEB_RESTRICTION_FORM_EDX_PERMISSIONS =
            new LocTextKey("sebserver.exam.form.sebrestriction.PERMISSION_COMPONENTS");
    private final static LocTextKey SEB_RESTRICTION_FORM_EDX_BLACKLIST_CHAPTERS =
            new LocTextKey("sebserver.exam.form.sebrestriction.BLACKLIST_CHAPTERS");
    private final static LocTextKey SEB_RESTRICTION_FORM_EDX_USER_BANNING_ENABLED =
            new LocTextKey("sebserver.exam.form.sebrestriction.USER_BANNING_ENABLED");

    static final String PAGE_CONTEXT_ATTR_LMS_ID = "ATTR_LMS_ID";

    Function<PageAction, PageAction> settingsFunction(final PageService pageService) {

        return action -> {

            final PageContext pageContext = action.pageContext();
            final ModalInputDialog<FormHandle<?>> dialog =
                    new ModalInputDialog<FormHandle<?>>(
                            action.pageContext().getParent().getShell(),
                            pageService.getWidgetFactory())
                                    .setDialogWidth(740)
                                    .setDialogHeight(400);

            final SEBRestrictionPropertiesForm bindFormContext = new SEBRestrictionPropertiesForm(
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

    private boolean doCreate(
            final PageService pageService,
            final PageContext pageContext,
            final FormHandle<?> formHandle) {

        final boolean isReadonly = BooleanUtils.toBoolean(
                pageContext.getAttribute(PageContext.AttributeKeys.FORCE_READ_ONLY));
        if (isReadonly) {
            return true;
        }

        final EntityKey entityKey = pageContext.getEntityKey();
        final LmsType lmsType = getLmsType(pageService, pageContext.getAttribute(PAGE_CONTEXT_ATTR_LMS_ID));
        SEBRestriction bodyValue = null;
        try {
            final Form form = formHandle.getForm();
            final Collection<String> browserKeys = Utils.getListOfLines(
                    form.getFieldValue(SEBRestriction.ATTR_BROWSER_KEYS));

            final Map<String, String> additionalAttributes = new HashMap<>();
            if (lmsType == LmsType.OPEN_EDX) {
                additionalAttributes.put(
                        OpenEdxSEBRestriction.ATTR_PERMISSION_COMPONENTS,
                        form.getFieldValue(OpenEdxSEBRestriction.ATTR_PERMISSION_COMPONENTS));
                additionalAttributes.put(
                        OpenEdxSEBRestriction.ATTR_WHITELIST_PATHS,
                        form.getFieldValue(OpenEdxSEBRestriction.ATTR_WHITELIST_PATHS));
                additionalAttributes.put(
                        OpenEdxSEBRestriction.ATTR_USER_BANNING_ENABLED,
                        form.getFieldValue(OpenEdxSEBRestriction.ATTR_USER_BANNING_ENABLED));
                additionalAttributes.put(
                        OpenEdxSEBRestriction.ATTR_BLACKLIST_CHAPTERS,
                        Utils.convertCarriageReturnToListSeparator(
                                form.getFieldValue(OpenEdxSEBRestriction.ATTR_BLACKLIST_CHAPTERS)));
            }

            bodyValue = new SEBRestriction(
                    Long.parseLong(entityKey.modelId),
                    null,
                    browserKeys,
                    additionalAttributes,
                    null);

        } catch (final Exception e) {
            log.error("Unexpected error while trying to get settings from form: ", e);
        }

        if (bodyValue == null) {
            return false;
        }

        return !pageService
                .getRestService()
                .getBuilder(SaveSEBRestriction.class)
                .withURIVariable(API.PARAM_MODEL_ID, entityKey.modelId)
                .withBody(bodyValue)
                .call()
                .onError(formHandle::handleError)
                .hasError();
    }

    private final class SEBRestrictionPropertiesForm
            implements ModalInputDialogComposer<FormHandle<?>> {

        private final PageService pageService;
        private final PageContext pageContext;

        protected SEBRestrictionPropertiesForm(
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
            final LmsType lmsType = getLmsType(
                    this.pageService,
                    this.pageContext.getAttribute(PAGE_CONTEXT_ATTR_LMS_ID));
            final boolean isReadonly = BooleanUtils.toBoolean(
                    this.pageContext.getAttribute(PageContext.AttributeKeys.FORCE_READ_ONLY));

            final Composite content = this.pageService
                    .getWidgetFactory()
                    .createPopupScrollComposite(parent);

            final SEBRestriction sebRestriction = restService
                    .getBuilder(GetSEBRestrictionSettings.class)
                    .withURIVariable(API.PARAM_MODEL_ID, entityKey.modelId)
                    .call()
                    .getOrThrow();

            final Chapters chapters = (lmsType == LmsType.OPEN_EDX)
                    ? restService.getBuilder(GetCourseChapters.class)
                            .withURIVariable(API.PARAM_MODEL_ID, entityKey.modelId)
                            .call()
                            .onError(t -> t.printStackTrace())
                            .getOr(null)
                    : null;

            final PageContext formContext = this.pageContext
                    .copyOf(content)
                    .clearEntityKeys();

            final FormHandle<SEBRestriction> formHandle = this.pageService.formBuilder(
                    formContext)
                    .withDefaultSpanInput(6)
                    .withEmptyCellSeparation(false)
                    .readonly(isReadonly)

                    .addField(FormBuilder.text(
                            "Info",
                            SEB_RESTRICTION_FORM_INFO,
                            this.pageService.getI18nSupport().getText(SEB_RESTRICTION_FORM_INFO_TEXT))
                            .asArea(50)
                            .asHTML()
                            .readonly(true))

                    .addField(FormBuilder.text(
                            SEBRestriction.ATTR_CONFIG_KEYS,
                            SEB_RESTRICTION_FORM_CONFIG_KEYS,
                            StringUtils.join(sebRestriction.getConfigKeys(), Constants.CARRIAGE_RETURN))
                            .asArea(50)
                            .readonly(true))

                    .addFieldIf(
                            () -> lmsType == LmsType.MOODLE_PLUGIN,
                            () -> FormBuilder.text(
                                    MoodleSEBRestriction.ATTR_ALT_BEK,
                                    SEB_RESTRICTION_FORM_MOODLE_ALT_BEK_KEY,
                                    sebRestriction
                                            .getAdditionalProperties()
                                            .get(MoodleSEBRestriction.ATTR_ALT_BEK))
                                    .readonly(true))

                    .addField(FormBuilder.text(
                            SEBRestriction.ATTR_BROWSER_KEYS,
                            (lmsType == LmsType.MOODLE_PLUGIN)
                                    ? SEB_RESTRICTION_FORM_MOODLE_BEK_KEY
                                    : SEB_RESTRICTION_FORM_BROWSER_KEYS,
                            StringUtils.join(sebRestriction.getBrowserExamKeys(), Constants.CARRIAGE_RETURN))
                            .asArea())

                    .addFieldIf(
                            () -> lmsType == LmsType.OPEN_EDX,
                            () -> FormBuilder.multiCheckboxSelection(
                                    OpenEdxSEBRestriction.ATTR_WHITELIST_PATHS,
                                    SEB_RESTRICTION_FORM_EDX_WHITE_LIST_PATHS,
                                    sebRestriction.getAdditionalProperties()
                                            .get(OpenEdxSEBRestriction.ATTR_WHITELIST_PATHS),
                                    resourceService::sebRestrictionWhiteListResources))

                    .addFieldIf(
                            () -> chapters == null && lmsType == LmsType.OPEN_EDX,
                            () -> FormBuilder.text(
                                    OpenEdxSEBRestriction.ATTR_BLACKLIST_CHAPTERS,
                                    SEB_RESTRICTION_FORM_EDX_BLACKLIST_CHAPTERS,
                                    Utils.convertListSeparatorToCarriageReturn(
                                            sebRestriction
                                                    .getAdditionalProperties()
                                                    .get(OpenEdxSEBRestriction.ATTR_BLACKLIST_CHAPTERS)))
                                    .asArea())

                    .addFieldIf(
                            () -> chapters != null && lmsType == LmsType.OPEN_EDX,
                            () -> FormBuilder.multiCheckboxSelection(
                                    OpenEdxSEBRestriction.ATTR_BLACKLIST_CHAPTERS,
                                    SEB_RESTRICTION_FORM_EDX_BLACKLIST_CHAPTERS,
                                    sebRestriction
                                            .getAdditionalProperties()
                                            .get(OpenEdxSEBRestriction.ATTR_BLACKLIST_CHAPTERS),
                                    () -> chapters.chapter_list
                                            .stream()
                                            .map(chapter -> new Tuple<>(chapter.id(), chapter.name()))
                                            .collect(Collectors.toList())))

                    .addFieldIf(
                            () -> lmsType == LmsType.OPEN_EDX,
                            () -> FormBuilder.multiCheckboxSelection(
                                    OpenEdxSEBRestriction.ATTR_PERMISSION_COMPONENTS,
                                    SEB_RESTRICTION_FORM_EDX_PERMISSIONS,
                                    sebRestriction.getAdditionalProperties()
                                            .get(OpenEdxSEBRestriction.ATTR_PERMISSION_COMPONENTS),
                                    resourceService::sebRestrictionPermissionResources))

                    .addFieldIf(
                            () -> lmsType == LmsType.OPEN_EDX,
                            () -> FormBuilder.checkbox(
                                    OpenEdxSEBRestriction.ATTR_USER_BANNING_ENABLED,
                                    SEB_RESTRICTION_FORM_EDX_USER_BANNING_ENABLED,
                                    sebRestriction
                                            .getAdditionalProperties()
                                            .get(OpenEdxSEBRestriction.ATTR_USER_BANNING_ENABLED)))

                    .build();

            return () -> formHandle;
        }

    }

    private LmsType getLmsType(final PageService pageService, final String lmsSetupId) {
        try {

            return pageService
                    .getRestService()
                    .getBuilder(GetLmsSetup.class)
                    .withURIVariable(API.PARAM_MODEL_ID, lmsSetupId)
                    .call()
                    .getOrThrow().lmsType;

        } catch (final Exception e) {
            return null;
        }
    }

    public PageAction setSEBRestriction(
            final PageAction action,
            final boolean activateRestriction,
            final RestService restService) {

        return setSEBRestriction(
                action,
                activateRestriction,
                restService,
                error -> action.pageContext().notifyError(SEB_RESTRICTION_ERROR, error));
    }

    public PageAction setSEBRestriction(
            final PageAction action,
            final boolean activateRestriction,
            final RestService restService,
            final Consumer<Exception> errorHandler) {

        restService.getBuilder((activateRestriction)
                ? ActivateSEBRestriction.class
                : DeactivateSEBRestriction.class)
                .withURIVariable(
                        API.PARAM_MODEL_ID,
                        action.getEntityKey().modelId)
                .call()
                .onError(errorHandler);

        return action;
    }

}
