package ch.ethz.seb.sebserver.gui.service.examconfig.impl.rules;

import ch.ethz.seb.sebserver.gbl.model.sebconfig.ConfigurationAttribute;
import ch.ethz.seb.sebserver.gbl.model.sebconfig.ConfigurationValue;
import ch.ethz.seb.sebserver.gbl.profile.GuiProfile;
import ch.ethz.seb.sebserver.gui.service.examconfig.ValueChangeRule;
import ch.ethz.seb.sebserver.gui.service.examconfig.impl.ViewContext;
import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

@Lazy
@Service
@GuiProfile
public class AllowScreenCapture implements ValueChangeRule  {

    private static final Logger log = LoggerFactory.getLogger(AllowScreenCapture.class);

    private static final String KEY_ALLOW_SCREEN_CAPTURE = "allowScreenCapture";
    private static final String KEY_ALLOW_WINDOW_CAPTURE = "allowWindowCapture";
    private static final String KEY_BLOCK_SS = "blockScreenShotsLegacy";
    @Override
    public boolean observesAttribute(final ConfigurationAttribute attribute) {
        return KEY_ALLOW_SCREEN_CAPTURE.equals(attribute.name) || KEY_ALLOW_WINDOW_CAPTURE.equals(attribute.name);
    }

    @Override
    public void applyRule(
            final ViewContext context,
            final ConfigurationAttribute attribute,
            final ConfigurationValue value) {

        if (context.isReadonly() || StringUtils.isBlank(value.value)) {
            return;
        }

        try {

            if (KEY_ALLOW_SCREEN_CAPTURE.equals(attribute.name)) {
                if (BooleanUtils.toBoolean(value.value)) {
                    context.enable(KEY_ALLOW_WINDOW_CAPTURE);
                } else {
                    context.setValue(KEY_ALLOW_WINDOW_CAPTURE, "false");
                    context.setValue(KEY_BLOCK_SS, "false");
                    context.disable(KEY_ALLOW_WINDOW_CAPTURE);
                    context.disable(KEY_BLOCK_SS);
                }
            } else if (KEY_ALLOW_WINDOW_CAPTURE.equals(attribute.name)) {
                if (BooleanUtils.toBoolean(value.value)) {
                    context.enable(KEY_BLOCK_SS);
                } else {
                    context.setValue(KEY_BLOCK_SS, "false");
                    context.disable(KEY_BLOCK_SS);
                }
            }

        } catch (final Exception e) {
            log.warn("Failed to apply rule: ", e);
        }
    }
}
