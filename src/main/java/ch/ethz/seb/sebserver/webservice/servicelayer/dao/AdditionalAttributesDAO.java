/*
 * Copyright (c) 2019 ETH Zürich, Educational Development and Technology (LET)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.webservice.servicelayer.dao;

import java.util.Collection;
import java.util.Map;
import java.util.stream.Collectors;

import ch.ethz.seb.sebserver.gbl.api.EntityType;
import ch.ethz.seb.sebserver.gbl.util.Result;
import ch.ethz.seb.sebserver.webservice.datalayer.batis.model.AdditionalAttributeRecord;

/** Defines functionality to access additional attributes.
 *
 * Additional attributes are name/value pairs associated with a specified entity but stored
 * in a separated data-base table. */
public interface AdditionalAttributesDAO {

    /** Use this to get all additional attribute records for a specific entity.
     *
     * @param type the entity type
     * @param entityId the entity identifier (primary key)
     * @return Result refer to the collection of additional attribute records or to an error if happened */
    Result<Collection<AdditionalAttributeRecord>> getAdditionalAttributes(
            EntityType type,
            Long entityId);

    /** Use this to get a AdditionalAttributeRecord for a specific attribute
     *
     * @param type the entity type
     * @param entityId the entity identifier (primary key)
     * @param attributeName the name of the attribute
     * @return Result refer to the collection of additional attribute records or to an error if happened */
    Result<AdditionalAttributeRecord> getAdditionalAttribute(
            EntityType type,
            Long entityId,
            String attributeName);

    /** Use this to save an additional attribute for a specific entity.
     * If the additional attribute with specified name already exists for the specified entity
     * this updates just the value for this additional attribute. Otherwise create a new instance
     * of additional attribute with the given data
     *
     * @param type the entity type
     * @param entityId the entity identifier (primary key)
     * @param name the name of the attribute
     * @param value the value of the attribute */
    Result<AdditionalAttributeRecord> saveAdditionalAttribute(
            EntityType type,
            Long entityId,
            String name,
            String value);

    /** Use this to save an additional attributes for a specific entity.
     * If an additional attribute with specified name already exists for the specified entity
     * this updates just the value for this additional attribute. Otherwise create a new instance
     * of additional attribute with the given data
     *
     * @param type the entity type
     * @param entityId the entity identifier (primary key)
     * @param attributes Map of attributes to save for */
    default Result<Collection<AdditionalAttributeRecord>> saveAdditionalAttributes(
            final EntityType type,
            final Long entityId,
            final Map<String, String> attributes) {

        return Result.tryCatch(() -> attributes.entrySet()
                .stream()
                .map(attr -> saveAdditionalAttribute(type, entityId, attr.getKey(), attr.getValue()))
                .flatMap(Result::onErrorLogAndSkip)
                .collect(Collectors.toList()));
    }

    /** Use this to delete an additional attribute by identifier (primary-key)
     *
     * @param id identifier (primary-key) */
    void delete(Long id);

    /** Use this to delete an additional attribute by its entity identifier and name.
     *
     * @param type the entity type
     * @param entityId the entity identifier (primary-key)
     * @param name the name of the additional attribute */
    void delete(EntityType type, Long entityId, String name);

    /** Use this to delete all additional attributes for a given entity.
     *
     * @param type the entity type
     * @param entityId the entity identifier (primary-key) */
    void deleteAll(EntityType type, Long entityId);

}
