/*
 * Copyright (c) 2018 ETH Zürich, Educational Development and Technology (LET)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.webservice.batis;

import static org.junit.Assert.*;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.sql.ResultSet;
import java.sql.SQLException;

import org.codehaus.jackson.JsonParseException;
import org.codehaus.jackson.map.JsonMappingException;
import org.codehaus.jackson.map.ObjectMapper;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.junit.Test;
import org.mockito.Mockito;

import ch.ethz.seb.sebserver.gbl.Constants;
import ch.ethz.seb.sebserver.webservice.datalayer.batis.JodaTimeTypeResolver;

public class JodaTimeTypeResolverTest {

    @Test
    public void testGetNullableResultExceptions() throws SQLException {
        final String columnName = "timestamp";

        final JodaTimeTypeResolver jodaTimeTypeResolver = new JodaTimeTypeResolver();

        final ResultSet resultSetMock = Mockito.mock(ResultSet.class);
        when(resultSetMock.getString(columnName)).thenReturn(null);

        try {
            @SuppressWarnings("unused")
            final DateTime nullableResult = jodaTimeTypeResolver.getNullableResult(resultSetMock, columnName);
            fail("Exception expected here");
        } catch (final Exception e) {
            assertEquals("Failed to parse date-time from SQL string: null", e.getMessage());
        }

    }

    @Test
    public void testGetNullableResultNoMillis() throws SQLException {
        final String columnName = "timestamp";
        final int columnIndex = 0;

        final DateTime pointInTime = new DateTime(0, DateTimeZone.UTC);
        final String pointInTimeString = pointInTime.toString(Constants.DATE_TIME_PATTERN_UTC_NO_MILLIS);
        assertEquals("1970-01-01 00:00:00", pointInTimeString);

        final JodaTimeTypeResolver jodaTimeTypeResolver = new JodaTimeTypeResolver();

        final ResultSet resultSetMock = Mockito.mock(ResultSet.class);
        when(resultSetMock.getString(columnName)).thenReturn(pointInTimeString);
        when(resultSetMock.getString(columnIndex)).thenReturn(pointInTimeString);

        DateTime nullableResult = jodaTimeTypeResolver.getNullableResult(resultSetMock, columnName);
        assertNotNull(nullableResult);
        assertEquals(pointInTimeString, nullableResult.toString(Constants.DATE_TIME_PATTERN_UTC_NO_MILLIS));
        assertEquals(pointInTime, nullableResult);

        nullableResult = jodaTimeTypeResolver.getNullableResult(resultSetMock, columnIndex);
        assertNotNull(nullableResult);
        assertEquals(pointInTimeString, nullableResult.toString(Constants.DATE_TIME_PATTERN_UTC_NO_MILLIS));
        assertEquals(pointInTime, nullableResult);
    }

    @Test
    public void testGetNullableResultWithMillis() throws SQLException {
        final String columnName = "timestamp";

        final DateTime pointInTime = new DateTime(0, DateTimeZone.UTC);
        final String pointInTimeString = pointInTime.toString(Constants.DATE_TIME_PATTERN_UTC_MILLIS);
        assertEquals("1970-01-01 00:00:00.0", pointInTimeString);

        final JodaTimeTypeResolver jodaTimeTypeResolver = new JodaTimeTypeResolver();

        final ResultSet resultSetMock = Mockito.mock(ResultSet.class);
        when(resultSetMock.getString(columnName)).thenReturn(pointInTimeString);

        final DateTime nullableResult = jodaTimeTypeResolver.getNullableResult(resultSetMock, columnName);
        assertNotNull(nullableResult);
        assertEquals("1970-01-01 00:00:00", nullableResult.toString(Constants.DATE_TIME_PATTERN_UTC_NO_MILLIS));
        assertEquals(pointInTime, nullableResult);
    }

    @Test
    public void test() throws JsonParseException, JsonMappingException, IOException {
        final Boolean readValue = new ObjectMapper().readValue("true", Boolean.class);
        assertTrue(readValue);
    }

}
