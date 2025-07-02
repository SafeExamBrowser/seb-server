package ch.ethz.seb.sebserver.webservice.integration.api.rest.api.seb.examconfig;


import ch.ethz.seb.sebserver.gbl.api.API;
import ch.ethz.seb.sebserver.gbl.api.EntityType;
import ch.ethz.seb.sebserver.gbl.model.sebconfig.ConfigurationNode;
import ch.ethz.seb.sebserver.gbl.profile.GuiProfile;
import ch.ethz.seb.sebserver.webservice.integration.api.rest.api.RestCall;
import com.fasterxml.jackson.core.type.TypeReference;
import org.springframework.context.annotation.Lazy;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;

@Lazy
@Component
@GuiProfile
public class ResetImportQuitPassword extends RestCall<ConfigurationNode> {

    public ResetImportQuitPassword() {
        super(new TypeKey<>(
                        CallType.UNDEFINED,
                        EntityType.CONFIGURATION_NODE,
                        new TypeReference<ConfigurationNode>() {
                        }),
                HttpMethod.POST,
                MediaType.APPLICATION_FORM_URLENCODED,
                API.CONFIGURATION_NODE_ENDPOINT +
                        API.MODEL_ID_VAR_PATH_SEGMENT +
                        API.CONFIGURATION_SET_QUIT_PWD_PATH_SEGMENT);
    }
}
