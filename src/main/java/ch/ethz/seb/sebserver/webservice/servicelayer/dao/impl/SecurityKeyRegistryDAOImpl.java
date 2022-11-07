/*
 * Copyright (c) 2022 ETH Zürich, Educational Development and Technology (LET)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.webservice.servicelayer.dao.impl;

import java.util.Collection;
import java.util.Set;
import java.util.function.Predicate;

import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import ch.ethz.seb.sebserver.gbl.api.EntityType;
import ch.ethz.seb.sebserver.gbl.model.EntityKey;
import ch.ethz.seb.sebserver.gbl.model.institution.SecurityKeyRegistry;
import ch.ethz.seb.sebserver.gbl.model.institution.SecurityKeyRegistry.EncryptionType;
import ch.ethz.seb.sebserver.gbl.model.institution.SecurityKeyRegistry.KeyType;
import ch.ethz.seb.sebserver.gbl.profile.WebServiceProfile;
import ch.ethz.seb.sebserver.gbl.util.Result;
import ch.ethz.seb.sebserver.webservice.datalayer.batis.mapper.SecurityKeyRegistryRecordMapper;
import ch.ethz.seb.sebserver.webservice.datalayer.batis.model.SecurityKeyRegistryRecord;
import ch.ethz.seb.sebserver.webservice.servicelayer.dao.FilterMap;
import ch.ethz.seb.sebserver.webservice.servicelayer.dao.SecurityKeyRegistryDAO;

@Lazy
@Component
@WebServiceProfile
public class SecurityKeyRegistryDAOImpl implements SecurityKeyRegistryDAO {

    private final SecurityKeyRegistryRecordMapper securityKeyRegistryRecordMapper;

    public SecurityKeyRegistryDAOImpl(final SecurityKeyRegistryRecordMapper securityKeyRegistryRecordMapper) {
        this.securityKeyRegistryRecordMapper = securityKeyRegistryRecordMapper;
    }

    @Override
    public EntityType entityType() {
        return EntityType.SEB_SECURITY_KEY_REGISTRY;
    }

    @Override
    public Result<SecurityKeyRegistry> byPK(final Long id) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Result<Collection<SecurityKeyRegistry>> allOf(final Set<Long> pks) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Result<SecurityKeyRegistry> createNew(final SecurityKeyRegistry data) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Result<SecurityKeyRegistry> save(final SecurityKeyRegistry data) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Result<Collection<EntityKey>> delete(final Set<EntityKey> all) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Result<Collection<SecurityKeyRegistry>> allMatching(
            final FilterMap filterMap,
            final Predicate<SecurityKeyRegistry> predicate) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Result<SecurityKeyRegistry> registerCopyForExam(final Long keyId, final Long examId) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Result<SecurityKeyRegistry> registerCopyForExamTemplate(final Long keyId, final Long examTemplateId) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public void notifyExamDeletion(final ExamDeletionEvent event) {
        // TODO Auto-generated method stub

    }

    @Override
    public void notifyExamTemplateDeletion(final ExamTemplateDeletionEvent event) {
        // TODO Auto-generated method stub

    }

    private Result<SecurityKeyRegistryRecord> recordByPK(final Long pk) {
        return Result.tryCatch(() -> this.securityKeyRegistryRecordMapper.selectByPrimaryKey(pk));
    }

    private SecurityKeyRegistry toDomainModel(final SecurityKeyRegistryRecord rec) {
        return new SecurityKeyRegistry(
                rec.getId(),
                rec.getInstitutionId(),
                KeyType.byString(rec.getType()),
                rec.getKey(),
                rec.getTag(),
                rec.getExamId(),
                rec.getExamTemplateId(),
                EncryptionType.byString(rec.getEncryptionType()));
    }

}
