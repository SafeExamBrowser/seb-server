/*
 * Copyright (c) 2020 ETH Zürich, Educational Development and Technology (LET)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.gui.service.page;

import ch.ethz.seb.sebserver.gbl.model.exam.ProctoringServiceSettings.ProctoringServerType;
import ch.ethz.seb.sebserver.gui.service.i18n.LocTextKey;

public interface RemoteProctoringView extends TemplateComposer {

    static final LocTextKey CLOSE_WINDOW_TEXT_KEY =
            new LocTextKey("sebserver.monitoring.exam.proctoring.action.close");
    static final LocTextKey BROADCAST_AUDIO_ON_TEXT_KEY =
            new LocTextKey("sebserver.monitoring.exam.proctoring.action.broadcaston.audio");
    static final LocTextKey BROADCAST_AUDIO_OFF_TEXT_KEY =
            new LocTextKey("sebserver.monitoring.exam.proctoring.action.broadcastoff.audio");
    static final LocTextKey BROADCAST_VIDEO_ON_TEXT_KEY =
            new LocTextKey("sebserver.monitoring.exam.proctoring.action.broadcaston.video");
    static final LocTextKey BROADCAST_VIDEO_OFF_TEXT_KEY =
            new LocTextKey("sebserver.monitoring.exam.proctoring.action.broadcastoff.video");
    static final LocTextKey CHAT_ON_TEXT_KEY =
            new LocTextKey("sebserver.monitoring.exam.proctoring.action.broadcaston.chat");
    static final LocTextKey CHAT_OFF_TEXT_KEY =
            new LocTextKey("sebserver.monitoring.exam.proctoring.action.broadcastoff.chat");

    /** Get the remote proctoring server type this remote proctoring view can handle.
     *
     * @return the remote proctoring server type this remote proctoring view can handle. */
    ProctoringServerType serverType();

}
