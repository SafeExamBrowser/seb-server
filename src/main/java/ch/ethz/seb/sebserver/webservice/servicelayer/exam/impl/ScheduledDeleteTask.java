package ch.ethz.seb.sebserver.webservice.servicelayer.exam.impl;

import ch.ethz.seb.sebserver.SEBServerInitEvent;
import ch.ethz.seb.sebserver.gbl.Constants;
import ch.ethz.seb.sebserver.gbl.async.AsyncServiceSpringConfig;
import ch.ethz.seb.sebserver.gbl.model.exam.ScheduledDelete;
import ch.ethz.seb.sebserver.gbl.model.exam.ScheduledDeleteInfo;
import ch.ethz.seb.sebserver.gbl.util.Utils;
import ch.ethz.seb.sebserver.webservice.WebserviceInfo;
import ch.ethz.seb.sebserver.webservice.servicelayer.bulkaction.impl.DeleteExamAction;
import ch.ethz.seb.sebserver.webservice.servicelayer.dao.ExamDAO;
import ch.ethz.seb.sebserver.webservice.servicelayer.dao.ScheduledDeleteDAO;
import ch.ethz.seb.sebserver.webservice.servicelayer.session.impl.proctoring.ScreenProctoringAPIBinding;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Lazy;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.Executor;

@Lazy
@Component
public class ScheduledDeleteTask {

    private static final Logger log = LoggerFactory.getLogger(ScheduledDeleteTask.class);
    public static final Logger INIT_LOGGER = LoggerFactory.getLogger("SERVICE_INIT");

    private final TaskScheduler taskScheduler;
    private final WebserviceInfo webserviceInfo;
    private final Executor deletionExecutor;
    private final DeleteExamAction deleteExamAction;
    private final ScheduledDeleteDAO scheduledDeleteDAO;
    private final ScreenProctoringAPIBinding screenProctoringAPIBinding;
    private final ExamDAO examDAO;

    public ScheduledDeleteTask(
            final TaskScheduler taskScheduler,
            final WebserviceInfo webserviceInfo,
            @Qualifier(AsyncServiceSpringConfig.EXECUTOR_BEAN_NAME) final Executor deletionExecutor,
            final DeleteExamAction deleteExamAction,
            final ScheduledDeleteDAO scheduledDeleteDAO,
            final ScreenProctoringAPIBinding screenProctoringAPIBinding,
            final ExamDAO examDAO) {

        this.taskScheduler = taskScheduler;
        this.webserviceInfo = webserviceInfo;
        this.deletionExecutor = deletionExecutor;
        this.deleteExamAction = deleteExamAction;
        this.scheduledDeleteDAO = scheduledDeleteDAO;
        this.screenProctoringAPIBinding = screenProctoringAPIBinding;
        this.examDAO = examDAO;
    }

    @EventListener(SEBServerInitEvent.class)
    public void init() {

        INIT_LOGGER.info("---->");
        INIT_LOGGER.info("----> Initialize Scheduled delete service");
        INIT_LOGGER.info("---->   Update every half hour to check and process pending deletions");
        INIT_LOGGER.info("---->");

        // update triggered every hour...
        this.taskScheduler.scheduleAtFixedRate(
                this::update,
                Instant.now().plusMillis(Constants.HOUR_IN_MILLIS / 2),
                Duration.ofMillis(Constants.HOUR_IN_MILLIS / 2));

    }

    private void update() {

        // only master service should do scheduled deletion
        if (!webserviceInfo.isMaster()) {
            return;
        }

        scheduledDeleteDAO
                .getPendingScheduledDelete()
                .onError(error -> log.error("Failed to update scheduled deletion due to unexpected error: ", error))
                .onSuccess(delete -> {
                    try {

                        if (delete.isNull()) {
                            return;
                        }

                        // check if task is already running on SPS
                        final ScheduledDelete element = delete.getElement();
                        if (element.state() == ScheduledDelete.State.SPS_RUNNING) {
                            // it is already running on SPS site so we directly go to SPS pre-processing stage here
                            this.preProcessSPS(element);
                            return;
                        }

                        final long scheduleTime = element.scheduleTime();
                        final long now = Utils.getMillisecondsNow();

                        if (log.isDebugEnabled()) {
                            log.debug("Found pending ScheduledDelete with schedule time: {} now is: {}", scheduleTime, now);
                        }

                        if (scheduleTime <= now) {
                            log.info("Found scheduled deletion for processing: {}", element);
                            this.preProcessSPS(element);
                        } else {
                            if (log.isDebugEnabled()) {
                                log.debug("Skip scheduled delete since schedule time is still older");
                            }
                        }

                    } catch (Exception e) {
                        log.error("Failed to verify pending ScheduledDelete: {}", e.getMessage());
                    }
                });
    }

    void preProcessSPS(final ScheduledDelete delete) {
        try {

            if (delete.spsId() != null) {

                // check if SPS still PENDING or RUNNING if so mark the scheduled delete as SPS_PENDING
                // and skip SEB Server deletion as long as SPS deletion is not done
                final ScheduledDelete scheduledDelete = screenProctoringAPIBinding
                        .getScheduledDeleteById(delete.spsId())
                        .getOrThrow();

                if (scheduledDelete.state() != ScheduledDelete.State.FINISHED) {

                    // mark this as SPS Pending if not already marked
                    if (delete.state() == ScheduledDelete.State.PENDING) {
                        if (!scheduledDeleteDAO.markAs(delete.id(), ScheduledDelete.State.SPS_RUNNING)) {
                            log.warn("Failed to mark scheduled delete as marked for SPS_PENDING");
                        }
                    }
                    return;
                }
            }

            // in this case there is no SPS data, or it is already deleted, and we can process the deletion
            deletionExecutor.execute(() -> process(delete));

        } catch (Exception e) {
            log.error("Failed to process scheduled delete: {} cause: ", delete, e);
        }
    }

    void process(final ScheduledDelete delete) {
        try {

            log.info("*******************************");
            log.info("**** Start processing scheduled deletion: {}", delete);

            if (!scheduledDeleteDAO.startProcessing(delete.id())) {
                log.error("**** Failed to mark scheduled deletion as RUNNING. Skip it and retry next time");
                return;
            }

            log.info("**** Scheduled deletion marked as RUNNING");

            delete.info().forEach(this::processOne);

            scheduledDeleteDAO.endProcessing(delete.id());

            log.info("**** Finished scheduled deletion: {}", delete.id());
            log.info("*******************************");

        } catch (Exception e) {
            log.error("Failed to process scheduled delete: {} cause: {}", delete, e.getMessage());
        }
    }

    void processOne(final ScheduledDeleteInfo delete) {
        try {

            log.info("**** Start delete Exam: {}", delete.examUUID());
            scheduledDeleteDAO.startSingleDeletion(delete.id());

            examDAO
                    .byModelId(delete.examUUID())
                    .map(deleteExamAction::scheduledDeleteExamInternal)
                    .onError(error -> scheduledDeleteDAO.endSingleDeletion(delete.id(), error.getMessage()))
                    .onSuccess(exam -> scheduledDeleteDAO.endSingleDeletion(delete.id(), null));

            log.info("**** Finished delete Exam: {}", delete.examUUID());


        } catch (Exception e) {
            log.error("Failed to process scheduled single Exam delete: {} cause: {}", delete, e.getMessage());
        }
    }
}
