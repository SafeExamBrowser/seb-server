package ch.ethz.seb.sebserver.webservice.servicelayer.session;

import ch.ethz.seb.sebserver.gbl.model.exam.Exam;
import ch.ethz.seb.sebserver.gbl.model.session.ClientConnection;
import ch.ethz.seb.sebserver.gbl.model.session.RunningExamInfo;

import java.security.Principal;
import java.util.Collection;

public interface SEBVersionRestrictionService {

    RunningExamInfo getGrantedRunningExamInfo(Exam exam);

    Collection<RunningExamInfo> checkSEBRestriction(ClientConnection cc, Principal principal);

}
