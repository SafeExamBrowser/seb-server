package ch.ethz.seb.sebserver.gbl.model.user;

import java.util.Map;

import ch.ethz.seb.sebserver.gbl.model.Domain;
import ch.ethz.seb.sebserver.gbl.util.Utils;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public class UserFeatures {

    public static final String ATTR_DEFAULT = "missingFeatureDefault";
    public static final String ATTR_FEATURE_PRIVILEGES = "featurePrivileges";

    public enum Feature {
        ADMIN_INSTITUTION("admin.institution"),

        ADMIN_USER_ADMINISTRATION("admin.user.administration"),
        ADMIN_USER_ACCOUNT("admin.user.account"),
        ADMIN_AUDIT_LOGS("admin.auditlogs"),

        CONFIG_CONNECTION_CONFIGURATION("config.connection.configuration"),
        CONFIG_EXAM_CONFIGURATION("config.exam.configuration"),
        CONFIG_TEMPLATE("config.template"),
        CONFIG_CERTIFICATE("config.certificate"),
        LMS_SETUP( "lms.setup"),
        LMS_SETUP_TEST("lms.setup.type.MOCKUP"),
        LMS_SETUP_MOODLE("lms.setup.type.MOODLE"),
        LMS_SETUP_MOODLE_PLUGIN("lms.setup.type.MOODLE_PLUGIN"),
        LMS_SETUP_OPEN_EDX("lms.setup.type.OPEN_EDX"),
        LMS_SETUP_ANS("lms.setup.type.ANS_DELFT"),
        LMS_SETUP_OPEN_OLAT("lms.setup.type.OPEN_OLAT"),

        QUIZ_LOOKUP("lms.quiz.lookup"),

        EXAM_ADMIN("exam.administration"),
        EXAM_ASK("exam.ask"),
        EXAM_CONNECTION_CONFIG("exam.connection.config"),
        EXAM_SEB_RESTRICTION( "exam.seb.restriction"),
        EXAM_LIVE_PROCTORING("exam.seb.liveProctoring"),
        EXAM_NO_LMS("exam.noLMS"),

        EXAM_SCREEN_PROCTORING("exam.seb.screenProctoring"),
        EXAM_INDICATORS("exam.monitoring.indicators"),
        EXAM_SEB_CLIENT_GROUPS("exam.seb.clientgroups"),
        EXAM_TEMPLATE("exam.template"),

        MONITORING_RUNNING_EXAMS("monitoring.running.exams"),
        MONITORING_RUNNING_EXAM_DETAIL_VIEW("monitoring.running.exam.detailview"),
        MONITORING_RUNNING_EXAM_DETAIL_VIEW_LOG_EXPORT("monitoring.running.exam.detailview.logexport"),
        //more? ...
        MONITORING_RUNNING_EXAM_QUIT("monitoring.running.exam.quit"),

        MONITORING_RUNNING_EXAM_LOCKSCREEN("monitoring.running.exam.lockscreen"),

        MONITORING_RUNNING_EXAM_CANCEL_CON("monitoring.running.exam.cancel.connection"),

        MONITORING_RUNNING_EXAM_STATE_FILTER("monitoring.running.exam.state.filter"),
        MONITORING_RUNNING_EXAM_ISSUE_FILTER("monitoring.running.exam.issue.filter"),
        MONITORING_RUNNING_EXAM_CLIENT_FILTER("monitoring.running.exam.client.filter"),
        MONITORING_RUNNING_EXAM_LIVE_PROCTORING("monitoring.running.exam.live.proctoring"),
        MONITORING_RUNNING_EXAM_SCREEN_PROCTORING("monitoring.running.exam.screen.proctoring"),
        MONITORING_FINISHED_EXAMS("monitoring.finished.exams"),
        MONITORING_OVERALL_LOG_EXPORT("monitoring.overall.export"),



        ;

        public final String featureName;

        Feature(final String featureName) {
            this.featureName = featureName;
        }
    }

    @JsonProperty(Domain.USER.ATTR_ID)
    public final String userId;
    @JsonProperty(ATTR_DEFAULT)
    public final Boolean missingFeatureDefault;
    @JsonProperty(ATTR_FEATURE_PRIVILEGES)
    public final Map<String, Boolean> featurePrivileges;

    @JsonCreator
    public UserFeatures(
            @JsonProperty(Domain.USER.ATTR_ID) final String userId,
            @JsonProperty(ATTR_DEFAULT) final Boolean missingFeatureDefault,
            @JsonProperty(ATTR_FEATURE_PRIVILEGES) final Map<String, Boolean> featurePrivileges) {

        this.userId = userId;
        this.missingFeatureDefault = missingFeatureDefault;
        this.featurePrivileges = Utils.immutableMapOf(featurePrivileges);
    }

    public String getUserId() {
        return userId;
    }

    public Boolean getMissingFeatureDefault() {
        return missingFeatureDefault;
    }

    public Map<String, Boolean> getFeaturePrivileges() {
        return featurePrivileges;
    }

    public boolean isFeatureEnabled(final Feature feature) {
        return featurePrivileges.getOrDefault(feature.featureName, missingFeatureDefault);
    }

    public boolean isFeatureEnabled(final String featureName) {
        return featurePrivileges.getOrDefault(featureName, missingFeatureDefault);
    }
}
