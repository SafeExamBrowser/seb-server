package ch.ethz.seb.sebserver.gbl.model.exam;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "SPSAPIAccessData", description = "Screen Proctoring Service API access data")
public interface SPSAPIAccessData {

    Long getExamId();

    String getSpsServiceURL();

    String getSpsAPIKey();

    CharSequence getSpsAPISecret();

    String getSpsAccountId();

    CharSequence getSpsAccountPassword();
}
