package ch.ethz.seb.sebserver.webservice.weblayer.oauth.resserver;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.apache.http.HttpHeaders;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;

import java.io.IOException;

public class UnauthorizedRequestHandler implements AuthenticationEntryPoint {

    static final String ERROR_MSG_TEMPLATE = """
                {
                  "error": "invalid_token",
                  "error_description": "Invalid access token: %s"
                }""";

    private static final Logger log = LoggerFactory.getLogger(UnauthorizedRequestHandler.class);
    private final String name;
    public UnauthorizedRequestHandler(String name) {
        this.name = name;
    }

    @Override
    public void commence(
            final HttpServletRequest request,
            final HttpServletResponse response,
            final AuthenticationException authenticationException) throws IOException {

        log.warn("{}: Unauthorized Request on: {}", name, request.getRequestURI(), authenticationException);

        try {
            String bearerTokenHeader = request.getHeader(HttpHeaders.AUTHORIZATION);
            if (bearerTokenHeader != null) {
                bearerTokenHeader = bearerTokenHeader.replace("Bearer ", "");
            }
            response.getOutputStream().print(String.format(ERROR_MSG_TEMPLATE, bearerTokenHeader));
        } catch (Exception e) {
            log.error("Failed to create proper OAuth error: {}", e.getMessage());
        }

        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.flushBuffer();
    }
}
