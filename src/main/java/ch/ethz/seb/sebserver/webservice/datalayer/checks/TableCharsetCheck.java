/*
 * Copyright (c) 2022 ETH Zürich, Educational Development and Technology (LET)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.webservice.datalayer.checks;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

import javax.sql.DataSource;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Lazy;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import ch.ethz.seb.sebserver.gbl.profile.WebServiceProfile;
import ch.ethz.seb.sebserver.gbl.util.Result;
import ch.ethz.seb.sebserver.webservice.DBIntegrityCheck;

@Lazy
@Component
@WebServiceProfile
public class TableCharsetCheck implements DBIntegrityCheck {

    private static final String SCHEMA_NAME_PROPERTY = "sebserver.init.database.integrity.check.schema";
    private static final String UTF8MB4_GENERAL_CI = "utf8mb4_general_ci";
    private static final String TABLE_NAME = "TABLE_NAME";
    private static final String TABLE_COLLATION = "TABLE_COLLATION";

    private static final Logger log = LoggerFactory.getLogger(TableCharsetCheck.class);

    private final DataSource dataSource;
    private final String schemaName;

    public TableCharsetCheck(
            final DataSource dataSource,
            final Environment environment) {
        super();
        this.dataSource = dataSource;
        this.schemaName = environment.getProperty(SCHEMA_NAME_PROPERTY, (String) null);
    }

    @Override
    public String name() {
        return "TableCharsetCheck";
    }

    @Override
    public String description() {
        return "Checks the char-set and collation of DB tables if correct utf8mb4_general_ci is set";
    }

    @Override
    public Result<String> applyCheck(final boolean tryFix) {

        if (StringUtils.isEmpty(this.schemaName)) {
            return Result.of("Skip check since sebserver.init.database.integrity.check.schema is not defined");
        }

        Connection connection = null;
        try {
            connection = this.dataSource.getConnection();

            final PreparedStatement prepareStatement =
                    connection.prepareStatement(
                            "SELECT * FROM INFORMATION_SCHEMA.TABLES WHERE TABLE_SCHEMA = \"" + this.schemaName + "\"");
            prepareStatement.execute();
            final ResultSet resultSet = prepareStatement.getResultSet();
            final Map<String, String> tablesWithWrongCollation = new HashMap<>();
            while (resultSet.next()) {
                final String collation = resultSet.getString(TABLE_COLLATION);
                if (!UTF8MB4_GENERAL_CI.equals(collation)) {
                    tablesWithWrongCollation.put(resultSet.getString(TABLE_NAME), collation);
                }
            }

            final Connection con = connection;
            if (!tablesWithWrongCollation.isEmpty()) {
                if (tryFix) {
                    tablesWithWrongCollation.entrySet().forEach(entry -> tryFix(con, entry));
                } else {
                    return Result.of("Found tables with wrong collation: " + tablesWithWrongCollation);
                }
            }

        } catch (final Exception e) {
            log.error("Failed to apply database table check: ", e);
        } finally {
            if (connection != null) {
                try {
                    connection.close();
                } catch (final SQLException e) {
                    log.error("Failed to close connection: ", e);
                }
            }
        }

        return Result.of("OK");
    }

    private void tryFix(final Connection connection, final Map.Entry<String, String> entry) {
        try {

            log.info("Try to fix collation for table: {}", entry);

            final PreparedStatement prepareStatement = connection.prepareStatement(
                    "ALTER TABLE " + entry.getKey() + " CONVERT TO CHARACTER SET 'utf8mb4' COLLATE '"
                            + UTF8MB4_GENERAL_CI
                            + "'");

            prepareStatement.execute();

            log.info("Successfully changed collision for table: {}", entry.getKey());

        } catch (final Exception e) {
            log.error("Failed to changed collision for table", e);
        }
    }

}
