/*
 * Copyright (c) 2022 ETH Zürich, Educational Development and Technology (LET)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.webservice.servicelayer.dao;

import java.util.Collection;

import org.springframework.context.event.EventListener;

import ch.ethz.seb.sebserver.gbl.model.EntityKey;
import ch.ethz.seb.sebserver.gbl.model.institution.SecurityKey;
import ch.ethz.seb.sebserver.gbl.model.institution.SecurityKey.KeyType;
import ch.ethz.seb.sebserver.gbl.util.Result;
import ch.ethz.seb.sebserver.webservice.servicelayer.dao.impl.ExamDeletionEvent;
import ch.ethz.seb.sebserver.webservice.servicelayer.dao.impl.ExamTemplateDeletionEvent;

/** Concrete EntityDAO interface of SecurityKeyRegistry entities */
public interface SecurityKeyRegistryDAO extends EntityDAO<SecurityKey, SecurityKey> {

    /** Use this to make a copy of an existing security key registry entry for the given exam.
     * The existing registry entry must be a global or one or one of an exam template.
     *
     * @param keyId The security key registry id.
     * @param examId The exam identifier for the security key copy
     * @return Result refer to the newly created SecurityKey or to an error when happened. */
    Result<SecurityKey> registerCopyForExam(Long keyId, Long examId);

    /** Use this to make a copy of r an existing security key registry entry for a given exam template.
     * The existing registry entry must be a global one or one of an exam.
     *
     * @param keyId The security key registry id.
     * @param examTemplateId The exam template identifier for the new security key copy
     * @return Result refer to the newly created SecurityKey or the an error when happened */
    Result<SecurityKey> registerCopyForExamTemplate(Long keyId, Long examTemplateId);

    /** Used to get all security key registry entries of given institution, exam and type.
     *
     * @param institutionId The institution identifier
     * @param examId The exam identifier
     * @param type The type of the security key
     * @return Result refer to collection of all matching security key registry entries or to an error when happened */
    Result<Collection<SecurityKey>> getAll(Long institutionId, Long examId, KeyType type);

    /** Used to delete a given security key registry entry.
     *
     * @param keyId The security key registry entry identifier
     * @return Result refer to the EntityKey of the deleted registry entry or to an error when happened */
    Result<EntityKey> delete(Long keyId);

    /** Internally used to notify exam deletion to delete all registry entries regarded to the deleted exam.
     *
     * @param event The ExamDeletionEvent fired on exam deletion */
    @EventListener(ExamDeletionEvent.class)
    void notifyExamDeletion(ExamDeletionEvent event);

    /** Internally used to notify exam template deletion to delete all registry entries regarded to the deleted exam
     * template
     *
     * @param event ExamTemplateDeletionEvent fired on exam template deletion */
    @EventListener(ExamTemplateDeletionEvent.class)
    void notifyExamTemplateDeletion(ExamTemplateDeletionEvent event);

    /** This checks if there is already a grant for the given key and return it if available
     * or the given key otherwise.
     *
     * @param key SecurityKey data to check if there is a grant registered
     * @return Result refer to the grant if available or the the given key if not or to an error when happened */
    Result<SecurityKey> getGrantOr(final SecurityKey key);

}
