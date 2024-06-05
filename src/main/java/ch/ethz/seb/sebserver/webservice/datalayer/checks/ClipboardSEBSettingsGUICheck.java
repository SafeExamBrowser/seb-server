/*
 *  Copyright (c) 2019 ETH Zürich, IT Services
 *
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.webservice.datalayer.checks;

import static ch.ethz.seb.sebserver.webservice.datalayer.batis.mapper.OrientationRecordDynamicSqlSupport.*;
import static ch.ethz.seb.sebserver.webservice.datalayer.batis.mapper.OrientationRecordDynamicSqlSupport.title;

import ch.ethz.seb.sebserver.gbl.profile.WebServiceProfile;
import ch.ethz.seb.sebserver.gbl.util.Result;
import ch.ethz.seb.sebserver.webservice.DBIntegrityCheck;
import ch.ethz.seb.sebserver.webservice.datalayer.batis.mapper.OrientationRecordDynamicSqlSupport;
import ch.ethz.seb.sebserver.webservice.datalayer.batis.mapper.OrientationRecordMapper;
import ch.ethz.seb.sebserver.webservice.datalayer.batis.model.OrientationRecord;
import ch.ethz.seb.sebserver.webservice.servicelayer.dao.OrientationDAO;
import org.mybatis.dynamic.sql.SqlBuilder;
import org.mybatis.dynamic.sql.update.UpdateDSL;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

@Lazy
@Component
@WebServiceProfile
public class ClipboardSEBSettingsGUICheck implements DBIntegrityCheck {

    public static final Logger INIT_LOGGER = LoggerFactory.getLogger("ch.ethz.seb.SEB_SERVER_INIT");

    private final OrientationRecordMapper orientationRecordMapper;

    public ClipboardSEBSettingsGUICheck(final OrientationRecordMapper orientationRecordMapper) {
        this.orientationRecordMapper = orientationRecordMapper;
    }

    @Override
    public String name() {
        return "ClipboardSEBSettingsGUICheck";
    }

    @Override
    public String description() {
        return "Check if clipboardPolicy SEB Setting is missing in the GUI and if so add it to GUI";
    }

    @Override
    public Result<String> applyCheck(boolean tryFix) {
        return Result.tryCatch(() -> {
            // check if clipboardPolicy SEB Setting is missing
            final Long count = orientationRecordMapper.countByExample()
                    .where(templateId, SqlBuilder.isEqualTo(0L))
                    .and(configAttributeId, SqlBuilder.isEqualTo(1201L))
                    .build()
                    .execute();

            if (count != null && count.intValue() > 0) {
                return "clipboardPolicy SEB Setting detected in GUI";
            }

            INIT_LOGGER.info("--------> Missing clipboardPolicy SEB Setting in GUI detected. Add it");

            // move allowedSEBVersion setting
            UpdateDSL.updateWithMapper(orientationRecordMapper::update, orientationRecord)
                    .set(yPosition).equalTo(21)
                    .where(templateId, SqlBuilder.isEqualTo(0L))
                    .and(configAttributeId, SqlBuilder.isEqualTo(1578L))
                    .build()
                    .execute();

            // add clipboardPolicy setting
            orientationRecordMapper.insert(new OrientationRecord(
                    null,
                    1201L,
                    0L,
                    9L,
                    "clipboardPolicy",
                    7,
                    18,
                    5,
                    2,
                    "NONE"
            ));

            return "Missing clipboardPolicy SEB Setting in GUI successfully added";
        });
    }
}
