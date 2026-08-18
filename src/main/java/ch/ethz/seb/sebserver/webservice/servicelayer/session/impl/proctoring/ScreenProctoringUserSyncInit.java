package ch.ethz.seb.sebserver.webservice.servicelayer.session.impl.proctoring;

import ch.ethz.seb.sebserver.SEBServerInit;
import ch.ethz.seb.sebserver.SEBServerInitEvent;
import ch.ethz.seb.sebserver.gbl.Constants;
import ch.ethz.seb.sebserver.gbl.async.AsyncServiceSpringConfig;
import ch.ethz.seb.sebserver.gbl.util.Utils;
import ch.ethz.seb.sebserver.webservice.WebserviceInfo;
import ch.ethz.seb.sebserver.webservice.servicelayer.dao.UserDAO;
import ch.ethz.seb.sebserver.webservice.servicelayer.session.ScreenProctoringService;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Lazy;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.util.concurrent.Executor;

@Lazy
@Component
public class ScreenProctoringUserSyncInit {

    private final WebserviceInfo webserviceInfo;
    private final UserDAO userDAO;
    private final ScreenProctoringService screenProctoringService;
    private final Executor executor;

    public ScreenProctoringUserSyncInit(
            final WebserviceInfo webserviceInfo,
            final UserDAO userDAO,
            final ScreenProctoringService screenProctoringService,
            final @Qualifier(AsyncServiceSpringConfig.EXECUTOR_BEAN_NAME) Executor executor) {

        this.webserviceInfo = webserviceInfo;
        this.userDAO = userDAO;
        this.screenProctoringService = screenProctoringService;
        this.executor = executor;
    }


    @EventListener(SEBServerInitEvent.class)
    private void init() {
        SEBServerInit.INIT_LOGGER.info("------>");
        SEBServerInit.INIT_LOGGER.info("------> Check SPS User Account synchronization");
        SEBServerInit.INIT_LOGGER.info("--------> Wait to become master and SPS availability for at least 3 minutes");
        SEBServerInit.INIT_LOGGER.info("------>");

        executor.execute(this::syncSPSUsers);
    }

    private void syncSPSUsers() {
        try {
            final long waitUntil = Utils.getMillisecondsNow() + 3 * Constants.MINUTE_IN_MILLIS;

            boolean master = this.webserviceInfo.isMaster();
            boolean spsAvailable = false;
            boolean timeUp = false;

            while (!(master && spsAvailable || timeUp)) {
                try {
                    Thread.sleep(10 * Constants.SECOND_IN_MILLIS);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }

                master = this.webserviceInfo.isMaster();
                if (master) {
                    spsAvailable = screenProctoringService.isAvailable();
                }

                timeUp = Utils.getMillisecondsNow() > waitUntil;
            }

            if (!master) {
                SEBServerInit.INIT_LOGGER.info("------> This service has not become master. Skip User Account synchronization");
                return;
            }

            if (!spsAvailable) {
                SEBServerInit.INIT_LOGGER.warn("------> SPS Service is not available. Skip User Account synchronization");
                return;
            }

            SEBServerInit.INIT_LOGGER.info("------> Start SPS User Account synchronization");

            userDAO
                    .getAllActiveUsersUUID()
                    .onSuccess(all -> all.forEach(screenProctoringService::synchronizeSPSUserWait))
                    .getOrThrow();

            SEBServerInit.INIT_LOGGER.info("--------> Finished initial SPS User Account synchronisation successfully");

        } catch (Exception e) {
            SEBServerInit.INIT_LOGGER.error("------> !!! Failed to apply SPS user synchronization due to unexpected error: ", e);
        }
    }
}
