/*
 * Copyright (c) 2021 ETH Zürich, Educational Development and Technology (LET)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.webservice.weblayer.api;

import java.io.BufferedInputStream;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.lang3.StringUtils;
import org.apache.tomcat.util.http.fileupload.IOUtils;
import org.springframework.http.MediaType;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.InitBinder;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import ch.ethz.seb.sebserver.gbl.Constants;
import ch.ethz.seb.sebserver.gbl.api.API;
import ch.ethz.seb.sebserver.gbl.api.EntityType;
import ch.ethz.seb.sebserver.gbl.api.authorization.PrivilegeType;
import ch.ethz.seb.sebserver.gbl.model.Entity;
import ch.ethz.seb.sebserver.gbl.model.EntityKey;
import ch.ethz.seb.sebserver.gbl.model.EntityName;
import ch.ethz.seb.sebserver.gbl.model.Page;
import ch.ethz.seb.sebserver.gbl.model.PageSortOrder;
import ch.ethz.seb.sebserver.gbl.model.sebconfig.CertificateInfo;
import ch.ethz.seb.sebserver.gbl.model.sebconfig.CertificateInfo.CertificateFileType;
import ch.ethz.seb.sebserver.gbl.model.user.UserLogActivityType;
import ch.ethz.seb.sebserver.gbl.profile.WebServiceProfile;
import ch.ethz.seb.sebserver.gbl.util.Result;
import ch.ethz.seb.sebserver.webservice.servicelayer.PaginationService;
import ch.ethz.seb.sebserver.webservice.servicelayer.authorization.AuthorizationService;
import ch.ethz.seb.sebserver.webservice.servicelayer.authorization.UserService;
import ch.ethz.seb.sebserver.webservice.servicelayer.dao.FilterMap;
import ch.ethz.seb.sebserver.webservice.servicelayer.dao.UserActivityLogDAO;
import ch.ethz.seb.sebserver.webservice.servicelayer.sebconfig.CertificateService;

@WebServiceProfile
@RestController
@RequestMapping("${sebserver.webservice.api.admin.endpoint}" + API.CERTIFICATE_ENDPOINT)
public class CertificateController {

    private final AuthorizationService authorization;
    private final UserActivityLogDAO userActivityLogDAO;
    private final PaginationService paginationService;
    private final CertificateService certificateService;

    public CertificateController(
            final AuthorizationService authorization,
            final UserActivityLogDAO userActivityLogDAO,
            final PaginationService paginationService,
            final CertificateService certificateService) {

        this.authorization = authorization;
        this.userActivityLogDAO = userActivityLogDAO;
        this.paginationService = paginationService;
        this.certificateService = certificateService;
    }

    /** This is called by Spring to initialize the WebDataBinder and is used here to
     * initialize the default value binding for the institutionId request-parameter
     * that has the current users insitutionId as default.
     *
     * See also UserService.addUsersInstitutionDefaultPropertySupport */
    @InitBinder
    public void initBinder(final WebDataBinder binder) {
        this.authorization
                .getUserService()
                .addUsersInstitutionDefaultPropertySupport(binder);
    }

    /** The generic endpoint to get a Page of domain-entities of a specific type.
     * </p>
     * GET /{api}/{domain-entity-name}
     * </p>
     * For example for the "exam" domain-entity
     * GET /admin-api/v1/exam
     * GET /admin-api/v1/exam?page_number=2&page_size=10&sort=-name
     * GET /admin-api/v1/exam?name=seb&active=true
     * </p>
     * Sorting: the sort parameter to sort the list of entities before paging
     * the sort parameter is the name of the entity-model attribute to sort with a leading '-' sign for
     * descending sort order. Note that not all entity-model attribute are suited for sorting while the most
     * are.
     * </p>
     * Filter: The filter attributes accepted by this API depend on the actual entity model (domain object)
     * and are of the form [domain-attribute-name]=[filter-value]. E.g.: name=abc or type=EXAM. Usually
     * filter attributes of text type are treated as SQL wildcard with %[text]% to filter all text containing
     * a given text-snippet.
     *
     * @param institutionId The institution identifier of the request.
     *            Default is the institution identifier of the institution of the current user
     * @param pageNumber the number of the page that is requested
     * @param pageSize the size of the page that is requested
     * @param sort the sort parameter to sort the list of entities before paging
     *            the sort parameter is the name of the entity-model attribute to sort with a leading '-' sign for
     *            descending sort order.
     * @param allRequestParams a MultiValueMap of all request parameter that is used for filtering.
     * @return Page of domain-model-entities of specified type */
    @RequestMapping(
            method = RequestMethod.GET,
            consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    public Page<CertificateInfo> getPage(
            @RequestParam(
                    name = API.PARAM_INSTITUTION_ID,
                    required = true,
                    defaultValue = UserService.USERS_INSTITUTION_AS_DEFAULT) final Long institutionId,
            @RequestParam(name = Page.ATTR_PAGE_NUMBER, required = false) final Integer pageNumber,
            @RequestParam(name = Page.ATTR_PAGE_SIZE, required = false) final Integer pageSize,
            @RequestParam(name = Page.ATTR_SORT, required = false) final String sort,
            @RequestParam final MultiValueMap<String, String> allRequestParams,
            final HttpServletRequest request) {

        checkReadPrivilege(institutionId);

        final FilterMap filterMap = new FilterMap(allRequestParams, request.getQueryString());
        final Collection<CertificateInfo> certificates = this.certificateService
                .getCertificateInfo(institutionId, filterMap)
                .getOrThrow();

        return this.paginationService.buildPageFromList(
                pageNumber,
                pageSize,
                sort,
                certificates,
                pageSort(sort));

    }

    @RequestMapping(
            path = API.NAMES_PATH_SEGMENT,
            method = RequestMethod.GET,
            consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    public Collection<EntityName> getNames(
            @RequestParam(
                    name = API.PARAM_INSTITUTION_ID,
                    required = true,
                    defaultValue = UserService.USERS_INSTITUTION_AS_DEFAULT) final Long institutionId,
            @RequestParam final MultiValueMap<String, String> allRequestParams,
            final HttpServletRequest request) {

        checkReadPrivilege(institutionId);

        return this.certificateService
                .getCertificateInfo(
                        institutionId,
                        new FilterMap(allRequestParams, request.getQueryString()))
                .getOrThrow()
                .stream()
                .map(Entity::toName)
                .collect(Collectors.toList());
    }

    @RequestMapping(
            path = API.CERTIFICATE_ALIAS_VAR_PATH_SEGMENT,
            method = RequestMethod.GET,
            consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    public CertificateInfo getAlias(
            @RequestParam(
                    name = API.PARAM_INSTITUTION_ID,
                    required = true,
                    defaultValue = UserService.USERS_INSTITUTION_AS_DEFAULT) final Long institutionId,
            @PathVariable final String alias) {

        return this.certificateService
                .getCertificateInfo(institutionId, alias)
                .getOrThrow();
    }

    @RequestMapping(
            method = RequestMethod.POST,
            consumes = MediaType.APPLICATION_OCTET_STREAM_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    public CertificateInfo loadCertificate(
            @RequestParam(
                    name = API.PARAM_INSTITUTION_ID,
                    required = true,
                    defaultValue = UserService.USERS_INSTITUTION_AS_DEFAULT) final Long institutionId,
            @RequestHeader(
                    name = API.IMPORT_FILE_ATTR_NAME,
                    required = true) final String fileName,
            @RequestHeader(
                    name = API.CERTIFICATE_ALIAS,
                    required = false) final String alias,
            @RequestHeader(name = API.IMPORT_PASSWORD_ATTR_NAME, required = false) final CharSequence password,
            final HttpServletRequest request) {

        this.checkWritePrivilege(institutionId);

        final CertificateFileType certificateFileType = CertificateFileType.forFileName(fileName);
        InputStream inputStream = null;

        try {

            inputStream = new BufferedInputStream(request.getInputStream());
            return this.certificateService.addCertificate(institutionId, certificateFileType, alias, inputStream)
                    .flatMap(certData -> this.userActivityLogDAO.log(UserLogActivityType.IMPORT, certData))
                    .getOrThrow();

        } catch (final Exception e) {
            IOUtils.closeQuietly(inputStream);
            if (e instanceof RuntimeException) {
                throw (RuntimeException) e;
            }
            throw new RuntimeException(e);
        }
    }

    @RequestMapping(
            path = API.CERTIFICATE_ALIAS_VAR_PATH_SEGMENT,
            method = RequestMethod.DELETE,
            consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    public Collection<EntityKey> deleteCertificate(
            @RequestParam(
                    name = API.PARAM_INSTITUTION_ID,
                    required = true,
                    defaultValue = UserService.USERS_INSTITUTION_AS_DEFAULT) final Long institutionId,
            @RequestParam(name = API.CERTIFICATE_ALIAS, required = true) final String aliases) {

        this.checkWritePrivilege(institutionId);

        return Arrays.asList(StringUtils.split(aliases, Constants.LIST_SEPARATOR))
                .stream()
                .map(alias -> this.certificateService
                        .removeCertificate(institutionId, alias))
                .flatMap(Result::onErrorLogAndSkip)
                .collect(Collectors.toList());
    }

    private void checkReadPrivilege(final Long institutionId) {
        this.authorization.check(
                PrivilegeType.READ,
                EntityType.CERTIFICATE,
                institutionId);
    }

    private void checkWritePrivilege(final Long institutionId) {
        this.authorization.check(
                PrivilegeType.WRITE,
                EntityType.CERTIFICATE,
                institutionId);
    }

    private static Function<Collection<CertificateInfo>, List<CertificateInfo>> pageSort(final String sort) {
        return certificates -> {
            final List<CertificateInfo> list = certificates.stream().collect(Collectors.toList());
            if (StringUtils.isBlank(sort)) {
                return list;
            }

            final String sortBy = PageSortOrder.decode(sort);
            if (sortBy.equals(CertificateInfo.FILTER_ATTR_ALIAS)) {
                list.sort(Comparator.comparing(cert -> cert.alias));
            }

            if (PageSortOrder.DESCENDING == PageSortOrder.getSortOrder(sort)) {
                Collections.reverse(list);
            }

            return list;
        };
    }

}
