package ch.ethz.seb.sebserver.gui.service.remote.webservice.api.useraccount;

import ch.ethz.seb.sebserver.gbl.api.API;
import ch.ethz.seb.sebserver.gbl.api.EntityType;
import ch.ethz.seb.sebserver.gbl.model.user.UserFeatures;
import ch.ethz.seb.sebserver.gbl.profile.GuiProfile;
import ch.ethz.seb.sebserver.gui.service.remote.webservice.api.RestCall;
import com.fasterxml.jackson.core.type.TypeReference;
import org.springframework.context.annotation.Lazy;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;

@Lazy
@Component
@GuiProfile
public class GetUserFeatures extends RestCall<UserFeatures>  {

    public GetUserFeatures() {
        super(new TypeKey<>(
                        CallType.GET_SINGLE,
                        EntityType.USER,
                        new TypeReference<UserFeatures>() {
                        }),
                HttpMethod.GET,
                MediaType.APPLICATION_FORM_URLENCODED,
                API.USER_ACCOUNT_ENDPOINT + API.CURRENT_USER_PATH_SEGMENT + API.FEATURES_PATH_SEGMENT);
    }
}
