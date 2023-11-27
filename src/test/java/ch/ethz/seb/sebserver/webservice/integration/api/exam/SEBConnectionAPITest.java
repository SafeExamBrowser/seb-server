package ch.ethz.seb.sebserver.webservice.integration.api.exam;

import static org.junit.Assert.*;
import static org.junit.Assert.assertEquals;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.security.Principal;
import java.util.Collection;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;


import ch.ethz.seb.sebserver.gbl.api.API;
import ch.ethz.seb.sebserver.gbl.api.JSONMapper;
import ch.ethz.seb.sebserver.gbl.model.Page;
import ch.ethz.seb.sebserver.gbl.model.exam.Exam;
import ch.ethz.seb.sebserver.gbl.model.session.ClientConnection;
import ch.ethz.seb.sebserver.gbl.model.session.RunningExamInfo;
import ch.ethz.seb.sebserver.webservice.servicelayer.authorization.UserService;
import ch.ethz.seb.sebserver.webservice.servicelayer.dao.ClientConnectionDAO;
import ch.ethz.seb.sebserver.webservice.servicelayer.dao.UserDAO;
import ch.ethz.seb.sebserver.webservice.weblayer.api.APIExceptionHandler;
import ch.ethz.seb.sebserver.webservice.weblayer.api.ExamAPI_V1_Controller;
import ch.ethz.seb.sebserver.webservice.weblayer.api.ExamAdministrationController;
import org.junit.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.context.request.async.StandardServletAsyncWebRequest;

@Sql(scripts = { "classpath:schema-test.sql", "classpath:data-test.sql", "classpath:data-test-additional.sql" })
public class SEBConnectionAPITest extends ExamAPIIntegrationTester {

    @Autowired
    private ExamAPI_V1_Controller examAPI_V1_Controller;
    @Autowired
    private ExamAdministrationController examAdministrationController;
    @Autowired
    private ClientConnectionDAO clientConnectionDAO;
    @Autowired
    private APIExceptionHandler apiExceptionHandler;
    @Autowired
    private UserDAO userDAO;
    @MockBean
    private UserService userService;

    @Test
    public void testRunningExam6Available() {

        Mockito.when(userService.getCurrentUser()).thenReturn(userDAO.sebServerUserByUsername("admin").get());

        final HttpServletRequest requestMock = Mockito.mock(HttpServletRequest.class);
        Page<Exam> page = examAdministrationController.getPage(
                1L,
                1,
                10,
                null,
                null,
                requestMock);

        assertNotNull(page);
        final Optional<Exam> runningExam = page.content.stream().filter(e -> e.status == Exam.ExamStatus.RUNNING).findFirst();
        assertTrue(runningExam.isPresent());
        final Exam exam = runningExam.get();
        assertEquals("Exam [id=2, institutionId=1, lmsSetupId=1, externalId=quiz6, name=quiz6, description=null, startTime=null, endTime=null, startURL=https://test.lms.mockup, type=MANAGED, owner=admin, supporter=[admin], status=RUNNING, browserExamKeys=null, active=true, lastUpdate=null]", exam.toString());
    }

    @Test
    public void testBadRequestError() throws Exception {
        final Principal principal = Mockito.mock(Principal.class);
        final HttpServletRequest request = Mockito.mock(HttpServletRequest.class);
        final HttpServletResponse response = new MockHttpServletResponse();

        final CompletableFuture<Collection<RunningExamInfo>> apiCall = examAPI_V1_Controller.handshakeCreate(
                null,
                null,
                null,
                null,
                principal,
                request,
                response);

                try {
                    apiCall.get();
                } catch (final Exception e) {
                    final ResponseEntity<Object> responseEntity = apiExceptionHandler
                            .handleAccessDenied(
                                    (AccessDeniedException) e.getCause(),
                                    new StandardServletAsyncWebRequest(request, response));

                    final String jsonBody = new JSONMapper().writeValueAsString(responseEntity.getBody());
                    assertEquals("[{\"messageCode\":\"1001\",\"systemMessage\":\"FORBIDDEN\",\"details\":\"Unknown or illegal client access\",\"attributes\":[]}]", jsonBody);
                }
    }

    @Test
    public void testOrdinarySEBHandshakeWithKnownExam() throws Exception {

        // ******************************************************************
        // 1. Handshake creation
        HttpServletResponse httpServletResponse = handshakePOST(
                1L,
                2L,
                "baba",
                "Win",
                "3.5",
                "m2000");

        // 1.1 Assert response
        assertEquals("200", String.valueOf(httpServletResponse.getStatus()));
        final String connectionToken = httpServletResponse.getHeader("SEBConnectionToken");

        assertNotNull(connectionToken);
        assertNotNull(httpServletResponse.getHeader("SEBExamSalt"));
        assertEquals(
                "[{\"examId\":\"2\",\"name\":\"quiz6\",\"url\":\"https://test.lms.mockup\",\"lmsType\":\"MOCKUP\"}]",
                ((MockHttpServletResponse) httpServletResponse).getContentAsString());

        // 1.2 Assert persistent data
        ClientConnection clientConnection = clientConnectionDAO
                .byConnectionToken(connectionToken)
                .getOrThrow();
        assertNotNull(clientConnection);
        assertEquals("CONNECTION_REQUESTED", clientConnection.getStatus().name());
        assertEquals(connectionToken, clientConnection.connectionToken);
        assertEquals(1, (long) clientConnection.institutionId);
        assertEquals(2, (long) clientConnection.examId);
        assertEquals("127.0.0.1", clientConnection.clientAddress);
        assertEquals("baba", clientConnection.sebClientUserId);
        assertEquals("Win", clientConnection.sebOSName);
        assertEquals("3.5", clientConnection.sebVersion);
        assertEquals("m2000", clientConnection.sebMachineName);
        // userSessionId should be client id if not given yet
        assertEquals("baba", clientConnection.userSessionId);

        // ******************************************************************
        // 2. Handshake update
        httpServletResponse = handshakePATCH(
                connectionToken,
                2L,
                "John Doe",
                "baba2",
                "Mac",
                "3.7",
                "fvbfvfsv",
                null);

        // 2.1 Assert response
        assertEquals("200", String.valueOf(httpServletResponse.getStatus()));
        assertNotNull(httpServletResponse.getHeader("SEBExamSalt"));

        // 2.2 Assert persistent data
        clientConnection = clientConnectionDAO
                .byConnectionToken(connectionToken)
                .getOrThrow();
        assertNotNull(clientConnection);
        assertEquals("AUTHENTICATED", clientConnection.getStatus().name());
        assertEquals(connectionToken, clientConnection.connectionToken);
        assertEquals(1, (long) clientConnection.institutionId);
        assertEquals(2, (long) clientConnection.examId);
        assertEquals("127.0.0.1", clientConnection.clientAddress);
        assertEquals("baba", clientConnection.sebClientUserId);     // should not be possible to overwrite previous setting
        assertEquals("Win", clientConnection.sebOSName);            // should not be possible to overwrite previous setting
        assertEquals("3.5", clientConnection.sebVersion);           // should not be possible to overwrite previous setting
        assertEquals("m2000", clientConnection.sebMachineName);     // should not be possible to overwrite previous setting

    }

    private HttpServletResponse handshakePOST(
            final Long institutionId,
            final Long examId,
            final String client_id,
            final String seb_os_name,
            final String seb_version,
            final String seb_machine_name) throws Exception {

        final Principal principal = Mockito.mock(Principal.class);
        final HttpServletRequest request = new MockHttpServletRequest("POST", "/exam-api/v1/handshake  ") ;
        final HttpServletResponse response = new MockHttpServletResponse();
        Mockito.when(principal.getName()).thenReturn("test");

        final MultiValueMap<String, String> formParams = new LinkedMultiValueMap<>();
        formParams.add(API.EXAM_API_PARAM_SEB_VERSION, seb_version);
        formParams.add(API.EXAM_API_PARAM_SEB_OS_NAME, seb_os_name);
        formParams.add(API.EXAM_API_PARAM_SEB_MACHINE_NAME, seb_machine_name);

        final CompletableFuture<Collection<RunningExamInfo>> apiCall = examAPI_V1_Controller
                .handshakeCreate(institutionId, examId, client_id, formParams, principal, request, response);

        response.getOutputStream().print(new JSONMapper().writeValueAsString(apiCall.get()));
        return response;
    }

    private HttpServletResponse handshakePATCH(
            final String connectionToken,
            final Long examId,
            final String userSessionId,
            final String client_id,
            final String seb_os_name,
            final String seb_version,
            final String seb_machine_name,
            final String askHash) throws Exception {

        final Principal principal = Mockito.mock(Principal.class);
        final HttpServletRequest request = new MockHttpServletRequest("PATCH", "/exam-api/v1/handshake  ") ;
        final HttpServletResponse response = new MockHttpServletResponse();
        Mockito.when(principal.getName()).thenReturn("test");

        examAPI_V1_Controller.handshakeUpdate(
                connectionToken, examId, userSessionId, seb_version, seb_os_name,
                seb_machine_name, askHash, client_id, principal, request, response)
                .get();

        return response;
    }

    private HttpServletResponse handshakePUT(
            final String connectionToken,
            final Long examId,
            final String userSessionId,
            final String client_id,
            final String seb_os_name,
            final String seb_version,
            final String seb_machine_name,
            final String askHash) throws Exception {

        final Principal principal = Mockito.mock(Principal.class);
        final HttpServletRequest request = new MockHttpServletRequest("PUT", "/exam-api/v1/handshake  ") ;
        final HttpServletResponse response = new MockHttpServletResponse();
        Mockito.when(principal.getName()).thenReturn("test");

        examAPI_V1_Controller.handshakeEstablish(
                        connectionToken, examId, userSessionId, seb_version, seb_os_name,
                        seb_machine_name, askHash, client_id, principal, request, response)
                .get();

        return response;
    }
}
