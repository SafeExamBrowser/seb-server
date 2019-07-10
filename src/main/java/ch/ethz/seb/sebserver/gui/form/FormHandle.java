/*
 * Copyright (c) 2018 ETH Zürich, Educational Development and Technology (LET)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.gui.form;

import java.util.function.Consumer;
import java.util.function.Predicate;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ch.ethz.seb.sebserver.gbl.api.APIMessage;
import ch.ethz.seb.sebserver.gbl.model.Entity;
import ch.ethz.seb.sebserver.gbl.util.Result;
import ch.ethz.seb.sebserver.gbl.util.Utils;
import ch.ethz.seb.sebserver.gui.form.Form.FormFieldAccessor;
import ch.ethz.seb.sebserver.gui.service.i18n.I18nSupport;
import ch.ethz.seb.sebserver.gui.service.i18n.LocTextKey;
import ch.ethz.seb.sebserver.gui.service.page.FieldValidationError;
import ch.ethz.seb.sebserver.gui.service.page.PageContext;
import ch.ethz.seb.sebserver.gui.service.page.PageService;
import ch.ethz.seb.sebserver.gui.service.page.impl.PageAction;
import ch.ethz.seb.sebserver.gui.service.remote.webservice.api.FormBinding;
import ch.ethz.seb.sebserver.gui.service.remote.webservice.api.RestCall;
import ch.ethz.seb.sebserver.gui.service.remote.webservice.api.RestCallError;

public class FormHandle<T extends Entity> {

    private static final Logger log = LoggerFactory.getLogger(FormHandle.class);

    public static final String FIELD_VALIDATION_LOCTEXT_PREFIX = "sebserver.form.validation.fieldError.";

    private final PageService pageService;
    private final PageContext pageContext;
    private final Form form;
    private final RestCall<T> post;
    private final I18nSupport i18nSupport;

    FormHandle(
            final PageService pageService,
            final PageContext pageContext,
            final Form form,
            final RestCall<T> post) {

        this.pageService = pageService;
        this.pageContext = pageContext;
        this.form = form;
        this.post = post;
        this.i18nSupport = pageService.getI18nSupport();
    }

    public FormBinding getFormBinding() {
        return this.form;
    }

    /** Process an API post request to send and save the form field values
     * to the webservice and publishes a page event to return to read-only-view
     * to indicate that the data was successfully saved or process an validation
     * error indication if there are some validation errors.
     *
     * @param action the save action context
     * @return the new Action context for read-only-view */
    public final PageAction processFormSave(final PageAction action) {
        return handleFormPost(doAPIPost(), action);
    }

    /** process a form post by first resetting all field validation errors (if there are some)
     * then collecting all input data from the form by form-binding to a either a JSON string in
     * HTTP PUT case or to an form-URL-encoded string on HTTP POST case. And PUT or POST the data
     * to the webservice by using the defined RestCall and return the response result of the RestCall.
     *
     * @return the response result of the post (or put) RestCall */
    public Result<T> doAPIPost() {
        // reset all errors that may still be displayed
        this.form.process(
                Utils.truePredicate(),
                fieldAccessor -> fieldAccessor.resetError());

        // post
        return this.post
                .newBuilder()
                .withFormBinding(this.form)
                .call();
    }

    /** Uses the result of a form post to either create and publish a new Action to
     * go to the read-only-view of the specified form to indicate a successful form post
     * or stay within the edit-mode of the form and indicate errors or field validation messages
     * to the user on error case.
     *
     * @param postResult The form post result
     * @param action the action that was applied with the form post
     * @return the new Action that was used to stay on page or go the read-only-view of the form */
    public PageAction handleFormPost(final Result<T> postResult, final PageAction action) {
        return postResult
                .map(result -> {

                    PageAction resultAction = this.pageService.pageActionBuilder(action.pageContext())
                            .newAction(action.definition)
                            .create();
                    if (resultAction.getEntityKey() == null) {
                        resultAction = resultAction.withEntityKey(result.getEntityKey());
                    }

                    return resultAction;
                })
                .onError(this::handleError)
                .getOrThrow();
    }

    public boolean handleError(final Throwable error) {
        if (error instanceof RestCallError) {
            ((RestCallError) error)
                    .getErrorMessages()
                    .stream()
                    .filter(APIMessage.ErrorMessage.FIELD_VALIDATION::isOf)
                    .map(FieldValidationError::new)
                    .forEach(fve -> this.form.process(
                            name -> name.equals(fve.fieldName),
                            fieldAccessor -> showValidationError(fieldAccessor, fve)));
            return true;
        } else {
            log.error("Unexpected error while trying to post form: ", error);
            this.pageContext.notifyError(error);
            return false;
        }
    }

    public boolean hasAnyError() {
        return this.form.hasAnyError();
    }

    private final void showValidationError(
            final FormFieldAccessor fieldAccessor,
            final FieldValidationError valError) {

        fieldAccessor.setError(this.i18nSupport.getText(new LocTextKey(
                FIELD_VALIDATION_LOCTEXT_PREFIX + valError.errorType,
                (Object[]) valError.getAttributes())));
    }

    public FormHandle<T> process(
            final Predicate<String> nameFilter,
            final Consumer<FormFieldAccessor> processor) {

        this.form.process(nameFilter, processor);
        return this;
    }

}
