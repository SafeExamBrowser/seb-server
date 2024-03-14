/*
 * Copyright (c) 2019 ETH Zürich, IT Services
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.gui.service.examconfig.impl.rules;

import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang3.BooleanUtils;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

import ch.ethz.seb.sebserver.gbl.model.sebconfig.ConfigurationAttribute;
import ch.ethz.seb.sebserver.gbl.model.sebconfig.ConfigurationValue;
import ch.ethz.seb.sebserver.gbl.profile.GuiProfile;
import ch.ethz.seb.sebserver.gbl.util.Tuple;
import ch.ethz.seb.sebserver.gui.service.examconfig.ValueChangeRule;
import ch.ethz.seb.sebserver.gui.service.examconfig.impl.ViewContext;

@Lazy
@Service
@GuiProfile
public class ProxyPasswordRule implements ValueChangeRule {

    public static final String KEY_HTTP_PWD_REQUIRED = "HTTPRequiresPassword";
    public static final String KEY_HTTPS_PWD_REQUIRED = "HTTPSRequiresPassword";
    public static final String KEY_FTP_PWD_REQUIRED = "FTPRequiresPassword";
    public static final String KEY_SOCKS_PWD_REQUIRED = "SOCKSRequiresPassword";
    public static final String KEY_RTSP_PWD_REQUIRED = "RTSPRequiresPassword";

    private final Map<String, Tuple<String>> observed;

    public ProxyPasswordRule() {
        this.observed = new HashMap<>();
        this.observed.put(KEY_HTTP_PWD_REQUIRED, new Tuple<>("HTTPUsername", "HTTPPassword"));
        this.observed.put(KEY_HTTPS_PWD_REQUIRED, new Tuple<>("HTTPSUsername", "HTTPSPassword"));
        this.observed.put(KEY_FTP_PWD_REQUIRED, new Tuple<>("FTPUsername", "FTPPassword"));
        this.observed.put(KEY_SOCKS_PWD_REQUIRED, new Tuple<>("SOCKSUsername", "SOCKSPassword"));
        this.observed.put(KEY_RTSP_PWD_REQUIRED, new Tuple<>("RTSPUsername", "RTSPPassword"));
    }

    @Override
    public boolean observesAttribute(final ConfigurationAttribute attribute) {
        return this.observed.containsKey(attribute.name);
    }

    @Override
    public void applyRule(
            final ViewContext context,
            final ConfigurationAttribute attribute,
            final ConfigurationValue value) {

        final Tuple<String> tuple = this.observed.get(attribute.name);
        if (tuple != null) {
            if (BooleanUtils.toBoolean(value.value)) {
                context.enable(tuple._1);
                context.enable(tuple._2);
            } else {
                context.disable(tuple._1);
                context.disable(tuple._2);
            }
        }
    }

}
