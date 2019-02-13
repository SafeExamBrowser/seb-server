/*
 * Copyright (c) 2018 ETH Zürich, Educational Development and Technology (LET)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.gbl.api;

public class API {

    public static final String LOGO_ENDPOINT = "/logo";

    public static final String LOGO_PATH_CODE = "/logo/{institutionId}";

    public static final String INSTITUTION_ENDPOINT = "/institution";

    public static final String LMS_SETUP_ENDPOINT = "/lms_setup";

    public static final String USER_ACCOUNT_ENDPOINT = "/useraccount";

    public static final String QUIZ_IMPORT_ENDPOINT = "/quiz";

    public static final String EXAM_ADMINISTRATION_ENDPOINT = "/exam";

    public static final String USER_ACTIVITY_LOG_ENDPOINT = "/useractivity";

    public static final String NAMES_SUFFIX = "/names";

    public static final String LIST_SUFFIX = "/list";

    public static final String ACTIVE_SUFFIX = "/active";

    public static final String INACTIVE_SUFFIX = "/inactive";

    public static final String PATH_VAR_MODEL_ID_NAME = "modelId";
    public static final String PATH_VAR_MODEL_ID = "/{" + PATH_VAR_MODEL_ID_NAME + "}";
    public static final String PATH_VAR_ACTIVE = PATH_VAR_MODEL_ID + ACTIVE_SUFFIX;
    public static final String PATH_VAR_INACTIVE = PATH_VAR_MODEL_ID + INACTIVE_SUFFIX;

}
