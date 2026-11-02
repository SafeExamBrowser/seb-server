package ch.ethz.seb.sebserver.webservice.servicelayer.exam;

// TODO this should handle complex exam deletion and provide methods to schedule, monitor and report different
//      stages of exam deletion that probably can vary for different Exam deletion scenarios or features
//      See: https://jira.ethz.ch/browse/SEBSP-220
//
// Some dependencies to consider on Exam Deletion:
//   - Exam can be deleted from within SEB Server as well as triggered by the LMS Integration (Moodle)
//   - Exam deletion can probably have different aspects like deletion of on SEB server data or deletion with SPS data (TBD)
//   - Exam deletion has external dependencies that can take some or depend on networking
//        - LMS Integration
//        - SPS Data
//   - Exam has a lot of internal dependencies that must be deleted in the right order to be able to delete the Exam
//        - SEB Connections, Indicators, LiveIndicator Table, Groups, Configs, Additional Attributes, ...

public interface ExamDeletionService {
}
