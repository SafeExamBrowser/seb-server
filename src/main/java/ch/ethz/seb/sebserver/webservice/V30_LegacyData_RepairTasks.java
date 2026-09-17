package ch.ethz.seb.sebserver.webservice;

import ch.ethz.seb.sebserver.SEBServerInit;
import ch.ethz.seb.sebserver.gbl.Constants;
import ch.ethz.seb.sebserver.gbl.api.EntityType;
import ch.ethz.seb.sebserver.gbl.async.AsyncServiceSpringConfig;
import ch.ethz.seb.sebserver.gbl.model.exam.Exam;
import ch.ethz.seb.sebserver.gbl.model.user.UserInfo;
import ch.ethz.seb.sebserver.gbl.model.user.UserRole;
import ch.ethz.seb.sebserver.gbl.util.Utils;
import ch.ethz.seb.sebserver.webservice.datalayer.batis.model.AdditionalAttributeRecord;
import ch.ethz.seb.sebserver.webservice.servicelayer.dao.AdditionalAttributesDAO;
import ch.ethz.seb.sebserver.webservice.servicelayer.dao.ExamDAO;
import ch.ethz.seb.sebserver.webservice.servicelayer.dao.UserDAO;
import ch.ethz.seb.sebserver.webservice.servicelayer.exam.ExamTemplateService;
import ch.ethz.seb.sebserver.webservice.servicelayer.session.ScreenProctoringService;
import org.apache.commons.lang3.BooleanUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Lazy;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.util.EnumSet;
import java.util.Set;
import java.util.stream.Collectors;

/** 1. Since 3.0 Every new imported Exam has an Exam Configuration from import process
 * bot legacy Exams might not have an Exam Config since it was possible to import an exam without Exam Config.
 * This repairs such legacy data by  going once through all Exam in status UpComing, Running and Finished
 * and create a default Exam Configuration for the Exam if no Exam Configuration exists already for the exam
 * <p>
 * 2. Since 3.0 The SPS collection strategy is only one left (apply to groups). For older Exam Template that sill has
 * "one group" collection strategy, this repair task sets the "apply to groups" strategy for these Exam Templates once. */
@Lazy
@Component
public class V30_LegacyData_RepairTasks {

    public static final Logger REPAIR_LOGGER = LoggerFactory.getLogger("ch.ethz.seb.SEB_SERVER_REPAIR");

    private static final String USER_ROLE_REPAIR_DONE_ATTR_NAME = "V3_USER_ROLE_REPAIR_DONE";
    private static final String EXAM_REPAIR_DONE_ATTR_NAME = "V3_EXAM_REPAIR_DONE";
    private static final String EXAM_TEMPLATE_REPAIR_DONE_ATTR_NAME = "V3_EXAM_TEMPLATE_REPAIR_DONE";

    private final AdditionalAttributesDAO additionalAttributesDAO;
    private final ExamDAO examDAO;
    private final UserDAO userDAO;
    private final ExamTemplateService examTemplateService;
    private final WebserviceInfo webserviceInfo;
    private final ScreenProctoringService screenProctoringService;

    public V30_LegacyData_RepairTasks(
            final AdditionalAttributesDAO additionalAttributesDAO, final ExamDAO examDAO,
            final UserDAO userDAO,
            final ExamTemplateService examTemplateService,
            final WebserviceInfo webserviceInfo,
            final ScreenProctoringService screenProctoringService) {

        this.additionalAttributesDAO = additionalAttributesDAO;
        this.examDAO = examDAO;
        this.userDAO = userDAO;
        this.examTemplateService = examTemplateService;
        this.webserviceInfo = webserviceInfo;
        this.screenProctoringService = screenProctoringService;
    }

    @Async(AsyncServiceSpringConfig.EXECUTOR_BEAN_NAME)
    void repairLegacyDataForV30() {

        if (webserviceInfo.hasProfile("test")) {
            REPAIR_LOGGER.info("No migration applies for test profile");
            return;
        }

        REPAIR_LOGGER.info("---->");
        REPAIR_LOGGER.info("----> Check to apply reparation task for legacy Data for version 3.0");
        REPAIR_LOGGER.info("------> Wait to become master and SPS availability for at least 2 minutes\"");

        try {

            final long waitUntil = Utils.getMillisecondsNow() + 2 * Constants.MINUTE_IN_MILLIS;
            boolean master = this.webserviceInfo.isMaster();
            boolean timeUp = false;

            while (!(master || timeUp)) {
                try {
                    Thread.sleep(10 * Constants.SECOND_IN_MILLIS);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }

                master = this.webserviceInfo.isMaster();
                timeUp = Utils.getMillisecondsNow() > waitUntil;
            }

            if (!master) {
                REPAIR_LOGGER.info("--------> This service has not become master. Skip legacy Data repair for version 3.0");
                return;
            }
        } catch (Exception e) {
            REPAIR_LOGGER.error("--------> !!! Failed to wait for becoming master: {}", e.getMessage());
            return;
        }

        repairUserRoles();
        repairExams();
        repairExamTemplates();
        syncSPSUsers();

    }

    private void repairUserRoles() {
        try {

            try {

                AdditionalAttributeRecord attr = additionalAttributesDAO
                        .getAdditionalAttribute(EntityType.USER, 0L, USER_ROLE_REPAIR_DONE_ATTR_NAME)
                        .getOr(null);

                if (attr != null && BooleanUtils.toBoolean(attr.getValue())) {
                    REPAIR_LOGGER.info("------> User Roles already repaired, skip repair task.");
                    return;
                }

            } catch (Exception e) {
                REPAIR_LOGGER.error("------> !!! Failed to check if User Roles repair task already applied. Cause: ", e);
                return;
            }

            REPAIR_LOGGER.info("------> ");
            REPAIR_LOGGER.info("------> Start repairing legacy User Roles and add subsequent roles for InstitutionalAdmin and ExamAdmin if needed");

            userDAO
                    .getAllActiveUsersUUID()
                    .onSuccess(all -> all.forEach(userId -> {
                        final UserInfo user = userDAO.byModelId(userId).getOr(null);
                        if (user != null) {
                            if (user.roles.contains(UserRole.INSTITUTIONAL_ADMIN.name()) &&
                                    (!user.roles.contains(UserRole.EXAM_ADMIN.name()) ||
                                     !user.roles.contains(UserRole.EXAM_SUPPORTER.name()))) {

                                REPAIR_LOGGER.info(
                                        "--------> Found Institutional Admin User with missing roles. Add missing roles: {} : {}",
                                        user.username,
                                        user.uuid);

                                final EnumSet<UserRole> roles = EnumSet.of(
                                        UserRole.INSTITUTIONAL_ADMIN,
                                        UserRole.EXAM_ADMIN,
                                        UserRole.EXAM_SUPPORTER);

                                if (user.roles.contains(UserRole.SEB_SERVER_ADMIN.name())) {
                                    roles.add(UserRole.SEB_SERVER_ADMIN);
                                }

                                updateUserRoles(user, roles);

                            } else if (user.roles.contains(UserRole.EXAM_ADMIN.name()) &&
                                    !user.roles.contains(UserRole.EXAM_SUPPORTER.name())) {

                                REPAIR_LOGGER.info(
                                        "--------> Found Exam Admin User with missing roles. Add missing roles: {} : {}",
                                        user.username,
                                        user.uuid);

                                final EnumSet<UserRole> roles = EnumSet.of(
                                        UserRole.EXAM_ADMIN,
                                        UserRole.EXAM_SUPPORTER);

                                if (user.roles.contains(UserRole.SEB_SERVER_ADMIN.name())) {
                                    roles.add(UserRole.SEB_SERVER_ADMIN);
                                }

                                updateUserRoles(user, roles);
                            }
                        }
                    }))
                    .getOrThrow();

            additionalAttributesDAO.saveAdditionalAttribute(EntityType.USER, 0L, USER_ROLE_REPAIR_DONE_ATTR_NAME, "true");

            REPAIR_LOGGER.info("------> Successfully finished repairing legacy User Roles");

        } catch (Exception e) {
            REPAIR_LOGGER.error("------> !!! Failed to repair legacy User Roles, error: ", e);
        }
    }

    /** Since 3.0 Every new imported Exam has an Exam Configuration from import process
     * bot legacy Exams might not have an Exam Config since it was possible to import an exam without Exam Config.
     * This repairs such legacy data by  going once through all Exam in status UpComing, Running and Finished
     * and create a default Exam Configuration for the Exam if no Exam Configuration exists already for the exam
     * <p>
     * Secondly this task synchronize the Exam data with the SPS Exam data if there is an SPS Exam.
     * This ensures that every SPS Exam has a valid institutional identifier. This is needed for Institutional
     * Administrators to be able to see all SPS data for their institutions.
     * <p>
     * And third, this task checks all assigned supervisors of the Exam if they still are valid after User Role
     * Repair. */
    private void repairExams() {
        try {

            try {

                AdditionalAttributeRecord attr = additionalAttributesDAO
                        .getAdditionalAttribute(EntityType.EXAM, 0L, EXAM_REPAIR_DONE_ATTR_NAME)
                        .getOr(null);

                if (attr != null && BooleanUtils.toBoolean(attr.getValue())) {
                    REPAIR_LOGGER.info("------> Exams already repaired, skip repair task.");
                    return;
                }

            } catch (final Exception e) {
                REPAIR_LOGGER.error("------> !!! Failed to check if Exam repair task already applied. Cause: ", e);
                return;
            }

            REPAIR_LOGGER.info("------>");
            REPAIR_LOGGER.info("------> Start repairing legacy Exams data...");

            // get all active exams and check for each if it has an Exam Config applies
            examDAO
                    .allExamIds()
                    .onSuccess(ids -> ids.forEach(examId -> {

                            examDAO
                                    .byPK(examId)
                                    .onSuccess(exam -> {

                                        REPAIR_LOGGER.info("--------> Repair exam: {} : {}", exam.id, exam.externalId);

                                        repairExamConfig(exam);
                                        repairExamSupervisors(exam);
                                        repairExamSyncSPS(exam);

                                    });


                    }))
                    .onError(error -> REPAIR_LOGGER.error("--------> !!! Failed to get exam ids: ", error));

            additionalAttributesDAO.saveAdditionalAttribute(EntityType.EXAM, 0L, EXAM_REPAIR_DONE_ATTR_NAME, "true");

            REPAIR_LOGGER.info("------> Successfully finished repairing legacy Exams add default Exam Configuration for all active Exams");

        } catch (final Exception e) {
            REPAIR_LOGGER.error("------> !!! Failed to repair legacy Exams with no Exam Configuration due to unexpected error: ", e);
        }
    }

    /** Synchronize all active user accounts with SPS user accounts. This is done on every startup
     * when a service becomes master. */
    private void syncSPSUsers() {

        try {

            if (!screenProctoringService.isAvailable()) {
                REPAIR_LOGGER.warn("------> SPS Service is not available. Skip User Account synchronization");
                return;
            }

            REPAIR_LOGGER.info("------>");
            REPAIR_LOGGER.info("------> Start SPS User Account synchronization");

            userDAO
                    .getAllActiveUsersUUID()
                    .onSuccess(all -> all.forEach(screenProctoringService::synchronizeSPSUserWait))
                    .getOrThrow();

            REPAIR_LOGGER.info("--------> Finished initial SPS User Account synchronisation successfully");
        } catch (Exception e) {
            REPAIR_LOGGER.error("--------> !!! Failed to synchronize SPS Users, error: {}", e.getMessage());
        }
    }

    private void repairExamConfig(final Exam exam) {
        try {

            if (exam.status == Exam.ExamStatus.ARCHIVED) {
                return;
            }

            if (examTemplateService.repairExamConfiguration(exam)) {
                REPAIR_LOGGER.info("----------> Successfully repaired Exam Configuration for Exam: {} : {}", exam.id, exam.externalId);
            }

        } catch (final Exception e) {
            REPAIR_LOGGER.error("----------> !!! Failed to apply default Exam Configuration for Exam: {} cause:", exam.externalId, e);
        }
    }

    private void repairExamSupervisors(final Exam exam) {
        try {

            if (exam.supporter != null && !exam.supporter.isEmpty()) {

                final Set<String> validSupporter = exam.supporter
                        .stream()
                        .filter(userDAO::isValidSupporterUser)
                        .collect(Collectors.toSet());

                if (exam.supporter.size() != validSupporter.size()) {

                    REPAIR_LOGGER.info(
                            "----------> Found invalid supervisor users for exam: {} : {} old supervisor UUIDs {}, new supervisor UUIDs {}",
                            exam.id,
                            exam.externalId,
                            exam.supporter,
                            validSupporter);

                    examDAO.updateSupporterAccounts(exam.id, validSupporter);
                }
            }

        } catch (Exception e) {
            REPAIR_LOGGER.error("----------> !!! Failed to repair supervisors for Exam: {} cause:", exam.externalId, e);
        }
    }

    private void repairExamSyncSPS(final Exam exam) {
        screenProctoringService
                .updateExamOnly(exam.id)
                .onError(error -> REPAIR_LOGGER.error(
                        "----------> !!! Failed to synchronize Exam with SPS: {}:{} cause: {}",
                        exam.id,
                        exam.externalId,
                        error.getMessage()))
                .onSuccess(e -> REPAIR_LOGGER.info(
                        "----------> Successfully synchronized Exam with SPS: {} : {}",
                        e.id,
                        e.externalId));
    }

    /** Since 3.0 The SPS collection strategy is only one left (apply to groups). For older Exam Template that sill has
     * "one group" collection strategy, this repair task sets the "apply to groups" strategy for these Exam Templates once.*/
    private void repairExamTemplates() {
        try {

            try {

                AdditionalAttributeRecord attr = additionalAttributesDAO
                        .getAdditionalAttribute(EntityType.EXAM_TEMPLATE, 0L, EXAM_TEMPLATE_REPAIR_DONE_ATTR_NAME)
                        .getOr(null);

                if (attr != null && BooleanUtils.toBoolean(attr.getValue())) {
                    REPAIR_LOGGER.info("------> ExamsTemplates already repaired, skip repair task.");
                    return;
                }

            } catch (Exception e) {
                REPAIR_LOGGER.error("------> !!! Failed to check if Exam Template repair task already applied. Cause: ", e);
                return;
            }

            REPAIR_LOGGER.info("------> Start repairing legacy Exam Templates add set 'apply to groups' SPS collecting strategy if not set already...");

            // get all ExamTemplates and check for each if collection strategy needs to be changed
            examTemplateService
                    .getAllIds()
                    .getOrThrow()
                    .forEach(examTemplateService::fixForV3);

            additionalAttributesDAO.saveAdditionalAttribute(EntityType.EXAM_TEMPLATE, 0L, EXAM_TEMPLATE_REPAIR_DONE_ATTR_NAME, "true");

            REPAIR_LOGGER.info("------> Successfully finished repairing legacy ExamTemplates and set SPS grouping strategy 'APPLY_SEB_GROUPS'");

        } catch (Exception e) {
            REPAIR_LOGGER.error("------> !!! Failed to repair legacy Exam Templates SPS collecting strategy: ", e);
        }
    }

    private void updateUserRoles(final UserInfo user, final EnumSet<UserRole> roles) {
        try {

            final Set<String> roleNames = roles.stream()
                    .map(UserRole::getName)
                    .collect(Collectors.toSet());

            userDAO
                    .pkForModelId(user.getModelId())
                    .onSuccess(id -> userDAO.updateUserRoles(id, roleNames));

            userDAO.byModelId(user.getModelId())
                    .onError(error -> REPAIR_LOGGER.error("------> !!! Failed to get updated user: {} cause: {}", user.uuid, error.getMessage()))
                    .onSuccess(uu -> REPAIR_LOGGER.info("------> Successfully update User Roles for user: {} new roles: {}", uu.username, uu.roles));

        } catch (Exception e) {
            REPAIR_LOGGER.error("--------> !!! Failed to update User Roles for user: {}, cause: {}", user.uuid, e.getMessage());
        }
    }
}
