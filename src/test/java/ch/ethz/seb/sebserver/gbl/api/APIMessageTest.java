/*
 * Copyright (c) 2019 ETH Zürich, Educational Development and Technology (LET)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.gbl.api;

import static org.junit.Assert.*;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import org.junit.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import ch.ethz.seb.sebserver.gbl.api.APIMessage.APIMessageException;

public class APIMessageTest {

    @Test
    public void testInit() {
        final APIMessage message = APIMessage.ErrorMessage.GENERIC.of();
        assertEquals(
                "APIMessage [messageCode=0, systemMessage=Generic error message, details=null, attributes=[]]",
                message.toString());

        assertTrue(APIMessage.ErrorMessage.GENERIC.isOf(message));

        final APIMessage message2 = APIMessage.ErrorMessage.GENERIC.of(
                new RuntimeException("Some Exception"),
                "attribute1",
                "attribute2");

        assertEquals(
                "APIMessage [messageCode=0, systemMessage=Generic error message, details=Some Exception, attributes=[attribute1, attribute2]]",
                message2.toString());

        final APIMessage message3 = new APIMessage("test", "test");
        assertEquals(
                "APIMessage [messageCode=test, systemMessage=test, details=null, attributes=[]]",
                message3.toString());
    }

    @Test
    public void testCreateErrorResponse() {
        final ResponseEntity<List<APIMessage>> errorResponse = APIMessage.ErrorMessage.GENERIC.createErrorResponse();
        assertTrue(errorResponse.getStatusCode() == HttpStatus.INTERNAL_SERVER_ERROR);
        final List<APIMessage> body = errorResponse.getBody();
        assertNotNull(body);
        assertFalse(body.isEmpty());
        assertTrue(body.size() == 1);
        assertEquals(
                "APIMessage [messageCode=0, systemMessage=Generic error message, details=null, attributes=[]]",
                body.get(0).toString());

    }

    @Test
    public void testToHTML() {
        final List<APIMessage> messages = Arrays.asList(
                APIMessage.ErrorMessage.GENERIC.of(),
                APIMessage.ErrorMessage.ILLEGAL_API_ARGUMENT.of());

        final String html = APIMessage.toHTML("title message", messages);
        assertEquals(
                "<b>Failure: </b><br/><br/>title message<br/><br/><b>Detail Messages:</b><br/><br/>&nbsp;&nbsp;code&nbsp;:&nbsp;0<br/>&nbsp;&nbsp;system message&nbsp;:&nbsp;Generic error message<br/>&nbsp;&nbsp;details&nbsp;:&nbsp;--<br/><br/>&nbsp;&nbsp;code&nbsp;:&nbsp;1010<br/>&nbsp;&nbsp;system message&nbsp;:&nbsp;Illegal API request argument<br/>&nbsp;&nbsp;details&nbsp;:&nbsp;--<br/><br/>",
                html);

        final String html2 = APIMessage.toHTML(messages);
        assertEquals(
                "<b>Messages:</b><br/><br/>&nbsp;&nbsp;code&nbsp;:&nbsp;0<br/>&nbsp;&nbsp;system message&nbsp;:&nbsp;Generic error message<br/>&nbsp;&nbsp;details&nbsp;:&nbsp;--<br/><br/>&nbsp;&nbsp;code&nbsp;:&nbsp;1010<br/>&nbsp;&nbsp;system message&nbsp;:&nbsp;Illegal API request argument<br/>&nbsp;&nbsp;details&nbsp;:&nbsp;--<br/><br/>",
                html2);
    }

    @Test
    public void testAPIMessageException() {
        final APIMessageException apiMessageException =
                new APIMessage.APIMessageException(APIMessage.ErrorMessage.FORBIDDEN);

        assertEquals("FORBIDDEN", apiMessageException.getMessage());
        Collection<APIMessage> apiMessages = apiMessageException.getAPIMessages();
        assertNotNull(apiMessages);
        assertFalse(apiMessages.isEmpty());
        assertTrue(apiMessages.size() == 1);

        final APIMessageException apiMessageException2 =
                new APIMessage.APIMessageException(APIMessage.ErrorMessage.FORBIDDEN, "detail", "attribute1",
                        "attribute2");

        assertEquals("FORBIDDEN", apiMessageException2.getMessage());
        apiMessages = apiMessageException2.getAPIMessages();
        assertNotNull(apiMessages);
        assertFalse(apiMessages.isEmpty());
        assertTrue(apiMessages.size() == 1);
        assertEquals(
                "APIMessage [messageCode=1001, systemMessage=FORBIDDEN, details=detail, attributes=[attribute1, attribute2]]",
                apiMessages.iterator().next().toString());
    }

}
