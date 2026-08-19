package ch.ethz.seb.sebserver.webservice.servicelayer.exam.impl;

import ch.ethz.seb.sebserver.SEBServerInit;
import ch.ethz.seb.sebserver.SEBServerInitEvent;
import ch.ethz.seb.sebserver.gbl.Constants;
import ch.ethz.seb.sebserver.gbl.api.EntityType;
import ch.ethz.seb.sebserver.gbl.async.AsyncServiceSpringConfig;
import ch.ethz.seb.sebserver.gbl.model.exam.Exam;
import ch.ethz.seb.sebserver.gbl.util.Utils;
import ch.ethz.seb.sebserver.webservice.WebserviceInfo;
import ch.ethz.seb.sebserver.webservice.datalayer.batis.model.AdditionalAttributeRecord;
import ch.ethz.seb.sebserver.webservice.servicelayer.dao.AdditionalAttributesDAO;
import ch.ethz.seb.sebserver.webservice.servicelayer.dao.ExamDAO;
import ch.ethz.seb.sebserver.webservice.servicelayer.exam.ExamTemplateService;
import org.apache.commons.lang3.BooleanUtils;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Lazy;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.util.concurrent.Executor;

/** Since 3.0 Every new imported Exam has an Exam Configuration from import process
 * bot legacy Exams might not have an Exam Config since it was possible to import an exam without Exam Config.
 * This repairs such legacy data by  going once through all Exam in status UpComing, Running and Finished
 * and create a default Exam Configuration for the Exam if no Exam Configuration exists already for the exam*/
@Lazy
@Component
public class ExamV30URepairTask {

    private static final String REPAIR_DONE_ATTR_NAME = "V3_EXAM_REPAIR_DONE";

    private final AdditionalAttributesDAO additionalAttributesDAO;
    private final ExamDAO examDAO;
    private final ExamTemplateService examTemplateService;
    private final WebserviceInfo webserviceInfo;
    private final Executor executor;

    public ExamV30URepairTask(
            final AdditionalAttributesDAO additionalAttributesDAO, final ExamDAO examDAO,
            final ExamTemplateService examTemplateService,
            final WebserviceInfo webserviceInfo,
            final @Qualifier(AsyncServiceSpringConfig.EXECUTOR_BEAN_NAME) Executor executor) {

        this.additionalAttributesDAO = additionalAttributesDAO;
        this.examDAO = examDAO;
        this.examTemplateService = examTemplateService;
        this.webserviceInfo = webserviceInfo;
        this.executor = executor;
    }


    @EventListener(SEBServerInitEvent.class)
    private void init() {

        try {

            AdditionalAttributeRecord attr = additionalAttributesDAO
                    .getAdditionalAttribute(EntityType.EXAM, 0L, REPAIR_DONE_ATTR_NAME)
                    .getOr(null);

            if (attr != null && BooleanUtils.toBoolean(attr.getValue())) {
                return;
            }

        } catch (Exception e) {
            SEBServerInit.INIT_LOGGER.error("------> Failed to check if Exam repair task already applied. Cause: ", e);
            return;
        }

        SEBServerInit.INIT_LOGGER.info("------>");
        SEBServerInit.INIT_LOGGER.info("------> Check to apply reparation task for legacy Exams for version 3.0");
        SEBServerInit.INIT_LOGGER.info("--------> Wait to become master for at least one minutes");
        SEBServerInit.INIT_LOGGER.info("------>");

        executor.execute(this::syncSPSUsers);
    }

    private void syncSPSUsers() {
        try {
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
                SEBServerInit.INIT_LOGGER.info("------> This service has not become master. Skip Exam repair");
                return;
            }

            SEBServerInit.INIT_LOGGER.info("------> Start repairing legacy Exams and add default Exam Configuration for all active Exams that has none yet...");

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

            additionalAttributesDAO.saveAdditionalAttribute(EntityType.EXAM, 0L, REPAIR_DONE_ATTR_NAME, "true");

            SEBServerInit.INIT_LOGGER.info("------> Successfully finished repairing legacy Exams and add default Exam Configuration for all active Exams");

        } catch (Exception e) {
            SEBServerInit.INIT_LOGGER.error("------> !!! Failed to repair legacy Exams with no Exam Configuration due to unexpected error: ", e);
        }
    }
}
