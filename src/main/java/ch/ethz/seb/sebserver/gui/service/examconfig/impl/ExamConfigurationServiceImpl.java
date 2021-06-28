/*
 * Copyright (c) 2019 ETH Zürich, Educational Development and Technology (LET)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.gui.service.examconfig.impl;

import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.apache.tomcat.util.buf.StringUtils;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

import ch.ethz.seb.sebserver.gbl.Constants;
import ch.ethz.seb.sebserver.gbl.api.API;
import ch.ethz.seb.sebserver.gbl.api.APIMessage;
import ch.ethz.seb.sebserver.gbl.api.APIMessage.ErrorMessage;
import ch.ethz.seb.sebserver.gbl.api.EntityType;
import ch.ethz.seb.sebserver.gbl.api.JSONMapper;
import ch.ethz.seb.sebserver.gbl.model.EntityKey;
import ch.ethz.seb.sebserver.gbl.model.sebconfig.Configuration;
import ch.ethz.seb.sebserver.gbl.model.sebconfig.ConfigurationAttribute;
import ch.ethz.seb.sebserver.gbl.model.sebconfig.ConfigurationTableValues;
import ch.ethz.seb.sebserver.gbl.model.sebconfig.ConfigurationValue;
import ch.ethz.seb.sebserver.gbl.model.sebconfig.Orientation;
import ch.ethz.seb.sebserver.gbl.model.sebconfig.TemplateAttribute;
import ch.ethz.seb.sebserver.gbl.model.sebconfig.View;
import ch.ethz.seb.sebserver.gbl.profile.GuiProfile;
import ch.ethz.seb.sebserver.gbl.util.Result;
import ch.ethz.seb.sebserver.gbl.util.Utils;
import ch.ethz.seb.sebserver.gui.service.examconfig.ExamConfigurationService;
import ch.ethz.seb.sebserver.gui.service.examconfig.InputFieldBuilder;
import ch.ethz.seb.sebserver.gui.service.examconfig.ValueChangeListener;
import ch.ethz.seb.sebserver.gui.service.examconfig.ValueChangeRule;
import ch.ethz.seb.sebserver.gui.service.i18n.LocTextKey;
import ch.ethz.seb.sebserver.gui.service.page.FieldValidationError;
import ch.ethz.seb.sebserver.gui.service.page.PageContext;
import ch.ethz.seb.sebserver.gui.service.page.impl.PageAction;
import ch.ethz.seb.sebserver.gui.service.remote.webservice.api.RestCall;
import ch.ethz.seb.sebserver.gui.service.remote.webservice.api.RestCallError;
import ch.ethz.seb.sebserver.gui.service.remote.webservice.api.RestService;
import ch.ethz.seb.sebserver.gui.service.remote.webservice.api.seb.examconfig.AttachDefaultOrientation;
import ch.ethz.seb.sebserver.gui.service.remote.webservice.api.seb.examconfig.GetConfigAttributes;
import ch.ethz.seb.sebserver.gui.service.remote.webservice.api.seb.examconfig.GetConfigurationValues;
import ch.ethz.seb.sebserver.gui.service.remote.webservice.api.seb.examconfig.GetOrientations;
import ch.ethz.seb.sebserver.gui.service.remote.webservice.api.seb.examconfig.GetViewList;
import ch.ethz.seb.sebserver.gui.service.remote.webservice.api.seb.examconfig.RemoveOrientation;
import ch.ethz.seb.sebserver.gui.service.remote.webservice.api.seb.examconfig.ResetTemplateValues;
import ch.ethz.seb.sebserver.gui.service.remote.webservice.api.seb.examconfig.SaveExamConfigTableValues;
import ch.ethz.seb.sebserver.gui.service.remote.webservice.api.seb.examconfig.SaveExamConfigValue;
import ch.ethz.seb.sebserver.gui.widget.WidgetFactory;

@Lazy
@Service
@GuiProfile
public class ExamConfigurationServiceImpl implements ExamConfigurationService {

    private static final Logger log = LoggerFactory.getLogger(ExamConfigurationServiceImpl.class);

    private final RestService restService;
    private final JSONMapper jsonMapper;
    private final WidgetFactory widgetFactory;

    private final InputFieldBuilderSupplier inputFieldBuilderSupplier;
    private final Collection<ValueChangeRule> valueChangeRules;

    public ExamConfigurationServiceImpl(
            final RestService restService,
            final JSONMapper jsonMapper,
            final WidgetFactory widgetFactory,
            final InputFieldBuilderSupplier inputFieldBuilderSupplier,
            final Collection<ValueChangeRule> valueChangeRules) {

        this.restService = restService;
        this.jsonMapper = jsonMapper;
        this.widgetFactory = widgetFactory;
        this.inputFieldBuilderSupplier = inputFieldBuilderSupplier;
        this.valueChangeRules = Utils.immutableCollectionOf(valueChangeRules);
    }

    @Override
    public WidgetFactory getWidgetFactory() {
        return this.widgetFactory;
    }

    @Override
    public InputFieldBuilder getInputFieldBuilder(
            final ConfigurationAttribute attribute,
            final Orientation orientation) {

        return this.inputFieldBuilderSupplier.getInputFieldBuilder(attribute, orientation);
    }

    @Override
    public Result<AttributeMapping> getAttributes(final Long templateId) {
        return Result.tryCatch(() -> new AttributeMapping(
                templateId,
                getAttributes(),
                getOrientations(templateId)));
    }

    @Override
    public Result<AttributeMapping> getAttributes(
            final TemplateAttribute attribute,
            final Orientation defaultOrientation) {

        final List<Orientation> orientations = getOrientations(attribute.templateId);
        if (attribute.getOrientation() == null) {
            orientations.add(defaultOrientation);
        }

        return Result.tryCatch(() -> new AttributeMapping(
                attribute.templateId,
                getAttributes(),
                orientations));
    }

    private List<Orientation> getOrientations(final Long templateId) {
        return this.restService
                .getBuilder(GetOrientations.class)
                .withQueryParam(Orientation.FILTER_ATTR_TEMPLATE_ID, String.valueOf(templateId))
                .call()
                .onError(t -> log.error("Failed to get all Orientation of template {}", templateId, t))
                .getOrThrow();
    }

    private List<ConfigurationAttribute> getAttributes() {
        return this.restService
                .getBuilder(GetConfigAttributes.class)
                .call()
                .onError(t -> log.error("Failed to get all ConfigurationAttribute"))
                .getOrThrow();
    }

    @Override
    public List<View> getViews(final AttributeMapping allAttributes) {
        final Collection<Long> viewIds = allAttributes.getViewIds();
        if (viewIds == null || viewIds.isEmpty()) {
            return Collections.emptyList();
        }

        final String ids = StringUtils.join(
                viewIds
                        .stream()
                        .map(String::valueOf)
                        .collect(Collectors.toList()),
                Constants.LIST_SEPARATOR_CHAR);

        return this.restService.getBuilder(GetViewList.class)
                .withQueryParam(API.PARAM_MODEL_ID_LIST, ids)
                .call()
                .getOrThrow()
                .stream()
                .sorted(Comparator.comparing(v -> v.position))
                .collect(Collectors.toList());
    }

    @Override
    public ViewContext createViewContext(
            final PageContext pageContext,
            final Configuration configuration,
            final View view,
            final Function<String, ViewContext> viewContextSupplier,
            final AttributeMapping attributeMapping,
            final int rows,
            final boolean readonly,
            final Runnable valueChageCallback) {

        return new ViewContext(
                configuration,
                view,
                viewContextSupplier,
                rows,
                attributeMapping,
                new ValueChangeListenerImpl(
                        pageContext,
                        this.restService,
                        this.jsonMapper,
                        this.valueChangeRules,
                        valueChageCallback),
                this.widgetFactory.getI18nSupport(),
                readonly);

    }

    @Override
    public Composite createViewGrid(final Composite parent, final ViewContext viewContext) {
        final Composite composite = new Composite(parent, SWT.NONE);
        final GridLayout gridLayout = new GridLayout(viewContext.getColumns(), true);
        gridLayout.verticalSpacing = 0;
        composite.setLayout(gridLayout);
        final GridData gridData = new GridData(SWT.FILL, SWT.TOP, true, false);
        composite.setLayoutData(gridData);

        final ViewGridBuilder viewGridBuilder = new ViewGridBuilder(
                composite,
                viewContext,
                this);

        for (final ConfigurationAttribute attribute : viewContext.getAttributes()) {
            final Orientation orientation = viewContext.getOrientation(attribute.id);
            if (orientation != null && viewContext.getId().equals(orientation.viewId)) {
                viewGridBuilder.add(attribute);
            }
        }

        viewGridBuilder.compose();
        return composite;
    }

    @Override
    public void initInputFieldValues(
            final Long configurationId,
            final Collection<ViewContext> viewContexts) {

        if (viewContexts == null || viewContexts.size() < 1) {
            log.warn("No viewContexts available");
            return;
        }

        final Collection<ConfigurationValue> attributeValues = this.restService
                .getBuilder(GetConfigurationValues.class)
                .withQueryParam(
                        ConfigurationValue.FILTER_ATTR_CONFIGURATION_ID,
                        String.valueOf(configurationId))
                .call()
                .onError(t -> log.error(
                        "Failed to get all ConfigurationValue for configuration with id: {}",
                        configurationId))
                .getOrElse(Collections::emptyList);

        viewContexts
                .forEach(vc -> vc.setValuesToInputFields(attributeValues));
    }

    @Override
    public final PageAction resetToDefaults(final PageAction action) {
        final EntityKey parentEntityKey = action.pageContext().getParentEntityKey();
        final Set<EntityKey> selection = action.getMultiSelection();
        if (selection != null && !selection.isEmpty()) {
            selection.forEach(entityKey -> callTemplateAction(
                    ResetTemplateValues.class,
                    parentEntityKey.modelId,
                    entityKey.modelId));
        } else {
            final EntityKey entityKey = action.getEntityKey();
            callTemplateAction(
                    ResetTemplateValues.class,
                    parentEntityKey.modelId,
                    entityKey.modelId);
        }

        return action;
    }

    @Override
    public final PageAction removeFromView(final PageAction action) {
        final EntityKey parentEntityKey = action.pageContext().getParentEntityKey();
        final Set<EntityKey> selection = action.getMultiSelection();
        if (selection != null && !selection.isEmpty()) {
            selection.forEach(entityKey -> callTemplateAction(
                    RemoveOrientation.class,
                    parentEntityKey.modelId,
                    entityKey.modelId));
        } else {
            final EntityKey entityKey = action.getEntityKey();
            callTemplateAction(
                    RemoveOrientation.class,
                    parentEntityKey.modelId,
                    entityKey.modelId);
        }

        return action;
    }

    @Override
    public final PageAction attachToDefaultView(final PageAction action) {
        final EntityKey parentEntityKey = action.pageContext().getParentEntityKey();
        final Set<EntityKey> selection = action.getMultiSelection();
        if (selection != null && !selection.isEmpty()) {
            selection.forEach(entityKey -> callTemplateAction(
                    AttachDefaultOrientation.class,
                    parentEntityKey.modelId,
                    entityKey.modelId));
        } else {
            final EntityKey entityKey = action.getEntityKey();
            callTemplateAction(
                    AttachDefaultOrientation.class,
                    parentEntityKey.modelId,
                    entityKey.modelId);
        }

        return action;
    }

    private void callTemplateAction(
            final Class<? extends RestCall<TemplateAttribute>> actionType,
            final String templateId,
            final String attributeId) {

        this.restService.getBuilder(actionType)
                .withURIVariable(API.PARAM_PARENT_MODEL_ID, templateId)
                .withURIVariable(API.PARAM_MODEL_ID, attributeId)
                .call()
                .getOrThrow();
    }

    private static final class ValueChangeListenerImpl implements ValueChangeListener {

        public static final String VALIDATION_ERROR_KEY_PREFIX = "sebserver.examconfig.props.validation.";

        private final PageContext pageContext;
        private final RestService restService;
        private final JSONMapper jsonMapper;
        private final Collection<ValueChangeRule> valueChangeRules;
        private final Runnable valueChageCallback;

        protected ValueChangeListenerImpl(
                final PageContext pageContext,
                final RestService restService,
                final JSONMapper jsonMapper,
                final Collection<ValueChangeRule> valueChangeRules,
                final Runnable valueChageCallback) {

            this.pageContext = pageContext;
            this.restService = restService;
            this.jsonMapper = jsonMapper;
            this.valueChangeRules = valueChangeRules;
            this.valueChageCallback = valueChageCallback;
        }

        @Override
        public void valueChanged(
                final ViewContext context,
                final ConfigurationAttribute attribute,
                final String value,
                final int listIndex) {

            final ConfigurationValue configurationValue = new ConfigurationValue(
                    null,
                    context.getInstitutionId(),
                    context.getConfigurationId(),
                    attribute.id,
                    listIndex,
                    value);

            try {
                final String jsonValue = this.jsonMapper.writeValueAsString(configurationValue);

                final Result<ConfigurationValue> savedValue = this.restService.getBuilder(SaveExamConfigValue.class)
                        .withBody(jsonValue)
                        .call();

                if (savedValue.hasError()) {
                    context.showError(attribute.id, verifyErrorMessage(savedValue.getError()));
                } else {
                    this.notifyGUI(context, attribute, savedValue.get());
                }

            } catch (final Exception e) {
                this.pageContext.notifySaveError(EntityType.CONFIGURATION_VALUE, e);
            }

            if (this.valueChageCallback != null) {
                this.valueChageCallback.run();
            }
        }

        @Override
        public void tableChanged(final ConfigurationTableValues tableValue) {
            this.restService.getBuilder(SaveExamConfigTableValues.class)
                    .withBody(tableValue)
                    .call();
        }

        private String verifyErrorMessage(final Exception error) {
            if (error instanceof RestCallError) {
                final List<APIMessage> errorMessages = ((RestCallError) error).getAPIMessages();
                if (errorMessages.isEmpty()) {
                    return "";
                }

                final APIMessage apiMessage = errorMessages.get(0);
                if (!ErrorMessage.FIELD_VALIDATION.isOf(apiMessage)) {
                    return "";
                }

                final FieldValidationError fieldValidationError = new FieldValidationError(apiMessage);
                return this.pageContext.getI18nSupport().getText(new LocTextKey(
                        VALIDATION_ERROR_KEY_PREFIX + fieldValidationError.errorType,
                        (Object[]) fieldValidationError.getAttributes()));
            }

            log.warn("Unexpected error happened while trying to set SEB configuration value: ", error);
            return VALIDATION_ERROR_KEY_PREFIX + "unexpected";
        }

        @Override
        public void notifyGUI(
                final ViewContext viewContext,
                final ConfigurationAttribute attribute,
                final ConfigurationValue value) {

            this.valueChangeRules.stream()
                    .filter(rule -> rule.observesAttribute(attribute))
                    .forEach(rule -> rule.applyRule(viewContext, attribute, value));

        }
    }

}
