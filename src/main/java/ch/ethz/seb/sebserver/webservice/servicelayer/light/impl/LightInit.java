package ch.ethz.seb.sebserver.webservice.servicelayer.light.impl;


import ch.ethz.seb.sebserver.SEBServerInitEvent;
import ch.ethz.seb.sebserver.gbl.model.sebconfig.SEBClientConfig;
import ch.ethz.seb.sebserver.gbl.util.Result;
import ch.ethz.seb.sebserver.gbl.util.Utils;
import ch.ethz.seb.sebserver.webservice.servicelayer.dao.FilterMap;
import ch.ethz.seb.sebserver.webservice.servicelayer.dao.SEBClientConfigDAO;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.context.annotation.Lazy;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.stream.Collectors;

@Lazy
@Service
@ConditionalOnExpression("'${sebserver.webservice.light.setup}'.equals('true')")
public class LightInit {

    private final SEBClientConfigDAO sebClientConfigDAO;

    public LightInit(final SEBClientConfigDAO sebClientConfigDAO){
            this.sebClientConfigDAO = sebClientConfigDAO;
    }



    @EventListener(SEBServerInitEvent.class)
    public void init() {
//        if(isConnectionConfigAbsent()){
//            this.sebClientConfigDAO.createNew(createLightConnectionConfiguration()).getOrThrow();
//        }
    }

    private boolean isConnectionConfigAbsent() {
        Collection<SEBClientConfig> connectionConfigs = this.sebClientConfigDAO
                .all(null, null)
                .getOrThrow();

        if(connectionConfigs.size() == 0){
            return true;
        }

        return false;
    }

    private SEBClientConfig createLightConnectionConfiguration(){
        return new SEBClientConfig(
                1L,
                1L,
                "light-config",
                SEBClientConfig.ConfigPurpose.CONFIGURE_CLIENT,
                1000L,
                SEBClientConfig.VDIType.NO,
                null,
                null,
                null,
                false,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                false,
                true,
                null,
                null,
                null);
    }



}
