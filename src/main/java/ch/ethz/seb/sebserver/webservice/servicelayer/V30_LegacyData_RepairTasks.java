package ch.ethz.seb.sebserver.webservice.servicelayer;

import ch.ethz.seb.sebserver.SEBServerInit;
import ch.ethz.seb.sebserver.SEBServerInitEvent;
import ch.ethz.seb.sebserver.gbl.Constants;
import ch.ethz.seb.sebserver.gbl.api.EntityType;
import ch.ethz.seb.sebserver.gbl.async.AsyncServiceSpringConfig;
import ch.ethz.seb.sebserver.gbl.model.exam.Exam;
import ch.ethz.seb.sebserver.gbl.model.user.UserInfo;
import ch.ethz.seb.sebserver.gbl.model.user.UserRole;
import ch.ethz.seb.sebserver.gbl.util.Utils;
import ch.ethz.seb.sebserver.webservice.WebserviceInfo;
import ch.ethz.seb.sebserver.webservice.datalayer.batis.model.AdditionalAttributeRecord;
import ch.ethz.seb.sebserver.webservice.servicelayer.dao.AdditionalAttributesDAO;
import ch.ethz.seb.sebserver.webservice.servicelayer.dao.ExamDAO;
import ch.ethz.seb.sebserver.webservice.servicelayer.dao.UserDAO;
import ch.ethz.seb.sebserver.webservice.servicelayer.exam.ExamTemplateService;
import org.apache.commons.lang3.BooleanUtils;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Lazy;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.util.EnumSet;
import java.util.concurrent.Executor;

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

    private static final String USER_ROLE_REPAIR_DONE_ATTR_NAME = "V3_USER_ROLE_REPAIR_DONE";
    private static final String EXAM_REPAIR_DONE_ATTR_NAME = "V3_EXAM_REPAIR_DONE";
    private static final String EXAM_TEMPLATE_REPAIR_DONE_ATTR_NAME = "V3_EXAM_TEMPLATE_REPAIR_DONE";

    private final AdditionalAttributesDAO additionalAttributesDAO;
    private final ExamDAO examDAO;
    private final UserDAO userDAO;
    private final ExamTemplateService examTemplateService;
    private final WebserviceInfo webserviceInfo;
    private final Executor executor;

    public V30_LegacyData_RepairTasks(
            final AdditionalAttributesDAO additionalAttributesDAO, final ExamDAO examDAO,
            final UserDAO userDAO,
            final ExamTemplateService examTemplateService,
            final WebserviceInfo webserviceInfo,
            final @Qualifier(AsyncServiceSpringConfig.EXECUTOR_BEAN_NAME) Executor executor) {

        this.additionalAttributesDAO = additionalAttributesDAO;
        this.examDAO = examDAO;
        this.userDAO = userDAO;
        this.examTemplateService = examTemplateService;
        this.webserviceInfo = webserviceInfo;
        this.executor = executor;
    }


    @EventListener(SEBServerInitEvent.class)
    private void init() {
        executor.execute(this::repair);
    }

    private void repair() {
        if (!isMaster()) {
            return;
        }

        SEBServerInit.INIT_LOGGER.info("------>");
        SEBServerInit.INIT_LOGGER.info("------> Check to apply reparation task for legacy Data for version 3.0");
        SEBServerInit.INIT_LOGGER.info("------>");

        repairUserRoles();
        repairExams();
        repairExamTemplates();
    }

    private void repairUserRoles() {
        try {

            try {

                AdditionalAttributeRecord attr = additionalAttributesDAO
                        .getAdditionalAttribute(EntityType.USER, 0L, USER_ROLE_REPAIR_DONE_ATTR_NAME)
                        .getOr(null);

                if (attr != null && BooleanUtils.toBoolean(attr.getValue())) {
                    SEBServerInit.INIT_LOGGER.info("--------> User Roles already repaired, skip repair task.");
                    return;
                }

            } catch (Exception e) {
                SEBServerInit.INIT_LOGGER.error("------> Failed to check if User Roles repair task already applied. Cause: ", e);
                return;
            }

            SEBServerInit.INIT_LOGGER.info("------> Start repairing legacy User Roles and add subsequent roles for InstitutionalAdmin and ExamAdmin if needed");

            userDAO
                    .getAllActiveUsersUUID()
                    .onSuccess(all -> all.forEach(userId -> {
                        final UserInfo user = userDAO.byModelId(userId).getOr(null);
                        if (user != null) {
                            if (user.roles.contains(UserRole.INSTITUTIONAL_ADMIN.name()) &&
                                    (!user.roles.contains(UserRole.EXAM_ADMIN.name()) ||
                                     !user.roles.contains(UserRole.EXAM_SUPPORTER.name()))) {

                                final EnumSet<UserRole> roles = EnumSet.of(
                                        UserRole.INSTITUTIONAL_ADMIN,
                                        UserRole.EXAM_ADMIN,
                                        UserRole.EXAM_SUPPORTER);

                                updateUserRoles(user, roles);

                            } else if (user.roles.contains(UserRole.EXAM_ADMIN.name()) &&
                                    !user.roles.contains(UserRole.EXAM_SUPPORTER.name())) {

                                final EnumSet<UserRole> roles = EnumSet.of(
                                        UserRole.EXAM_ADMIN,
                                        UserRole.EXAM_SUPPORTER);

                                updateUserRoles(user, roles);
                            }
                        }
                    }))
                    .getOrThrow();

            additionalAttributesDAO.saveAdditionalAttribute(EntityType.USER, 0L, USER_ROLE_REPAIR_DONE_ATTR_NAME, "true");

            SEBServerInit.INIT_LOGGER.info("------> Successfully finished repairing legacy User Roles and ");

        } catch (Exception e) {
            SEBServerInit.INIT_LOGGER.error("------> !!! Failed to repair legacy User Roles, error: ", e);
        }
    }

    /** Since 3.0 Every new imported Exam has an Exam Configuration from import process
     * bot legacy Exams might not have an Exam Config since it was possible to import an exam without Exam Config.
     * This repairs such legacy data by  going once through all Exam in status UpComing, Running and Finished
     * and create a default Exam Configuration for the Exam if no Exam Configuration exists already for the exam */
    private void repairExams() {
        try {

            try {

                AdditionalAttributeRecord attr = additionalAttributesDAO
                        .getAdditionalAttribute(EntityType.EXAM, 0L, EXAM_REPAIR_DONE_ATTR_NAME)
                        .getOr(null);

                if (attr != null && BooleanUtils.toBoolean(attr.getValue())) {
                    SEBServerInit.INIT_LOGGER.info("--------> Exams already repaired, skip repair task.");
                    return;
                }

            } catch (Exception e) {
                SEBServerInit.INIT_LOGGER.error("------> Failed to check if Exam repair task already applied. Cause: ", e);
                return;
            }

            SEBServerInit.INIT_LOGGER.info("------> Start repairing legacy Exams add default Exam Configuration for all active Exams that has none yet...");

            // get all active exams and check for each if it has a Exam Config applied
            examDAO
                    .allNoneArchivedExamIds()
                    .onSuccess(ids -> ids.forEach(examId -> {
                        try {
                            final Exam exam = examDAO
                                    .byPK(examId)
                                    .getOrThrow();

                            examTemplateService.repairExamConfiguration(exam);

                        } catch (Exception e) {
                            SEBServerInit.INIT_LOGGER.error("------> !!! Failed to apply default Exam Configuration for Exam: {} cause:", examId, e);
                        }
                    }))
                    .onError(error -> SEBServerInit.INIT_LOGGER.error("--------> !!! Failed to get exam ids: ", error));

            additionalAttributesDAO.saveAdditionalAttribute(EntityType.EXAM, 0L, EXAM_REPAIR_DONE_ATTR_NAME, "true");

            SEBServerInit.INIT_LOGGER.info("------> Successfully finished repairing legacy Exams add default Exam Configuration for all active Exams");

        } catch (Exception e) {
            SEBServerInit.INIT_LOGGER.error("------> !!! Failed to repair legacy Exams with no Exam Configuration due to unexpected error: ", e);
        }
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
                    SEBServerInit.INIT_LOGGER.info("--------> ExamsTemplates already repaired, skip repair task.");
                    return;
                }

            } catch (Exception e) {
                SEBServerInit.INIT_LOGGER.error("------> Failed to check if Exam Template repair task already applied. Cause: ", e);
                return;
            }

            SEBServerInit.INIT_LOGGER.info("------> Start repairing legacy Exam Templates add set 'apply to groups' SPS collecting strategy if not set already...");

            // get all ExamTemplates and check for each if collection strategy needs to be changed
            examTemplateService
                    .getAllIds()
                    .getOrThrow()
                    .forEach(examTemplateService::fixForV3);

            additionalAttributesDAO.saveAdditionalAttribute(EntityType.EXAM_TEMPLATE, 0L, EXAM_TEMPLATE_REPAIR_DONE_ATTR_NAME, "true");

            SEBServerInit.INIT_LOGGER.info("------> Successfully finished repairing legacy ExamTemplates and set SPS grouping strategy 'APPLY_SEB_GROUPS'");

        } catch (Exception e) {
            SEBServerInit.INIT_LOGGER.error("------> !!! Failed to repair legacy Exam Templates SPS collecting strategy: ", e);
        }
    }

    private boolean isMaster() {
        final long waitUntil = Utils.getMillisecondsNow() + Constants.MINUTE_IN_MILLIS;

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
            SEBServerInit.INIT_LOGGER.info("------> This service has not become master. Skip repair.");
            return false;
        }

        return true;
    }

    private void updateUserRoles(final UserInfo user, final EnumSet<UserRole> roles) {
        SEBServerInit.INIT_LOGGER.info("--------> Update User Roles for user: {}", user);

        userDAO
                .pkForModelId(user.getModelId())
                .onSuccess(id -> userDAO.updateUserRoles(id, roles));

        final UserInfo updatedUser = userDAO.byModelId(user.getModelId()).getOr(null);
        if (updatedUser != null) {
            SEBServerInit.INIT_LOGGER.info("--------> Successfully update User Roles for user: {}", updatedUser);
        }
    }
}
