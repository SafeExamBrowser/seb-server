/*
 * Copyright (c) 2019 ETH Zürich, Educational Development and Technology (LET)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.webservice.servicelayer.dao.impl;

import java.util.Collection;

import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import ch.ethz.seb.sebserver.gbl.model.session.ClientInstruction.InstructionType;
import ch.ethz.seb.sebserver.gbl.profile.WebServiceProfile;
import ch.ethz.seb.sebserver.gbl.util.Result;
import ch.ethz.seb.sebserver.webservice.datalayer.batis.mapper.ClientInstructionRecordMapper;
import ch.ethz.seb.sebserver.webservice.datalayer.batis.model.ClientInstructionRecord;
import ch.ethz.seb.sebserver.webservice.servicelayer.dao.ClientInstructionDAO;

@Lazy
@Component
@WebServiceProfile
public class ClientInstructionDAOImpl implements ClientInstructionDAO {

    private final ClientInstructionRecordMapper clientInstructionRecordMapper;

    protected ClientInstructionDAOImpl(final ClientInstructionRecordMapper clientInstructionRecordMapper) {
        this.clientInstructionRecordMapper = clientInstructionRecordMapper;
    }

    @Override
    @Transactional(readOnly = true)
    public Result<Collection<ClientInstructionRecord>> getAllActive() {
        return Result.tryCatch(() -> this.clientInstructionRecordMapper
                .selectByExample()
                .build()
                .execute());
    }

    @Override
    @Transactional
    public Result<Void> delete(final Long id) {
        return Result.tryCatch(() -> {
            final int deleteByPrimaryKey = this.clientInstructionRecordMapper.deleteByPrimaryKey(id);
            if (deleteByPrimaryKey != 1) {
                throw new RuntimeException("Failed to delete ClientInstruction with id: " + id);
            }
        });
    }

    @Override
    public Result<ClientInstructionRecord> insert(
            final Long examId,
            final InstructionType type,
            final String attributes,
            final String connectionToken) {

        return Result.tryCatch(() -> {
            final ClientInstructionRecord clientInstructionRecord = new ClientInstructionRecord(
                    null,
                    examId,
                    connectionToken,
                    type.name(),
                    attributes);

            this.clientInstructionRecordMapper.insert(clientInstructionRecord);
            return clientInstructionRecord;
        });
    }

}
