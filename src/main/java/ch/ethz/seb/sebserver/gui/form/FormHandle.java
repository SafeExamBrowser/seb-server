/*
 * Copyright (c) 2018 ETH Zürich, Educational Development and Technology (LET)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.gui.form;

import java.util.function.Consumer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ch.ethz.seb.sebserver.gbl.model.Entity;
import ch.ethz.seb.sebserver.gbl.util.Result;
import ch.ethz.seb.sebserver.gui.content.action.ActionDefinition;
import ch.ethz.seb.sebserver.gui.form.Form.FormFieldAccessor;
import ch.ethz.seb.sebserver.gui.service.i18n.I18nSupport;
import ch.ethz.seb.sebserver.gui.service.i18n.LocTextKey;
import ch.ethz.seb.sebserver.gui.service.page.FieldValidationError;
import ch.ethz.seb.sebserver.gui.service.page.PageContext;
import ch.ethz.seb.sebserver.gui.service.page.PageContext.AttributeKeys;
import ch.ethz.seb.sebserver.gui.service.page.action.Action;
import ch.ethz.seb.sebserver.gui.service.page.event.ActionEvent;
import ch.ethz.seb.sebserver.gui.service.remote.webservice.api.RestCall;
import ch.ethz.seb.sebserver.gui.service.remote.webservice.api.RestCallError;

public class FormHandle<T extends Entity> {

    private static final Logger log = LoggerFactory.getLogger(FormHandle.class);

    public static final String FIELD_VALIDATION_LOCTEXT_PREFIX = "sebserver.form.validation.fieldError.";

    private final PageContext pageContext;
    private final Form form;
    private final RestCall<T> post;
    private final I18nSupport i18nSupport;

    FormHandle(
            final PageContext pageContext,
            final Form form,
            final RestCall<T> post,
            final I18nSupport i18nSupport) {

        this.pageContext = pageContext;
        this.form = form;
        this.post = post;
        this.i18nSupport = i18nSupport;
    }

    public final Action postChanges(final Action action) {
        return doAPIPost(action.definition)
                .getOrThrow();
    }

    public Result<Action> doAPIPost(final ActionDefinition actionDefinition) {
        this.form.process(
                name -> true,
                fieldAccessor -> fieldAccessor.resetError());

        return this.post
                .newBuilder()
                .withFormBinding(this.form)
                .call()
                .map(result -> {
                    final Action action = this.pageContext.createAction(actionDefinition)
                            .withAttribute(AttributeKeys.READ_ONLY, "true")
                            .withEntity(result.getEntityKey());
                    this.pageContext.publishPageEvent(new ActionEvent(action, false));
                    return action;
                })
                .onErrorDo(this::handleError)
        //.map(this.postPostHandle)
        ;
    }

    private void handleError(final Throwable error) {
        if (error instanceof RestCallError) {
            ((RestCallError) error)
                    .getErrorMessages()
                    .stream()
                    .map(FieldValidationError::new)
                    .forEach(fve -> this.form.process(
                            name -> name.equals(fve.fieldName),
                            fieldAccessor -> showValidationError(fieldAccessor, fve)));
        } else {
            log.error("Unexpected error while trying to post form: ", error);
            this.pageContext.notifyError(error);
        }
    }

    private final void showValidationError(
            final FormFieldAccessor fieldAccessor,
            final FieldValidationError valError) {

        fieldAccessor.setError(this.i18nSupport.getText(new LocTextKey(
                FIELD_VALIDATION_LOCTEXT_PREFIX + valError.errorType,
                (Object[]) valError.attributes)));
    }

    public FormHandle<T> process(final Consumer<Form> consumer) {
        consumer.accept(this.form);
        return this;
    }

}
