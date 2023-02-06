/*
 * Copyright (c) 2023 ETH Zürich, Educational Development and Technology (LET)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.webservice.servicelayer.session.impl;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

import ch.ethz.seb.sebserver.gbl.Constants;
import ch.ethz.seb.sebserver.gbl.model.exam.AllowedSEBVersion;
import ch.ethz.seb.sebserver.gbl.model.exam.AllowedSEBVersion.ClientVersion;
import ch.ethz.seb.sebserver.gbl.profile.WebServiceProfile;
import ch.ethz.seb.sebserver.webservice.datalayer.batis.model.ClientConnectionRecord;
import ch.ethz.seb.sebserver.webservice.servicelayer.dao.ClientConnectionDAO;
import ch.ethz.seb.sebserver.webservice.servicelayer.session.SEBClientVersionService;

@Lazy
@Service
@WebServiceProfile
public class SEBClientVersionServiceImpl implements SEBClientVersionService {

    private static final Logger log = LoggerFactory.getLogger(SEBClientVersionServiceImpl.class);

    private final ClientConnectionDAO clientConnectionDAO;
    private final ExamSessionCacheService examSessionCacheService;

    public final Set<String> knownWindowsOSTags;
    public final Set<String> knownMacOSTags;
    public final Set<String> knownIOSTags;
    public final Set<String> knownRestrictedVersions;

    public SEBClientVersionServiceImpl(
            final ClientConnectionDAO clientConnectionDAO,
            final ExamSessionCacheService examSessionCacheService,
            @Value("${ebserver.webservice.config.knownWindowsOSTags:Win,Windows}") final String knownWindowsOSTags,
            @Value("${ebserver.webservice.config.knownMacOSTags:macOS}") final String knownMacOSTags,
            @Value("${ebserver.webservice.config.knownIOSTags:iOS,iPad,iPadOS}") final String knownIOSTags,
            @Value("${ebserver.webservice.config.knownRestrictedVersions:BETA,rc,1.0.0.0}") final String knownRestrictedVersions) {

        this.clientConnectionDAO = clientConnectionDAO;
        this.examSessionCacheService = examSessionCacheService;
        this.knownWindowsOSTags = new HashSet<>(Arrays.asList(StringUtils.split(
                knownWindowsOSTags,
                Constants.LIST_SEPARATOR)));
        this.knownMacOSTags = new HashSet<>(Arrays.asList(StringUtils.split(
                knownMacOSTags,
                Constants.LIST_SEPARATOR)));
        this.knownIOSTags = new HashSet<>(Arrays.asList(StringUtils.split(
                knownIOSTags,
                Constants.LIST_SEPARATOR)));
        this.knownRestrictedVersions = new HashSet<>(Arrays.asList(StringUtils.split(
                knownRestrictedVersions,
                Constants.LIST_SEPARATOR)));
    }

    @Override
    public boolean isAllowedVersion(
            final String clientOSName,
            final String clientVersion,
            final List<AllowedSEBVersion> allowedSEBVersions) {

        // first check if this is a known restricted version
        if (this.knownRestrictedVersions.stream().filter(clientVersion::contains).findFirst().isPresent()) {
            if (log.isDebugEnabled()) {
                log.debug("Found default restricted SEB client version: {}", clientVersion);
            }
            return false;
        }

        final String osType = verifyOSType(clientOSName, clientVersion);

        if (StringUtils.isBlank(osType)) {
            if (log.isDebugEnabled()) {
                log.debug("No SEB client OS type tag found in : {} {}", clientOSName, clientVersion);
            }
            return false;
        }

        try {
            final String[] versionSplit = StringUtils.split(clientVersion, Constants.SPACE);
            final String versioNumber = versionSplit[0];
            final String[] versionNumberSplit = StringUtils.split(versioNumber, Constants.DOT);

            final int major = extractVersionNumber(versionNumberSplit[0]);
            final int minor = extractVersionNumber(versionNumberSplit[1]);
            final int patch = extractVersionNumber(versionNumberSplit[2]);

            final ClientVersion version = new ClientVersion(osType, major, minor, patch);
            return allowedSEBVersions
                    .stream()
                    .filter(v -> v.match(version))
                    .findFirst()
                    .isPresent();
        } catch (final Exception e) {
            log.warn("Unexpected error while trying to parse SEB version number in: {} {}", clientOSName,
                    clientVersion);
            return false;
        }
    }

    @Override
    public void checkVersionAndUpdateClientConnection(
            final ClientConnectionRecord record,
            final List<AllowedSEBVersion> allowedSEBVersions) {

        if (isAllowedVersion(record.getClientOsName(), record.getClientVersion(), allowedSEBVersions)) {
            saveSecurityCheckState(record, true);
        } else {
            saveSecurityCheckState(record, false);
        }
    }

    private int extractVersionNumber(final String versionNumPart) {
        try {
            return Integer.parseInt(versionNumPart);
        } catch (final NumberFormatException nfe) {
            return Integer.parseInt(String.valueOf(versionNumPart.charAt(0)));
        }
    }

    private String verifyOSType(final String clientOSName, final String clientVersion) {
        final char c = clientVersion.charAt(0);
        final String osVersionText = (c >= 'A' && c <= 'Z') ? clientVersion : clientOSName;
        if (StringUtils.isNotBlank(osVersionText)) {
            if (this.knownWindowsOSTags.stream().filter(osVersionText::contains).findFirst().isPresent()) {
                return AllowedSEBVersion.OS_WINDOWS_IDENTIFIER;
            }
            if (this.knownMacOSTags.stream().filter(osVersionText::contains).findFirst().isPresent()) {
                return AllowedSEBVersion.OS_MAC_IDENTIFIER;
            }
            if (this.knownIOSTags.stream().filter(osVersionText::contains).findFirst().isPresent()) {
                return AllowedSEBVersion.OS_IOS_IDENTIFIER;
            }
        }

        return null;
    }

    private void saveSecurityCheckState(final ClientConnectionRecord record, final Boolean checkStatus) {
        this.clientConnectionDAO
                .saveSEBClientVersionCheckStatus(record.getId(), checkStatus)
                .onError(error -> log.error("Failed to save ClientConnection grant: ",
                        error))
                .onSuccess(c -> this.examSessionCacheService.evictClientConnection(record.getConnectionToken()));
    }
}
