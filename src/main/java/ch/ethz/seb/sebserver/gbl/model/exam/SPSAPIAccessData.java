package ch.ethz.seb.sebserver.gbl.model.exam;

public interface SPSAPIAccessData {

    //Long getExamId();

    String getSpsServiceURL();

    String getInternalServiceURL();

    String getSpsAPIKey();

    CharSequence getSpsAPISecret();

    String getSpsAccountId();

    CharSequence getSpsAccountPassword();
}
