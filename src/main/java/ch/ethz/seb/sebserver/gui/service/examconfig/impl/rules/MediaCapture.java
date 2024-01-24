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
public class MediaCapture implements ValueChangeRule  {

    private static final Logger log = LoggerFactory.getLogger(MediaCapture.class);

    private static final String KEY_MEDIA_AUTOPLAY = "browserMediaAutoplay";
    private static final String KEY_MEDIA_AUTOPLAY_AUDIO = "browserMediaAutoplayAudio";

    private static final String KEY_MEDIA_AUTOPLAY_VIDEO = "browserMediaAutoplayVideo";


    @Override
    public boolean observesAttribute(final ConfigurationAttribute attribute) {
        return KEY_MEDIA_AUTOPLAY.equals(attribute.name);
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
            if (BooleanUtils.toBoolean(value.value)) {
                context.enable(KEY_MEDIA_AUTOPLAY_AUDIO);
                context.enable(KEY_MEDIA_AUTOPLAY_VIDEO);
            } else {
                context.setValue(KEY_MEDIA_AUTOPLAY_AUDIO, "false");
                context.setValue(KEY_MEDIA_AUTOPLAY_VIDEO, "false");
                context.disable(KEY_MEDIA_AUTOPLAY_AUDIO);
                context.disable(KEY_MEDIA_AUTOPLAY_VIDEO);
            }

        } catch (final Exception e) {
            log.warn("Failed to apply rule: ", e);
        }
    }
}
