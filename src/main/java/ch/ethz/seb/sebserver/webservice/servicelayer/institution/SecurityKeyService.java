/*
 * Copyright (c) 2022 ETH Zürich, Educational Development and Technology (LET)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.webservice.servicelayer.institution;

import java.util.Collection;

import ch.ethz.seb.sebserver.gbl.model.EntityKey;
import ch.ethz.seb.sebserver.gbl.model.institution.SecurityCheckResult;
import ch.ethz.seb.sebserver.gbl.model.institution.SecurityKey;
import ch.ethz.seb.sebserver.gbl.model.session.ClientConnection;
import ch.ethz.seb.sebserver.gbl.util.Result;

public interface SecurityKeyService {

    /** This attribute name is used to store the App-Signature-Key given by a SEB Client */
    public static final String ADDITIONAL_ATTR_APP_SIGNATURE_KEY = "APP_SIGNATURE_KEY";

    /** This attribute name is used on exams to store the flag for indicating the signature key check */
    public static final String ADDITIONAL_ATTR_SIGNATURE_KEY_CHECK_ENABLED = "SIGNATURE_KEY_CHECK_ENABLED";

    /** This attribute name is used to store the signature check grant threshold for statistical checks */
    public static final String ADDITIONAL_ATTR_STATISTICAL_GRANT_COUNT_THRESHOLD = "STATISTICAL_GRANT_COUNT_THRESHOLD";

    /** This attribute name is used to store the signature check encryption certificate is one is used */
    public static final String ADDITIONAL_ATTR_SIGNATURE_KEY_CERT_ALIAS = "SIGNATURE_KEY_CERT_ALIAS";

    Result<Collection<SecurityKey>> getPlainGrants(Long institutionId, Long examId);

    Result<Collection<SecurityKey>> getPlainAppSignatureKeyGrants(Long institutionId, Long examId);

    Result<SecurityKey> registerSecurityKey(SecurityKey key);

    Result<SecurityKey> registerGlobalAppSignatureKey(Long institutionId, Long connectionId, String tag);

    Result<SecurityKey> registerExamAppSignatureKey(Long institutionId, Long examId, Long connectionId, String tag);

    Result<SecurityCheckResult> applyAppSignatureCheck(
            Long institutionId,
            Long examId,
            String connectionToken,
            String appSignatureKey);

    boolean checkAppSignatureKey(ClientConnection clientConnection, String appSignatureKey);

    void updateAppSignatureKeyGrants(Long examId);

    Result<SecurityKey> getDecrypted(SecurityKey key);

    Result<EntityKey> deleteSecurityKeyGrant(String keyModelId);

}
