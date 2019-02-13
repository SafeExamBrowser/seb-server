/*
 * Copyright (c) 2018 ETH Zürich, Educational Development and Technology (LET)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.gbl.api;

public final class API {

    public static final String PARAM_INSTITUTION_ID = "institutionId";
    public static final String PARAM_MODEL_ID = "modelId";
    public static final String PARAM_ENTITY_TYPE = "entityType";

    public static final String INSTITUTION_VAR_PATH_SEGMENT = "/{" + PARAM_INSTITUTION_ID + "}";
    public static final String MODEL_ID_VAR_PATH_SEGMENT = "/{" + PARAM_MODEL_ID + "}";

    public static final String LOGO_ENDPOINT = "/logo";
    public static final String INSTITUTIONAL_LOGO_PATH = LOGO_ENDPOINT + INSTITUTION_VAR_PATH_SEGMENT;

    public static final String INSTITUTION_ENDPOINT = "/institution";

    public static final String LMS_SETUP_ENDPOINT = "/lms_setup";

    public static final String USER_ACCOUNT_ENDPOINT = "/useraccount";

    public static final String QUIZ_IMPORT_ENDPOINT = "/quiz";

    public static final String EXAM_ADMINISTRATION_ENDPOINT = "/exam";

    public static final String USER_ACTIVITY_LOG_ENDPOINT = "/useractivity";

    public static final String SELF_PATH_SEGMENT = "/self";

    public static final String NAMES_PATH_SEGMENT = "/names";

    public static final String LIST_PATH_SEGMENT = "/list";

    public static final String ACTIVE_PATH_SEGMENT = "/active";

    public static final String INACTIVE_PATH_SEGMENT = "/inactive";

    public static final String PATH_VAR_ACTIVE = MODEL_ID_VAR_PATH_SEGMENT + ACTIVE_PATH_SEGMENT;
    public static final String PATH_VAR_INACTIVE = MODEL_ID_VAR_PATH_SEGMENT + INACTIVE_PATH_SEGMENT;

}
