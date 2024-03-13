/*
 * Copyright (c) 2019 ETH Zürich, IT Services
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.webservice.weblayer.api;

import java.util.Arrays;
import java.util.Collection;
import java.util.EnumSet;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;

import ch.ethz.seb.sebserver.gbl.model.*;
import org.apache.commons.lang3.StringUtils;
import org.mybatis.dynamic.sql.SqlTable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.InitBinder;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;

import ch.ethz.seb.sebserver.gbl.Constants;
import ch.ethz.seb.sebserver.gbl.api.API;
import ch.ethz.seb.sebserver.gbl.api.API.BulkActionType;
import ch.ethz.seb.sebserver.gbl.api.EntityType;
import ch.ethz.seb.sebserver.gbl.api.POSTMapper;
import ch.ethz.seb.sebserver.gbl.api.authorization.PrivilegeType;
import ch.ethz.seb.sebserver.gbl.util.Pair;
import ch.ethz.seb.sebserver.gbl.util.Result;
import ch.ethz.seb.sebserver.webservice.servicelayer.PaginationService;
import ch.ethz.seb.sebserver.webservice.servicelayer.authorization.AuthorizationService;
import ch.ethz.seb.sebserver.webservice.servicelayer.authorization.UserService;
import ch.ethz.seb.sebserver.webservice.servicelayer.bulkaction.BulkActionService;
import ch.ethz.seb.sebserver.webservice.servicelayer.bulkaction.impl.BulkAction;
import ch.ethz.seb.sebserver.webservice.servicelayer.dao.EntityDAO;
import ch.ethz.seb.sebserver.webservice.servicelayer.dao.FilterMap;
import ch.ethz.seb.sebserver.webservice.servicelayer.dao.UserActivityLogDAO;
import ch.ethz.seb.sebserver.webservice.servicelayer.validation.BeanValidationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;

/** Abstract Entity-Controller that defines generic Entity rest API endpoints that are supported
 * by all entity types.
 *
 * @param <T> The concrete Entity domain-model type used on all GET, PUT
 * @param <M> The concrete Entity domain-model type used for POST methods (new) */
@SecurityRequirement(name = "oauth2")
public abstract class EntityController<T extends Entity, M extends Entity> {

    private static final Logger log = LoggerFactory.getLogger(EntityController.class);

    protected final AuthorizationService authorization;
    protected final BulkActionService bulkActionService;
    protected final EntityDAO<T, M> entityDAO;
    protected final UserActivityLogDAO userActivityLogDAO;
    protected final PaginationService paginationService;
    protected final BeanValidationService beanValidationService;

    protected EntityController(
            final AuthorizationService authorization,
            final BulkActionService bulkActionService,
            final EntityDAO<T, M> entityDAO,
            final UserActivityLogDAO userActivityLogDAO,
            final PaginationService paginationService,
            final BeanValidationService beanValidationService) {

        this.authorization = authorization;
        this.bulkActionService = bulkActionService;
        this.entityDAO = entityDAO;
        this.userActivityLogDAO = userActivityLogDAO;
        this.paginationService = paginationService;
        this.beanValidationService = beanValidationService;
    }

    /** This is called by Spring to initialize the WebDataBinder and is used here to
     * initialize the default value binding for the institutionId request-parameter
     * that has the current users institutionId as default.
     * <p>
     * See also UserService.addUsersInstitutionDefaultPropertySupport */
    @InitBinder
    public void initBinder(final WebDataBinder binder) {
        this.authorization
                .getUserService()
                .addUsersInstitutionDefaultPropertySupport(binder);
    }

    // ******************
    // * GET (getPage)
    // ******************

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
    @Operation(
            summary = "Get a page of the specific domain entity. Sorting and filtering is applied before paging",
            description = "Sorting: the sort parameter to sort the list of entities before paging\n"
                    + "the sort parameter is the name of the entity-model attribute to sort with a leading '-' sign for\n"
                    + "descending sort order. Note that not all entity-model attribute are suited for sorting while the most\n"
                    + "are.\n"
                    + "</p>\n"
                    + "Filter: The filter attributes accepted by this API depend on the actual entity model (domain object)\n"
                    + "and are of the form [domain-attribute-name]=[filter-value]. E.g.: name=abc or type=EXAM. Usually\n"
                    + "filter attributes of text type are treated as SQL wildcard with %[text]% to filter all text containing\n"
                    + "a given text-snippet.",
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    content = { @Content(mediaType = MediaType.APPLICATION_FORM_URLENCODED_VALUE) }),
            parameters = {
                    @Parameter(
                            name = Page.ATTR_PAGE_NUMBER,
                            description = "The number of the page to get from the whole list. If the page does not exists, the API retruns with the first page."),
                    @Parameter(
                            name = Page.ATTR_PAGE_SIZE,
                            description = "The size of the page to get."),
                    @Parameter(
                            name = Page.ATTR_SORT,
                            description = "the sort parameter to sort the list of entities before paging"),
                    @Parameter(
                            name = API.PARAM_INSTITUTION_ID,
                            description = "The institution identifier of the request.\n"
                                    + "Default is the institution identifier of the institution of the current user"),
                    @Parameter(
                            name = "filterCriteria",
                            description = "Additional filter criterias \n" +
                                    "For OpenAPI 3 input please use the form: {\"columnName\":\"filterValue\"}",
                            example = "{\"name\":\"ethz\"}",
                            required = false,
                            allowEmptyValue = true)
            })
    @RequestMapping(
            method = RequestMethod.GET,
            consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    public Page<T> getPage(
            @RequestParam(
                    name = API.PARAM_INSTITUTION_ID,
                    required = true,
                    defaultValue = UserService.USERS_INSTITUTION_AS_DEFAULT) final Long institutionId,
            @RequestParam(name = Page.ATTR_PAGE_NUMBER, required = false) final Integer pageNumber,
            @RequestParam(name = Page.ATTR_PAGE_SIZE, required = false) final Integer pageSize,
            @RequestParam(name = Page.ATTR_SORT, required = false) final String sort,
            @RequestParam final MultiValueMap<String, String> allRequestParams,
            final HttpServletRequest request) {

        // at least current user must have read access for specified entity type within its own institution
        checkReadPrivilege(institutionId);

        final FilterMap filterMap = new FilterMap(allRequestParams, request.getQueryString());
        populateFilterMap(filterMap, institutionId, sort);

        final Page<T> page = this.paginationService.getPage(
                pageNumber,
                pageSize,
                sort,
                getSQLTableOfEntity().tableNameAtRuntime(),
                () -> getAll(filterMap))
                .getOrThrow();

        return page;
    }

    // ******************
    // * GET (names)
    // ******************

    @Operation(
            summary = "Get a filtered list of specific entity name keys.",
            description = "An entity name key is a minimal entity data object with the entity-type, modelId and the name of the entity."
                    + "</p>\n"
                    + "Filter: The filter attributes accepted by this API depend on the actual entity model (domain object)\n"
                    + "and are of the form [domain-attribute-name]=[filter-value]. E.g.: name=abc or type=EXAM. Usually\n"
                    + "filter attributes of text type are treated as SQL wildcard with %[text]% to filter all text containing\n"
                    + "a given text-snippet.",
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    content = { @Content(mediaType = MediaType.APPLICATION_FORM_URLENCODED_VALUE) }),
            parameters = {
                    @Parameter(
                            name = API.PARAM_INSTITUTION_ID,
                            description = "The institution identifier of the request.\n"
                                    + "Default is the institution identifier of the institution of the current user"),
                    @Parameter(
                            name = "filterCriteria",
                            description = "Additional filter criterias \n" +
                                    "For OpenAPI 3 input please use the form: {\"columnName\":\"filterValue\"}",
                            example = "{\"name\":\"ethz\"}",
                            required = false,
                            allowEmptyValue = true)
            })
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

        // at least current user must have read access for specified entity type within its own institution
        checkReadPrivilege(institutionId);

        final FilterMap filterMap = new FilterMap(allRequestParams, request.getQueryString());

        // if current user has no read access for specified entity type within other institution then its own institution,
        // then the current users institutionId is put as a SQL filter criteria attribute to extends query performance
        if (!this.authorization.hasGrant(PrivilegeType.READ, this.getGrantEntityType())) {
            filterMap.putIfAbsent(API.PARAM_INSTITUTION_ID, String.valueOf(institutionId));
        }

        final Collection<T> all = getAll(filterMap)
                .getOrThrow();

        return all
                .stream()
                .map(Entity::toName)
                .collect(Collectors.toList());
    }

    // ******************
    // * GET (dependency)
    // ******************

    @Operation(
            summary = "Get a list of dependency keys of all dependent entity objects for a "
                    + "specified source entity and bulk action.",
            description = "Get a list of dependency keys of all dependent entity objects for a "
                    + "specified source entity and bulk action.\n " +
                    "This can be used to verify depended objects for a certain bulk action to "
                    + "give a report of affected objects beforehand.\n " +
                    "For example for a delete action of a certain object, this gives all objects "
                    + "that will also be deleted within the deletion of the source object",
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    content = { @Content(mediaType = MediaType.APPLICATION_FORM_URLENCODED_VALUE) }),

            parameters = {
                    @Parameter(
                            name = API.PARAM_MODEL_ID,
                            description = "The model identifier of the source entity object to geht the dependencies for.",
                            in = ParameterIn.PATH),
                    @Parameter(
                            name = API.PARAM_BULK_ACTION_TYPE,
                            description = "The bulk action type defining the type of action to get the dependencies for.\n"
                                    + "This is the name of the enumeration "),
                    @Parameter(
                            name = API.PARAM_BULK_ACTION_ADD_INCLUDES,
                            description = "Indicates if the following 'includes' parameter shall be processed or not.\n The default is false "),
                    @Parameter(
                            name = API.PARAM_BULK_ACTION_INCLUDES,
                            description = "A comma separated list of names of the EntityType enumeration that defines all entity types that shall be included in the result.")
            })
    @RequestMapping(
            path = API.MODEL_ID_VAR_PATH_SEGMENT + API.DEPENDENCY_PATH_SEGMENT,
            method = RequestMethod.GET,
            consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    public Collection<EntityDependency> getDependencies(
            @PathVariable final String modelId,
            @RequestParam(name = API.PARAM_BULK_ACTION_TYPE, required = true) final BulkActionType bulkActionType,
            @RequestParam(name = API.PARAM_BULK_ACTION_ADD_INCLUDES, defaultValue = "false") final boolean addIncludes,
            @RequestParam(name = API.PARAM_BULK_ACTION_INCLUDES, required = false) final List<String> includes) {

        this.entityDAO
                .byModelId(modelId)
                .map(this::checkReadAccess);

        final BulkAction bulkAction = new BulkAction(
                bulkActionType,
                this.entityDAO.entityType(),
                Arrays.asList(new EntityKey(modelId, this.entityDAO.entityType())),
                convertToEntityType(addIncludes, includes));

        this.bulkActionService.collectDependencies(bulkAction);
        return bulkAction.getDependencies();
    }

    // ******************
    // * GET (single)
    // ******************

    @Operation(
            summary = "Get a single entity by its modelId.",
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    content = { @Content(mediaType = MediaType.APPLICATION_FORM_URLENCODED_VALUE) }),
            parameters = {
                    @Parameter(
                            name = API.PARAM_MODEL_ID,
                            description = "The model identifier of the entity object to get.",
                            in = ParameterIn.PATH)
            })
    @RequestMapping(
            path = API.MODEL_ID_VAR_PATH_SEGMENT,
            method = RequestMethod.GET,
            consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    public T getBy(@PathVariable final String modelId) {

        return this.entityDAO
                .byModelId(modelId)
                .flatMap(this::checkReadAccess)
                .getOrThrow();
    }

    // ******************
    // * GET (list)
    // ******************

    @Operation(
            summary = "Get a list of entity objects by a given list of model identifiers of entities.",
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    content = { @Content(mediaType = MediaType.APPLICATION_FORM_URLENCODED_VALUE) }),
            parameters = {
                    @Parameter(
                            name = API.PARAM_MODEL_ID_LIST,
                            description = "Comma separated list of model identifiers.")
            })
    @RequestMapping(
            path = API.LIST_PATH_SEGMENT,
            method = RequestMethod.GET,
            consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    public List<T> getForIds(@RequestParam(name = API.PARAM_MODEL_ID_LIST, required = true) final String modelIds) {

        return Result.tryCatch(() -> Arrays.stream(StringUtils.split(modelIds, Constants.LIST_SEPARATOR_CHAR))
                .map(modelId -> new EntityKey(modelId, this.entityDAO.entityType()))
                .collect(Collectors.toSet()))
                .flatMap(this.entityDAO::byEntityKeys)
                .getOrThrow()
                .stream()
                .filter(this::hasReadAccess)
                .collect(Collectors.toList());
    }

    // ******************
    // * POST (create)
    // ******************

    @Operation(
            summary = "Create a new entity object of specifies type by using the given form parameter",
            description = "This expects " + MediaType.APPLICATION_FORM_URLENCODED_VALUE +
                    " format for the form parameter" +
                    " and tries to create a new entity object from this form parameter, " +
                    "resulting in an error if there are missing" +
                    " or incorrect form paramter. The needed form paramter " +

                    "can be verified within the specific entity object.",
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    content = { @Content(mediaType = MediaType.APPLICATION_FORM_URLENCODED_VALUE) }),
            parameters = {
                    @Parameter(
                            name = "formParams",
                            description = "The from paramter value map that is been used to create a new entity object.",
                            in = ParameterIn.DEFAULT),
                    @Parameter(
                            name = API.PARAM_INSTITUTION_ID,
                            description = "The institution identifier of the request.\n"
                                    + "Default is the institution identifier of the institution of the current user"),
            })
    @RequestMapping(
            method = RequestMethod.POST,
            consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    public T create(
            @RequestParam final MultiValueMap<String, String> allRequestParams,
            @RequestParam(
                    name = API.PARAM_INSTITUTION_ID,
                    required = true,
                    defaultValue = UserService.USERS_INSTITUTION_AS_DEFAULT) final Long institutionId,
            final HttpServletRequest request) {

        // check write privilege for requested institution and concrete entityType
        this.checkWritePrivilege(institutionId);

        final POSTMapper postMap = new POSTMapper(allRequestParams, request.getQueryString())
                .putIfAbsent(API.PARAM_INSTITUTION_ID, String.valueOf(institutionId));

        final M requestModel = this.createNew(postMap);

        return this.checkCreateAccess(requestModel)
                .flatMap(this::validForCreate)
                .flatMap(this.entityDAO::createNew)
                .flatMap(this::logCreate)
                .flatMap(this::notifyCreated)
                .getOrThrow();
    }

    // ****************
    // * PUT (save)
    // ****************

    @Operation(
            summary = "Modifies an already existing entity object of the specific type.",
            description = "This expects " + MediaType.APPLICATION_JSON_VALUE +
                    " format for the response data and verifies consistencies " +
                    "within the definition of the specific entity object type. " +
                    "Missing (NULL) parameter that are not mandatory will be ignored and the original value will not be affected",
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    content = { @Content(mediaType = MediaType.APPLICATION_JSON_VALUE) }))
    @RequestMapping(
            method = RequestMethod.PUT,
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    public T savePut(@Valid @RequestBody final T modifyData) {
        return this.checkModifyAccess(modifyData)
                .flatMap(this::validForSave)
                .flatMap(this.entityDAO::save)
                .flatMap(this::logModify)
                .flatMap(this::notifySaved)
                .getOrThrow();
    }

    // ************************
    // * DELETE (hard-delete)
    // ************************

    @Operation(
            summary = "Deletes a single entity (and all its dependencies) by its modelId.",
            description = "To check or report what dependent object also would be deleted for a certain entity object, "
                    +
                    "please use the dependency endpoint to get a report of all dependent entity objects.",
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    content = { @Content(mediaType = MediaType.APPLICATION_FORM_URLENCODED_VALUE) }),
            parameters = {
                    @Parameter(
                            name = API.PARAM_MODEL_ID,
                            description = "The model identifier of the entity object to get.",
                            in = ParameterIn.PATH),
                    @Parameter(
                            name = API.PARAM_BULK_ACTION_ADD_INCLUDES,
                            description = "Indicates if the following 'includes' parameter shall be processed or not.\n The default is false "),
                    @Parameter(
                            name = API.PARAM_BULK_ACTION_INCLUDES,
                            description = "A comma separated list of names of the EntityType enumeration that defines all entity types that shall be included in the result.")
            })
    @RequestMapping(
            path = API.MODEL_ID_VAR_PATH_SEGMENT,
            method = RequestMethod.DELETE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    public EntityProcessingReport hardDelete(
            @PathVariable final String modelId,
            @RequestParam(name = API.PARAM_BULK_ACTION_ADD_INCLUDES, defaultValue = "false") final boolean addIncludes,
            @RequestParam(name = API.PARAM_BULK_ACTION_INCLUDES, required = false) final List<String> includes) {

        return this.entityDAO.byModelId(modelId)
                .flatMap(this::checkWriteAccess)
                .flatMap(this::validForDelete)
                .flatMap(this::logDelete)
                .flatMap(entity -> bulkDelete(entity, convertToEntityType(addIncludes, includes)))
                .flatMap(this::notifyDeleted)
                .flatMap(pair -> this.logBulkAction(pair.b))
                .getOrThrow();
    }

    @Operation(
            summary = "Force deletes a single entity (and all its dependencies) by its modelId.",
            description = "To check or report what dependent object also would be deleted for a certain entity object, "
                    +
                    "please use the dependency endpoint to get a report of all dependent entity objects.",
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    content = { @Content(mediaType = MediaType.APPLICATION_FORM_URLENCODED_VALUE) }),
            parameters = {
                    @Parameter(
                            name = API.PARAM_MODEL_ID,
                            description = "The model identifier of the entity object to get.",
                            in = ParameterIn.PATH),
                    @Parameter(
                            name = API.PARAM_BULK_ACTION_ADD_INCLUDES,
                            description = "Indicates if the following 'includes' parameter shall be processed or not.\n The default is false "),
                    @Parameter(
                            name = API.PARAM_BULK_ACTION_INCLUDES,
                            description = "A comma separated list of names of the EntityType enumeration that defines all entity types that shall be included in the result.")
            })
    @RequestMapping(
            path = API.MODEL_ID_VAR_PATH_SEGMENT + API.FORCE_PATH_SEGMENT,
            method = RequestMethod.DELETE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    public EntityProcessingReport forceHardDelete(
            @PathVariable final String modelId,
            @RequestParam(name = API.PARAM_BULK_ACTION_ADD_INCLUDES, defaultValue = "false") final boolean addIncludes,
            @RequestParam(name = API.PARAM_BULK_ACTION_INCLUDES, required = false) final List<String> includes) {

        return this.entityDAO.byModelId(modelId)
                .flatMap(this::checkWriteAccess)
                .flatMap(this::logDelete)
                .flatMap(entity -> bulkDelete(entity, convertToEntityType(addIncludes, includes)))
                .flatMap(this::notifyDeleted)
                .flatMap(pair -> this.logBulkAction(pair.b))
                .getOrThrow();
    }

    // **************************
    // * DELETE ALL (hard-delete)
    // **************************

    @Operation(
            summary = "Deletes all given entity (and all its dependencies) by a given list of model identifiers.",
            description = "To check or report what dependent object also would be deleted for a certain entity object, "
                    +
                    "please use the dependency endpoint to get a report of all dependent entity objects.",
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    content = { @Content(mediaType = MediaType.APPLICATION_FORM_URLENCODED_VALUE) }),
            parameters = {
                    @Parameter(
                            name = API.PARAM_MODEL_ID_LIST,
                            description = "The list of model identifiers of specific entity type to delete.",
                            in = ParameterIn.QUERY),
                    @Parameter(
                            name = API.PARAM_BULK_ACTION_ADD_INCLUDES,
                            description = "Indicates if the following 'includes' paramerer shall be processed or not.\n The default is false "),
                    @Parameter(
                            name = API.PARAM_BULK_ACTION_INCLUDES,
                            description = "A comma separated list of names of the EntityType enumeration that defines all entity types that shall be included in the result.")
            })
    @RequestMapping(
            method = RequestMethod.DELETE,
            consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    public EntityProcessingReport hardDeleteAll(
            @RequestParam(name = API.PARAM_MODEL_ID_LIST) final List<String> ids,
            @RequestParam(name = API.PARAM_BULK_ACTION_ADD_INCLUDES, defaultValue = "false") final boolean addIncludes,
            @RequestParam(name = API.PARAM_BULK_ACTION_INCLUDES, required = false) final List<String> includes,
            @RequestParam(
                    name = API.PARAM_INSTITUTION_ID,
                    required = true,
                    defaultValue = UserService.USERS_INSTITUTION_AS_DEFAULT) final Long institutionId) {

        this.checkWritePrivilege(institutionId);

        if (ids == null || ids.isEmpty()) {
            return EntityProcessingReport.ofEmptyError();
        }

        final EntityType entityType = this.entityDAO.entityType();

        final Collection<EntityKey> sources = ids.stream()
                .map(id -> {
                    return this.entityDAO.byModelId(id)
                            .flatMap(this::validForDelete)
                            .getOr(null);
                })
                .filter(Objects::nonNull)
                .map(ModelIdAware::getModelId)
                .map(id -> new EntityKey(id, entityType))
                .collect(Collectors.toList());

        final BulkAction bulkAction = new BulkAction(
                BulkActionType.HARD_DELETE,
                entityType,
                sources,
                convertToEntityType(addIncludes, includes));

        return this.bulkActionService
                .createReport(bulkAction)
                .flatMap(this::logBulkAction)
                .flatMap(this::notifyAllDeleted)
                .getOrThrow();
    }

    protected void populateFilterMap(final FilterMap filterMap, final Long institutionId, final String sort) {
        // If current user has no read access for specified entity type within other institution
        // then the current users institutionId is put as a SQL filter criteria attribute to extends query performance
        if (!this.authorization.hasGrant(PrivilegeType.READ, getGrantEntityType())) {
            filterMap.putIfAbsent(API.PARAM_INSTITUTION_ID, String.valueOf(institutionId));
        }

        // If sorting is on institution name we need to join the institution table
        if (sort != null && sort.contains(Entity.FILTER_ATTR_INSTITUTION)) {
            filterMap.putIfAbsent(FilterMap.ATTR_ADD_INSITUTION_JOIN, Constants.TRUE_STRING);
        }
    }

    protected EnumSet<EntityType> convertToEntityType(final boolean addIncludes, final List<String> includes) {
        final EnumSet<EntityType> includeDependencies = (includes != null)
                ? (includes.isEmpty())
                        ? EnumSet.noneOf(EntityType.class)
                        : EnumSet.copyOf(includes.stream().map(name -> {
                            try {
                                return EntityType.valueOf(name);
                            } catch (final Exception e) {
                                return null;
                            }
                        })
                                .filter(Objects::nonNull)
                                .collect(Collectors.toSet()))
                : (addIncludes) ? EnumSet.noneOf(EntityType.class) : null;
        return includeDependencies;
    }

    protected Result<Pair<T, EntityProcessingReport>> bulkDelete(
            final T entity,
            final EnumSet<EntityType> includeDependencies) {

        final BulkAction bulkAction = new BulkAction(
                BulkActionType.HARD_DELETE,
                entity.entityType(),
                Arrays.asList(new EntityName(entity.getModelId(), entity.entityType(), entity.getName())),
                includeDependencies);

        return Result.tryCatch(() -> new Pair<>(
                entity,
                this.bulkActionService
                        .createReport(bulkAction)
                        .getOrThrow()));
    }

    protected void checkReadPrivilege(final Long institutionId) {
        this.authorization.check(
                PrivilegeType.READ,
                getGrantEntityType(),
                institutionId);
    }

    protected void checkModifyPrivilege(final Long institutionId) {
        this.authorization.check(
                PrivilegeType.MODIFY,
                getGrantEntityType(),
                institutionId);
    }

    protected void checkWritePrivilege(final Long institutionId) {
        this.authorization.check(
                PrivilegeType.WRITE,
                getGrantEntityType(),
                institutionId);
    }

    protected Result<Collection<T>> getAll(final FilterMap filterMap) {
        return this.entityDAO.allMatching(
                filterMap,
                this::hasReadAccess);
    }

    protected Result<T> notifyCreated(final T entity) {
        return Result.of(entity);
    }

    protected Result<M> validForCreate(final M entity) {
        if (entity.getModelId() == null) {
            return this.beanValidationService.validateBean(entity);
        } else {
            return Result.ofError(
                    new APIConstraintViolationException("Model identifier already defined: " + entity.getModelId()));
        }
    }

    protected Result<T> validForSave(final T entity) {
        if (entity.getModelId() != null) {
            return Result.of(entity);
        } else {
            return Result.ofError(new APIConstraintViolationException("Missing model identifier"));
        }
    }

    protected Result<T> validForDelete(final T entity) {
        if (entity.getModelId() != null) {
            return Result.of(entity);
        } else {
            return Result.ofError(new APIConstraintViolationException("Missing model identifier"));
        }
    }

    protected Result<T> notifySaved(final T entity) {
        return Result.of(entity);
    }

    protected Result<Pair<T, EntityProcessingReport>> notifyDeleted(final Pair<T, EntityProcessingReport> pair) {
        return Result.of(pair);
    }

    protected Result<EntityProcessingReport> notifyAllDeleted(final EntityProcessingReport pair) {
        return Result.of(pair);
    }

    protected Result<T> checkReadAccess(final T entity) {
        final GrantEntity grantEntity = toGrantEntity(entity);
        if (grantEntity != null) {
            this.authorization.checkRead(grantEntity);
        }
        return Result.of(entity);
    }

    protected boolean hasReadAccess(final T entity) {
        final GrantEntity grantEntity = toGrantEntity(entity);
        if (grantEntity != null) {
            return this.authorization.hasReadGrant(grantEntity);
        }

        return true;
    }

    protected Result<T> checkModifyAccess(final T entity) {
        final GrantEntity grantEntity = toGrantEntity(entity);
        if (grantEntity != null) {
            this.authorization.checkModify(grantEntity);
        }
        return Result.of(entity);
    }

    protected Result<T> checkWriteAccess(final T entity) {
        final GrantEntity grantEntity = toGrantEntity(entity);
        if (grantEntity != null) {
            this.authorization.checkWrite(grantEntity);
        }
        return Result.of(entity);
    }

    /** Checks creation (write) privileges for a given Entity.
     * Usually the GrantEntity and the Entity instance are the same if the Entity extends from GrantEntity.
     * Otherwise the implementing EntityController must override this method and resolve the
     * related GrantEntity for a given Entity.
     * For example, the GrantEntity of Indicator is the related Exam
     *
     * @param entity the Entity to check creation/write access for
     * @return Result of the access check containing either the original entity or an error if no access granted */
    protected Result<M> checkCreateAccess(final M entity) {
        if (entity instanceof GrantEntity) {
            this.authorization.checkWrite((GrantEntity) entity);
            return Result.of(entity);
        }

        return Result.ofError(new IllegalArgumentException("Entity instance is not of type GrantEntity. "
                + "Do override the checkCreateAccess method from EntityController within the specific -Controller implementation"));
    }

    /** Gets the GrantEntity instance for a given Entity instance.
     * Usually the GrantEntity and the Entity instance are the same if the Entity extends from GrantEntity.
     * Otherwise the implementing EntityController must override this method and resolve the
     * related GrantEntity for a given Entity.
     * For example, the GrantEntity of Indicator is the related Exam
     *
     * @param entity the Entity to get the related GrantEntity for
     * @return the GrantEntity instance for a given Entity instance */
    protected GrantEntity toGrantEntity(final T entity) {
        if (entity == null) {
            return null;
        }

        if (entity instanceof GrantEntity) {
            return (GrantEntity) entity;
        }

        throw new IllegalArgumentException("Entity instance is not of type GrantEntity. "
                + "Do override the toGrantEntity method from EntityController within the specific -Controller implementation");
    }

    /** Get the EntityType of the GrantEntity that is used for grant checks of the concrete Entity.
     * <p>
     * NOTE: override this if the EntityType of the GrantEntity is not the same as the Entity itself.
     * For example, the Exam is the GrantEntity of an Indicator
     *
     * @return the EntityType of the GrantEntity that is used for grant checks of the concrete Entity */
    protected EntityType getGrantEntityType() {
        return this.entityDAO.entityType();
    }

    /** Makes a CREATE user activity log for the specified entity.
     * This may be overwritten if the create user activity log should be skipped.
     *
     * @param entity the Entity instance
     * @return Result of entity */
    protected Result<T> logCreate(final T entity) {
        return this.userActivityLogDAO.logCreate(entity);
    }

    /** Makes a MODIFY user activity log for the specified entity.
     * This may be overwritten if the create user activity log should be skipped.
     *
     * @param entity the Entity instance
     * @return Result refer to the logged Entity instance or to an error if happened */
    protected Result<T> logModify(final T entity) {
        return this.userActivityLogDAO.logModify(entity);
    }

    /** Makes a DELETE user activity log for the specified entity.
     * This may be overwritten if the create user activity log should be skipped.
     *
     * @param entity the Entity instance
     * @return Result refer to the logged Entity instance or to an error if happened */
    protected Result<T> logDelete(final T entity) {
        return this.userActivityLogDAO.logDelete(entity);
    }

    /** Makes user activity log for a bulk action.
     *
     * @param bulkActionReport the EntityProcessingReport
     * @return Result of entity */
    protected Result<EntityProcessingReport> logBulkAction(final EntityProcessingReport bulkActionReport) {

        this.userActivityLogDAO.logBulkAction(bulkActionReport)
                .onError(error -> log.warn("Failed to log audit for bulk action: {}", bulkActionReport, error));

        return Result.of(bulkActionReport);
    }

    /** Implements the creation of a new entity from the post parameters given within the POSTMapper
     *
     * @param postParams contains all post parameter from request
     * @return new created Entity instance */
    protected abstract M createNew(POSTMapper postParams);

    /** Gets the MyBatis SqlTable for the concrete Entity
     *
     * @return the MyBatis SqlTable for the concrete Entity */
    protected abstract SqlTable getSQLTableOfEntity();

}
