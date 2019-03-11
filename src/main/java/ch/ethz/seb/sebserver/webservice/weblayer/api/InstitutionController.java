/*
 * Copyright (c) 2018 ETH Zürich, Educational Development and Technology (LET)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.webservice.weblayer.api;

import java.io.InputStream;

import javax.servlet.http.HttpServletResponse;

import org.apache.tomcat.util.http.fileupload.IOUtils;
import org.mybatis.dynamic.sql.SqlTable;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import ch.ethz.seb.sebserver.gbl.api.API;
import ch.ethz.seb.sebserver.gbl.api.EntityType;
import ch.ethz.seb.sebserver.gbl.api.POSTMapper;
import ch.ethz.seb.sebserver.gbl.authorization.PrivilegeType;
import ch.ethz.seb.sebserver.gbl.model.institution.Institution;
import ch.ethz.seb.sebserver.gbl.profile.WebServiceProfile;
import ch.ethz.seb.sebserver.webservice.datalayer.batis.mapper.InstitutionRecordDynamicSqlSupport;
import ch.ethz.seb.sebserver.webservice.servicelayer.PaginationService;
import ch.ethz.seb.sebserver.webservice.servicelayer.authorization.AuthorizationService;
import ch.ethz.seb.sebserver.webservice.servicelayer.authorization.SEBServerUser;
import ch.ethz.seb.sebserver.webservice.servicelayer.bulkaction.BulkActionService;
import ch.ethz.seb.sebserver.webservice.servicelayer.dao.InstitutionDAO;
import ch.ethz.seb.sebserver.webservice.servicelayer.dao.UserActivityLogDAO;
import ch.ethz.seb.sebserver.webservice.servicelayer.seb.SebClientConfigService;
import ch.ethz.seb.sebserver.webservice.servicelayer.validation.BeanValidationService;

@WebServiceProfile
@RestController
@RequestMapping("/${sebserver.webservice.api.admin.endpoint}" + API.INSTITUTION_ENDPOINT)
public class InstitutionController extends ActivatableEntityController<Institution, Institution> {

    private final InstitutionDAO institutionDAO;
    private final SebClientConfigService sebClientConfigService;

    public InstitutionController(
            final InstitutionDAO institutionDAO,
            final AuthorizationService authorization,
            final UserActivityLogDAO userActivityLogDAO,
            final BulkActionService bulkActionService,
            final PaginationService paginationService,
            final BeanValidationService beanValidationService,
            final SebClientConfigService sebClientConfigService) {

        super(authorization,
                bulkActionService,
                institutionDAO,
                userActivityLogDAO,
                paginationService,
                beanValidationService);

        this.institutionDAO = institutionDAO;
        this.sebClientConfigService = sebClientConfigService;
    }

    @Override
    protected Class<Institution> modifiedDataType() {
        return Institution.class;
    }

    @Override
    protected SqlTable getSQLTableOfEntity() {
        return InstitutionRecordDynamicSqlSupport.institutionRecord;
    }

    @RequestMapping(path = API.SELF_PATH_SEGMENT, method = RequestMethod.GET)
    public Institution getOwn() {
        final SEBServerUser currentUser = this.authorization
                .getUserService()
                .getCurrentUser();

        final Long institutionId = currentUser.institutionId();
        return this.institutionDAO.byPK(institutionId).getOrThrow();
    }

    @RequestMapping(
            path = API.INSTITUTION_VAR_PATH_SEGMENT + API.SEB_CONFIG_EXPORT_PATH_SEGMENT,
            method = RequestMethod.GET,
            produces = MediaType.APPLICATION_OCTET_STREAM_VALUE) // TODO check if this is the right format
    public void downloadSEBConfig(
            @PathVariable final Long institutionId,
            final HttpServletResponse response) throws Exception {

        this.authorization.check(PrivilegeType.WRITE, EntityType.SEB_CLIENT_CONFIGURATION);

        response.setContentType(MediaType.APPLICATION_OCTET_STREAM_VALUE);
        response.setStatus(HttpStatus.OK.value());

        final InputStream sebConfigFileIn = this.sebClientConfigService
                .exportSebClientConfigurationOfInstitution(institutionId)
                .getOrThrow();

        IOUtils.copyLarge(sebConfigFileIn, response.getOutputStream());
        response.flushBuffer();
    }

    @Override
    protected Institution createNew(final POSTMapper postParams) {
        return new Institution(null, postParams);
    }

}
