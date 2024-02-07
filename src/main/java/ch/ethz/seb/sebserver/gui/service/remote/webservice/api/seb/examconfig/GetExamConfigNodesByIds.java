package ch.ethz.seb.sebserver.gui.service.remote.webservice.api.seb.examconfig;

import ch.ethz.seb.sebserver.gbl.api.API;
import ch.ethz.seb.sebserver.gbl.api.EntityType;
import ch.ethz.seb.sebserver.gbl.model.sebconfig.ConfigurationNode;
import ch.ethz.seb.sebserver.gbl.profile.GuiProfile;
import ch.ethz.seb.sebserver.gui.service.remote.webservice.api.RestCall;
import com.fasterxml.jackson.core.type.TypeReference;
import org.springframework.context.annotation.Lazy;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;

import java.util.Collection;

@Lazy
@Component
@GuiProfile
public class GetExamConfigNodesByIds extends RestCall<Collection<ConfigurationNode>> {

    public GetExamConfigNodesByIds() {
        super(new TypeKey<>(
                CallType.GET_LIST,
                EntityType.CONFIGURATION_NODE,
                new TypeReference<Collection<ConfigurationNode>>() {
                }),
                HttpMethod.GET,
                MediaType.APPLICATION_FORM_URLENCODED,
                API.CONFIGURATION_NODE_ENDPOINT + API.LIST_PATH_SEGMENT
        );
    }
}
