/*
 * Copyright (c) 2019 ETH Zürich, Educational Development and Technology (LET)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.gui.service.page;

import java.util.List;
import java.util.function.Consumer;

import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Shell;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ch.ethz.seb.sebserver.gbl.api.EntityType;
import ch.ethz.seb.sebserver.gbl.model.EntityKey;
import ch.ethz.seb.sebserver.gui.service.ResourceService;
import ch.ethz.seb.sebserver.gui.service.i18n.I18nSupport;
import ch.ethz.seb.sebserver.gui.service.i18n.LocTextKey;

/** Holds a page-context and defines some convenient functionality for page handling */
public interface PageContext {

    Logger log = LoggerFactory.getLogger(PageContext.class);

    /** Defines attribute keys that can be used to store attribute values within the page context state */
    interface AttributeKeys {

        String PAGE_TEMPLATE_COMPOSER_NAME = "ATTR_PAGE_TEMPLATE_COMPOSER_NAME";

        String READ_ONLY = "READ_ONLY";
        String FORCE_READ_ONLY = "FORCE_READ_ONLY";
        String READ_ONLY_FROM = "READ_ONLY_FROM";

        String ENTITY_ID = "ENTITY_ID";
        String PARENT_ENTITY_ID = "PARENT_ENTITY_ID";
        String ENTITY_TYPE = "ENTITY_TYPE";
        String PARENT_ENTITY_TYPE = "PARENT_ENTITY_TYPE";
        String ENTITY_ID_LIST = "ENTITY_ID_LIST";
        String ENTITY_LIST_TYPE = "ENTITY_TYPE";

        String IMPORT_FROM_QUIZ_DATA = "IMPORT_FROM_QUIZ_DATA";

        String COPY_AS_TEMPLATE = "COPY_AS_TEMPLATE";
        String CREATE_FROM_TEMPLATE = "CREATE_FROM_TEMPLATE";

    }

    /** The resource-bundle key of the generic load entity error message. */
    String GENERIC_LOAD_ERROR_TEXT_KEY = "sebserver.error.get.entity";
    String GENERIC_REMOVE_ERROR_TEXT_KEY = "sebserver.error.remove.entity";
    String GENERIC_SAVE_ERROR_TEXT_KEY = "sebserver.error.save.entity";
    String GENERIC_ACTIVATE_ERROR_TEXT_KEY = "sebserver.error.activate.entity";
    String GENERIC_IMPORT_ERROR_TEXT_KEY = "sebserver.error.import";
    LocTextKey SUCCESS_MSG_TITLE =
            new LocTextKey("sebserver.page.message");
    LocTextKey UNEXPECTED_ERROR_KEY =
            new LocTextKey("sebserver.error.action.unexpected.message");

    /** Get the I18nSupport service
     *
     * @return the I18nSupport service */
    I18nSupport getI18nSupport();

    /** Use this to get the ComposerService used by this PageContext
     *
     * @return the ComposerService used by this PageContext */
    ComposerService composerService();

    /** Get the RWT Shell that is bound within this PageContext
     *
     * @return the RWT Shell that is bound within this PageContext */
    Shell getShell();

    /** Get the page root Component.
     *
     * @return the page root Component. */
    Composite getRoot();

    /** Get the Component that is currently set as parent during page tree compose
     *
     * @return the parent Component */
    Composite getParent();

    /** Get a copy of this PageContext.
     *
     * @return a deep copy of this PageContext */
    PageContext copy();

    /** Create a copy of this PageContext with a new parent Composite.
     * The implementation should take care of the immutability of PageContext and return a copy with the new parent
     * by leave this PageContext as is.
     *
     * @param parent the new parent Composite
     * @return a copy of this PageContext with a new parent Composite. */
    PageContext copyOf(Composite parent);

    /** Create a copy of this PageContext with and additionally page context attributes.
     * The additionally page context attributes will get merged with them already defined
     * The implementation should take care of the immutability of PageContext and return a copy with the merge
     * by leave this and the given PageContext as is.
     *
     * @param otherContext the other PageContext to copy the attributes from
     * @return a copy of this PageContext with with and additionally page context attributes. */
    PageContext copyOfAttributes(PageContext otherContext);

    /** Adds the specified attribute to the existing page context attributes.
     * The implementation should take care of the immutability of PageContext and return a copy
     * by leave this PageContext as is.
     *
     * @param key the key of the attribute
     * @param value the value of the attribute
     * @return this PageContext instance (builder pattern) */
    PageContext withAttribute(String key, String value);

    /** Gets a copy of this PageContext with cleared attribute map.
     *
     * @return a copy of this PageContext with cleared attribute map. */
    PageContext clearAttributes();

    /** Get the attribute value that is mapped to the given name or null of no mapping exists
     *
     * @param name the attribute name
     * @return the attribute value that is mapped to the given name or null if no mapping exists */
    String getAttribute(String name);

    /** Get the attribute value that is mapped to the given name or a default value if no mapping exists
     *
     * @param name the attribute name
     * @param def the default value
     * @return the attribute value that is mapped to the given name or null of no mapping exists */
    String getAttribute(String name, String def);

    /** Indicates if the attribute with the key READ_ONLY is set to true within this PageContext
     *
     * @return true if the attribute with the key READ_ONLY is set to true */
    boolean isReadonly();

    /** Gets an EntityKey for the base Entity that is associated within this PageContext by using
     * the attribute keys ENTITY_ID and ENTITY_TYPE to fetch the attribute values for an EntityKey
     *
     * @return the EntityKey of the base Entity that is associated within this PageContext */
    EntityKey getEntityKey();

    /** Gets an EntityKey for the parent Entity that is associated within this PageContext by using
     * the attribute keys PARENT_ENTITY_ID and PARENT_ENTITY_TYPE to fetch the attribute values for an EntityKey
     *
     * @return the EntityKey of the parent Entity that is associated within this PageContext */
    EntityKey getParentEntityKey();

    /** Get a list of entity keys within the attribute ENTITY_ID_LIST and ENTITY_LIST_TYPE.
     *
     * @return A list of entity keys if available from the attributes map */
    List<EntityKey> getEntityKeyList();

    /** Adds a given EntityKey as base Entity key to a new PageContext that is returned as a copy of this PageContext.
     *
     * @param entityKey the EntityKey to add as base Entity key
     * @return the new PageContext with the EntityKey added */
    PageContext withEntityKey(EntityKey entityKey);

    /** Adds a given EntityKey as parent Entity key to a new PageContext that is returned as a copy of this PageContext.
     *
     * @param entityKey the EntityKey to add as parent Entity key
     * @return the new PageContext with the EntityKey added */
    PageContext withParentEntityKey(EntityKey entityKey);

    /** Adds a given collection of EntityKey to a new PageContext that is returned as a copy of this PageContext.
     *
     * @param entityKeys the list of EntityKey to add
     * @return the new PageContext with the list of EntityKey added */
    PageContext withEntityKeys(List<EntityKey> entityKeys);

    /** Create a copy of this PageContext and resets both entity keys attributes, the base and the parent EntityKey
     *
     * @return copy of this PageContext with reset EntityKey attributes (base and parent) */
    PageContext clearEntityKeys();

    /** Indicates if an attribute with the specified name exists within this PageContext
     *
     * @param name the name of the attribute
     * @return true if the attribute with the specified name exists within this PageContext */
    boolean hasAttribute(String name);

    /** Returns a new PageContext with the removed attribute by name
     *
     * @param name the name of the attribute to remove
     * @return a copy of this PageContext with the removed attribute */
    PageContext removeAttribute(String name);

    /** Apply a confirm dialog with a specified confirm message and a callback code
     * block that will be executed on users OK selection.
     *
     * @param confirmMessage the localized confirm message key
     * @param callback callback code block that will be called on users selection */
    void applyConfirmDialog(LocTextKey confirmMessage, final Consumer<Boolean> callback);

    /** This can be used to forward to a defined page.
     *
     * @param pageDefinition the defined page */
    void forwardToPage(PageDefinition pageDefinition);

    /** Forward to main page */
    void forwardToMainPage();

    /** Forward to login page */
    void forwardToLoginPage();

    /** Notify an error dialog to the user with specified error message and
     * optional exception instance
     *
     * @param errorMessage the error message to display
     * @param error the error as Exception */
    void notifyError(LocTextKey errorMessage, Exception error);

    /** Notify a generic load error to the user by pop-up
     *
     * @param entityType the type of the entity
     * @param error the original error */
    default void notifyLoadError(final EntityType entityType, final Exception error) {
        notifyError(
                new LocTextKey(
                        GENERIC_LOAD_ERROR_TEXT_KEY,
                        getI18nSupport().getText(ResourceService.getEntityTypeNameKey(entityType))),
                error);
    }

    /** Notify a generic remove error to the user by pop-up
     *
     * @param entityType the type of the entity
     * @param error the original error */
    default void notifyRemoveError(final EntityType entityType, final Exception error) {
        notifyError(
                new LocTextKey(
                        GENERIC_REMOVE_ERROR_TEXT_KEY,
                        getI18nSupport().getText(ResourceService.getEntityTypeNameKey(entityType))),
                error);
    }

    /** Notify a generic save error to the user by pop-up
     *
     * @param entityType the type of the entity
     * @param error the original error */
    default void notifySaveError(final EntityType entityType, final Exception error) {
        notifyError(
                new LocTextKey(
                        GENERIC_SAVE_ERROR_TEXT_KEY,
                        getI18nSupport().getText(ResourceService.getEntityTypeNameKey(entityType))),
                error);
    }

    /** Notify a generic activation error to the user by pop-up
     *
     * @param entityType the type of the entity
     * @param error the original error */
    default void notifyActivationError(final EntityType entityType, final Exception error) {
        notifyError(
                new LocTextKey(
                        GENERIC_ACTIVATE_ERROR_TEXT_KEY,
                        getI18nSupport().getText(ResourceService.getEntityTypeNameKey(entityType))),
                error);
    }

    /** Notify a generic import error to the user by pop-up
     *
     * @param entityType the type of the entity
     * @param error the original error */
    default void notifyImportError(final EntityType entityType, final Exception error) {
        notifyError(
                new LocTextKey(
                        GENERIC_IMPORT_ERROR_TEXT_KEY,
                        getI18nSupport().getText(ResourceService.getEntityTypeNameKey(entityType))),
                error);
    }

    /** Notify a generic unexpected error to the user by pop-up
     *
     * @param error the original error */
    default void notifyUnexpectedError(final Exception error) {
        if (error instanceof PageMessageException) {
            publishInfo(((PageMessageException) error).getMessageKey());
            return;
        }
        notifyError(UNEXPECTED_ERROR_KEY, error);
    }

    /** Publish and shows a message to the user with the given localized title and
     * localized message. The message text can also be HTML text as far as RWT supports it.
     *
     * @param title the localized text key of the title message
     * @param message the localized text key of the message */
    void publishPageMessage(LocTextKey title, LocTextKey message);

    /** Publish an information message to the user with the given localized message.
     * The message text can also be HTML text as far as RWT supports it
     *
     * @param message the localized text key of the message */
    default void publishInfo(final LocTextKey message) {
        publishPageMessage(new LocTextKey("sebserver.page.message"), message);
    }

    /** Publish and shows a formatted PageMessageException to the user.
     *
     * @param pme the PageMessageException */
    void publishPageMessage(PageMessageException pme);

}
