package ch.ethz.seb.sebserver.webservice.servicelayer.session.impl;

import ch.ethz.seb.sebserver.gbl.model.exam.AllowedSEBVersion;
import ch.ethz.seb.sebserver.gbl.model.exam.Exam;
import ch.ethz.seb.sebserver.gbl.model.session.ClientConnection;
import ch.ethz.seb.sebserver.gbl.model.session.RunningExamInfo;
import ch.ethz.seb.sebserver.webservice.WebserviceInfo;
import ch.ethz.seb.sebserver.webservice.servicelayer.dao.ClientConnectionDAO;
import ch.ethz.seb.sebserver.webservice.servicelayer.dao.LmsSetupDAO;
import ch.ethz.seb.sebserver.webservice.servicelayer.dao.SEBClientConfigDAO;
import ch.ethz.seb.sebserver.webservice.servicelayer.session.ExamSessionService;
import ch.ethz.seb.sebserver.webservice.servicelayer.session.SEBClientVersionService;
import ch.ethz.seb.sebserver.webservice.servicelayer.session.SEBVersionRestrictionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.security.Principal;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

@Service
public class SEBVersionRestrictionServiceImpl implements SEBVersionRestrictionService {

    private static final Logger log = LoggerFactory.getLogger(SEBVersionRestrictionServiceImpl.class);

    private final static String REDIRECT_URL_TEMPLATE = "%s/admin-api/v1/seb-version-info?selected-exam=%s&seb-version=%s&restriction=%s&download=https://www.safeexambrowser.org/download_en.html";


    private final SEBClientVersionService sebClientVersionService;
    private final ExamSessionService examSessionService;
    private final SEBClientConfigDAO sebClientConfigDAO;
    private final ClientConnectionDAO clientConnectionDAO;
    private final WebserviceInfo webserviceInfo;
    private final LmsSetupDAO lmsSetupDAO;

    public SEBVersionRestrictionServiceImpl(
            final SEBClientVersionService sebClientVersionService,
            final ExamSessionService examSessionService,
            final SEBClientConfigDAO sebClientConfigDAO,
            final ClientConnectionDAO clientConnectionDAO,
            final WebserviceInfo webserviceInfo,
            final LmsSetupDAO lmsSetupDAO) {

        this.sebClientVersionService = sebClientVersionService;
        this.examSessionService = examSessionService;
        this.sebClientConfigDAO = sebClientConfigDAO;
        this.clientConnectionDAO = clientConnectionDAO;
        this.webserviceInfo = webserviceInfo;
        this.lmsSetupDAO = lmsSetupDAO;
    }

    // TODO cache LMSSetup and use from cache here
    public RunningExamInfo getGrantedRunningExamInfo(final Exam exam) {
        return new RunningExamInfo(
                exam,
                this.lmsSetupDAO.byPK(exam.lmsSetupId)
                        .map(lms -> lms.lmsType)
                        .getOr(null));
    }

    @Override
    public Collection<RunningExamInfo> checkSEBRestriction(final ClientConnection cc, final Principal principal) {

        try {

            final String sebVersion = cc.sebVersion;
            final String sebOSName = cc.sebOSName;

            if (cc.examId == null) {

                return examSessionService
                        .getRunningExams(
                                cc.institutionId,
                                sebClientConfigDAO.getExamSelectionPredicate(principal.getName()))
                        .getOrThrow()
                        .stream()
                        .map(exam -> getForExam(cc, exam, false))
                        .toList();

            } else {

                final Exam exam = examSessionService
                        .getRunningExam(cc.examId)
                        .getOrThrow();

                RunningExamInfo forExam = getForExam(cc, exam, true);
                if (forExam == null) {
                    return null;
                }

                return Collections.singletonList(forExam);

            }

        } catch (Exception e) {
            log.error("Failed to check SEB Restriction: ", e);
            // skip check
            return null;
        }
    }

    private RunningExamInfo getForExam(final ClientConnection cc, final Exam exam, final boolean examSelected) {

        if (exam.allowedSEBVersions == null || exam.allowedSEBVersions.isEmpty()) {
            // no version restriction for this exam
            if (examSelected) {
                return null;
            } else {
                return getGrantedRunningExamInfo(exam);
            }
        }

        final SEBVersionInfo sebVersionRestrictedInfo = getSEBVersionRestrictedInfo(exam);

        if (cc.sebVersion == null || cc.sebOSName == null) {
            // if we do not have this version and os at this time, SEB is restricted
            return restrictVersion(exam, cc, sebVersionRestrictedInfo.allowedSEBVersionsInfo, examSelected);
        }

        // apply version check
        if (!sebClientVersionService.isAllowedVersion(
                cc.sebVersion,
                cc.sebOSName,
                sebVersionRestrictedInfo.allowedSEBVersions)) {

            // restrict version for this exam
            return restrictVersion(exam, cc, sebVersionRestrictedInfo.allowedSEBVersionsInfo, examSelected);
        } else {

            // grant check (mark as granted) and go on...
            clientConnectionDAO
                    .saveSEBClientVersionCheckStatus(cc.id, true)
                    .onError(error -> log.error("Failed to mark ClientConnection for SEB Client Version Check granted: {}", error.getMessage()));

            if (examSelected) {
                return null;
            } else {
                return getGrantedRunningExamInfo(exam);
            }
        }
    }



    private RunningExamInfo restrictVersion(
            final Exam exam,
            final ClientConnection cc,
            final String allowedSEBVersions,
            final boolean examSelected) {

        if (examSelected) {
            // If exam has been selected we can store the check for this connection as not granted
            // If exam has not been selected this is for the Exam list that is responded and we skip to save the grant for later when exam is selected
            clientConnectionDAO
                    .saveSEBClientVersionCheckStatus(cc.id, false)
                    .onError(error -> log.error("Failed to mark ClientConnection for SEB Client Version Check not granted: {}", error.getMessage()));
        }

        final String examName = exam != null ? String.valueOf(exam.name) : "--";

        final String redirectURL = String.format(
                REDIRECT_URL_TEMPLATE,
                webserviceInfo.getExternalServerURL(),
                examName,
                cc.sebVersion != null ? cc.sebVersion : "--",
                allowedSEBVersions);


        return new RunningExamInfo(
                exam != null ? String.valueOf(exam.id) : "--",
                examName,
                redirectURL,
                "LMSType");
    }



    // TODO cache these SEBVersionInfo
    private SEBVersionInfo getSEBVersionRestrictedInfo(final Exam exam) {
        if (exam != null) {
            return new SEBVersionInfo(exam.allowedSEBVersions, createAllowedInfo(exam));
        } else {
            return new SEBVersionInfo(Collections.emptyList(), "Exam was not selected");
        }
    }

    private String createAllowedInfo(final Exam exam) {
        if (exam != null && exam.allowedSEBVersions != null && !exam.allowedSEBVersions.isEmpty()) {

            return exam.allowedSEBVersions
                    .stream()
                    .map(v -> v.wholeVersionString)
                    .reduce(
                            "",
                            (acc, v) -> acc + v + ", " );

        }

        return "--";
    }

    private static final class SEBVersionInfo {

        final List<AllowedSEBVersion> allowedSEBVersions;
        final String allowedSEBVersionsInfo;


        private SEBVersionInfo(
                final List<AllowedSEBVersion> allowedSEBVersions,
                final String allowedSEBVersionsInfo) {

            this.allowedSEBVersions = allowedSEBVersions;
            this.allowedSEBVersionsInfo = allowedSEBVersionsInfo;
        }
    }
}
