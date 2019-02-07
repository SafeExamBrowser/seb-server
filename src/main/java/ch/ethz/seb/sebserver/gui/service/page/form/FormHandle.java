/*
 * Copyright (c) 2018 ETH Zürich, Educational Development and Technology (LET)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.gui.service.page.form;

import java.util.function.Consumer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ch.ethz.seb.sebserver.gui.service.i18n.I18nSupport;
import ch.ethz.seb.sebserver.gui.service.i18n.LocTextKey;
import ch.ethz.seb.sebserver.gui.service.page.PageContext;
import ch.ethz.seb.sebserver.gui.service.page.action.ActionDefinition;
import ch.ethz.seb.sebserver.gui.service.page.event.ActionEvent;
import ch.ethz.seb.sebserver.gui.service.page.form.Form.FormFieldAccessor;
import ch.ethz.seb.sebserver.gui.service.page.validation.FieldValidationError;
import ch.ethz.seb.sebserver.gui.service.remote.webservice.api.RestCall;
import ch.ethz.seb.sebserver.gui.service.remote.webservice.api.RestCallError;

public class FormHandle<T> {

    private static final Logger log = LoggerFactory.getLogger(FormHandle.class);

    public static final String FIELD_VALIDATION_LOCTEXT_PREFIX = "org.sebserver.form.validation.fieldError.";

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

    public void doAPIPost(final ActionDefinition action) {
        this.form.process(
                name -> true,
                fieldAccessor -> fieldAccessor.resetError());

        this.post
                .newBuilder()
                .withFormBinding(this.form)
                .call()
                .map(result -> {
                    this.pageContext.publishPageEvent(new ActionEvent(action, result));
                    return result;
                }).onErrorDo(error -> {
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
                });
    }

    private final void showValidationError(
            final FormFieldAccessor<?> fieldAccessor,
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
