package ch.ethz.seb.sebserver.gui.api.exam;

import ch.ethz.seb.sebserver.gbl.api.API;
import ch.ethz.seb.sebserver.gbl.api.EntityType;
import ch.ethz.seb.sebserver.gbl.model.EntityProcessingReport;

import ch.ethz.seb.sebserver.gui.api.RestCall;
import com.fasterxml.jackson.core.type.TypeReference;
import org.springframework.context.annotation.Lazy;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;

@Lazy
@Component

public class ForceDeleteExam extends RestCall<EntityProcessingReport> {

    public ForceDeleteExam() {
        super(new TypeKey<>(
                        CallType.DELETE,
                        EntityType.EXAM,
                        new TypeReference<EntityProcessingReport>() {
                        }),
                HttpMethod.DELETE,
                MediaType.APPLICATION_FORM_URLENCODED,
                API.EXAM_ADMINISTRATION_ENDPOINT + API.MODEL_ID_VAR_PATH_SEGMENT + API.FORCE_PATH_SEGMENT);
    }

}
