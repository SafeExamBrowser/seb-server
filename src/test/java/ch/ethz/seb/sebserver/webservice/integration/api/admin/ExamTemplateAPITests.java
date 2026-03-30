package ch.ethz.seb.sebserver.webservice.integration.api.admin;

import ch.ethz.seb.sebserver.gbl.api.API;
import ch.ethz.seb.sebserver.gbl.api.EntityType;
import ch.ethz.seb.sebserver.gbl.model.Domain;
import ch.ethz.seb.sebserver.gbl.model.EntityKey;
import ch.ethz.seb.sebserver.gbl.model.Page;
import ch.ethz.seb.sebserver.gbl.model.exam.*;
import ch.ethz.seb.sebserver.gbl.util.Result;
import ch.ethz.seb.sebserver.webservice.servicelayer.session.impl.proctoring.ScreenProctoringAPIBinding;
import com.fasterxml.jackson.core.type.TypeReference;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.jdbc.Sql;

import java.util.*;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;


@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class ExamTemplateAPITests extends AdministrationAPIIntegrationTester {

    @MockBean
    ScreenProctoringAPIBinding screenProctoringAPIBinding;


    @Test
    @Order(1)
    @Sql(scripts = { "classpath:schema-test.sql", "classpath:data-test.sql", "classpath:data-test-additional.sql" })
    public void test_01_createExamTemplate() throws Exception {
        when(screenProctoringAPIBinding.testConnection(any())).thenReturn(Result.EMPTY);

        final String sebAdminAccess = getSebAdminAccess();

        final IndicatorTemplate indicatorTemplate = new IndicatorTemplate(
                null,
                null,
                "Indicator 1",
                Indicator.IndicatorType.BATTERY_STATUS,
                "ffffff",
                null,
                null,
                List.of(new Indicator.Threshold(40d, "ffaaff", null))
        );

        final ClientGroupTemplate clientGroup1 = new ClientGroupTemplate(
                null,
                null,
                "SEB Group 1",
                ClientGroupData.ClientGroupType.CLIENT_OS,
                "ffffff",
                null,
                null,
                null,
                ClientGroupData.ClientOS.WINDOWS,
                null,
                null,
                true
        );
        final ClientGroupTemplate clientGroup2 = new ClientGroupTemplate(
                null,
                null,
                "SEB Group 2",
                ClientGroupData.ClientGroupType.CLIENT_OS,
                "ffffff",
                null,
                null,
                null,
                ClientGroupData.ClientOS.MAC_OS,
                null,
                null,
                false
        );

        Map<String, String> examAttributes = new HashMap<>();
        examAttributes.put("enableScreenProctoring", "true");
        examAttributes.put("spsCollectingStrategy", "APPLY_SEB_GROUPS");
        examAttributes.put("spsCollectingGroupName", "Fallback Group");

        ExamTemplate examTemplate = new ExamTemplate(
                null,
                1L,
                "Test ExamTemplate",
                "Test ExamTemplate",
                Exam.ExamType.BYOD,
                Collections.singletonList("admin"),
                null,
                false,
                false,
                1L,
                List.of(indicatorTemplate),
                Arrays.asList(clientGroup1, clientGroup2),
                examAttributes
        );

        // new create the ExamTemplate
        ExamTemplate createdExamTemplate = new RestAPITestHelper()
                .withAccessToken(sebAdminAccess)
                .withPath(API.EXAM_TEMPLATE_ENDPOINT + API.EXAM_TEMPLATE_FULL_CREATE)
                .withMethod(HttpMethod.POST)
                .withBodyJson(examTemplate)
                .withExpectedStatus(HttpStatus.OK)
                .getAsObject(new TypeReference<ExamTemplate>() {
                });

        assertNotNull(createdExamTemplate);
        assertNotNull(createdExamTemplate.id);
        assertEquals(
                "ExamTemplate [" +
                        "id=1, " +
                        "institutionId=1, " +
                        "name=Test ExamTemplate, " +
                        "description=Test ExamTemplate, " +
                        "examType=BYOD, " +
                        "supporter=[admin], " +
                        "configTemplateId=2, " +
                        "indicatorTemplates=[Indicator [id=0, examTemplateId=1, name=Indicator 1, type=BATTERY_STATUS, defaultColor=ffffff, defaultIcon=null, tags=null, " +
                        "thresholds=[Threshold [value=40.0, color=ffaaff, icon=null]]]], " +
                        "clientGroupTemplates=[" +
                        "ClientGroupTemplate{id=0, examTemplateId=1, name='SEB Group 1', type=CLIENT_OS, color='ffffff', icon='null', ipRangeStart='null', ipRangeEnd='null', clientOS=WINDOWS, nameRangeStartLetter=null, nameRangeEndLetter=null, screenProctoringEnabled=true}, " +
                        "ClientGroupTemplate{id=1, examTemplateId=1, name='SEB Group 2', type=CLIENT_OS, color='ffffff', icon='null', ipRangeStart='null', ipRangeEnd='null', clientOS=MAC_OS, nameRangeStartLetter=null, nameRangeEndLetter=null, screenProctoringEnabled=false}], " +
                        "examAttributes={spsCollectingStrategy=APPLY_SEB_GROUPS, spsCollectingGroupName=Fallback Group, enableScreenProctoring=true, spsSEBGroupsSelection=0}, " +
                        "institutionalDefault=false, " +
                        "lmsIntegration=false]",
                createdExamTemplate.toString());

    }

    @Test
    @Order(2)
    public void test_02_getExamTemplates() throws Exception {
        final String sebAdminAccess = getSebAdminAccess();

        // get Page gets models with only th ebase data
        Page<ExamTemplate> page = new RestAPITestHelper()
                .withAccessToken(sebAdminAccess)
                .withPath(API.EXAM_TEMPLATE_ENDPOINT)
                .withMethod(HttpMethod.GET)
                .withExpectedStatus(HttpStatus.OK)
                .getAsObject(new TypeReference<Page<ExamTemplate>>() {
                });

        assertNotNull(page);
        assertNotNull(page.content);
        Collection<ExamTemplate> content = page.getContent();
        assertFalse(content.isEmpty());
        assertEquals(1, content.size());
        assertEquals(
                "ExamTemplate [" +
                        "id=1, " +
                        "institutionId=1, " +
                        "name=Test ExamTemplate, " +
                        "description=Test ExamTemplate, " +
                        "examType=BYOD, supporter=[admin], " +
                        "configTemplateId=2, " +
                        "indicatorTemplates=[Indicator [id=0, examTemplateId=1, name=Indicator 1, type=BATTERY_STATUS, defaultColor=ffffff, defaultIcon=null, tags=null, " +
                        "thresholds=[Threshold [value=40.0, color=ffaaff, icon=null]]]], clientGroupTemplates=[], " +
                        "examAttributes={}, " +
                        "institutionalDefault=false, " +
                        "lmsIntegration=false]",
                content.iterator().next().toString());

        // get single with full data
        ExamTemplate examTemplate = new RestAPITestHelper()
                .withAccessToken(sebAdminAccess)
                .withPath(API.EXAM_TEMPLATE_ENDPOINT)
                .withPath("1")
                .withMethod(HttpMethod.GET)
                .withExpectedStatus(HttpStatus.OK)
                .getAsObject(new TypeReference<ExamTemplate>() {
                });

        assertNotNull(examTemplate);
        assertEquals(
                "ExamTemplate [" +
                        "id=1, " +
                        "institutionId=1, " +
                        "name=Test ExamTemplate, " +
                        "description=Test ExamTemplate, " +
                        "examType=BYOD, " +
                        "supporter=[admin], " +
                        "configTemplateId=2, " +
                        "indicatorTemplates=[Indicator [id=0, examTemplateId=1, name=Indicator 1, type=BATTERY_STATUS, defaultColor=ffffff, defaultIcon=null, tags=null, " +
                        "thresholds=[Threshold [value=40.0, color=ffaaff, icon=null]]]], " +
                        "clientGroupTemplates=[" +
                        "ClientGroupTemplate{id=0, examTemplateId=1, name='SEB Group 1', type=CLIENT_OS, color='ffffff', icon='null', ipRangeStart='null', ipRangeEnd='null', clientOS=WINDOWS, nameRangeStartLetter=null, nameRangeEndLetter=null, screenProctoringEnabled=true}, " +
                        "ClientGroupTemplate{id=1, examTemplateId=1, name='SEB Group 2', type=CLIENT_OS, color='ffffff', icon='null', ipRangeStart='null', ipRangeEnd='null', clientOS=MAC_OS, nameRangeStartLetter=null, nameRangeEndLetter=null, screenProctoringEnabled=false}], " +
                        "examAttributes={spsCollectingStrategy=APPLY_SEB_GROUPS, spsCollectingGroupName=Fallback Group, enableScreenProctoring=true, spsSEBGroupsSelection=0}, " +
                        "institutionalDefault=false, " +
                        "lmsIntegration=false]",
                examTemplate.toString());
    }

    @Test
    @Order(3)
    public void test_03_modifyBaseData() throws Exception {
        final String sebAdminAccess = getSebAdminAccess();
        when(screenProctoringAPIBinding.testConnection(any())).thenReturn(Result.EMPTY);

        // modify some data and disable SPS
        Map<String, String> examAttributes = new HashMap<>();
        examAttributes.put("enableScreenProctoring", "false");

        ExamTemplate examTemplate = new ExamTemplate(
                1L,
                1L,
                "Test ExamTemplate Modify Name",
                "Test ExamTemplate Modify Name",
                Exam.ExamType.MANAGED,
                null,
                null,
                false,
                false,
                1L,
                null,
                null,
                examAttributes
        );

        ExamTemplate modifiedExamTemplate = new RestAPITestHelper()
                .withAccessToken(sebAdminAccess)
                .withPath(API.EXAM_TEMPLATE_ENDPOINT)
                .withMethod(HttpMethod.PUT)
                .withBodyJson(examTemplate)
                .withExpectedStatus(HttpStatus.OK)
                .getAsObject(new TypeReference<ExamTemplate>() {
                });

        assertNotNull(modifiedExamTemplate);
        assertNotNull(modifiedExamTemplate.id);
        assertEquals(
                "ExamTemplate [" +
                        "id=1, " +
                        "institutionId=1, " +
                        "name=Test ExamTemplate Modify Name, " +
                        "description=Test ExamTemplate Modify Name, " +
                        "examType=MANAGED, " +
                        "supporter=null, " +
                        "configTemplateId=2, " +
                        "indicatorTemplates=[Indicator [id=0, examTemplateId=1, name=Indicator 1, type=BATTERY_STATUS, defaultColor=ffffff, defaultIcon=null, tags=null, " +
                        "thresholds=[Threshold [value=40.0, color=ffaaff, icon=null]]]], " +
                        "clientGroupTemplates=[" +
                        "ClientGroupTemplate{id=0, examTemplateId=1, name='SEB Group 1', type=CLIENT_OS, color='ffffff', icon='null', ipRangeStart='null', ipRangeEnd='null', clientOS=WINDOWS, nameRangeStartLetter=null, nameRangeEndLetter=null, screenProctoringEnabled=false}, " +
                        "ClientGroupTemplate{id=1, examTemplateId=1, name='SEB Group 2', type=CLIENT_OS, color='ffffff', icon='null', ipRangeStart='null', ipRangeEnd='null', clientOS=MAC_OS, nameRangeStartLetter=null, nameRangeEndLetter=null, screenProctoringEnabled=false}], " +
                        "examAttributes={spsCollectingStrategy=APPLY_SEB_GROUPS, spsCollectingGroupName=Fallback Group, enableScreenProctoring=false, spsSEBGroupsSelection=0}, " +
                        "institutionalDefault=false, " +
                        "lmsIntegration=false]",
                modifiedExamTemplate.toString());
    }

    @Test
    @Order(4)
    public void test_04_create_modify_deleteIndicator() throws Exception {
        final String sebAdminAccess = getSebAdminAccess();
        when(screenProctoringAPIBinding.testConnection(any())).thenReturn(Result.EMPTY);

        // create new Indicator Template for Existing Exam Template
        IndicatorTemplate indicatorTemplate = new RestAPITestHelper()
                .withAccessToken(sebAdminAccess)
                .withPath(API.EXAM_TEMPLATE_ENDPOINT + API.EXAM_INDICATOR_ENDPOINT)
                .withMethod(HttpMethod.POST)
                .withAttribute(IndicatorTemplate.ATTR_EXAM_TEMPLATE_ID, "1")
                .withAttribute(Domain.INDICATOR.ATTR_NAME, "New Indicator Test")
                .withAttribute(Domain.INDICATOR.ATTR_TYPE, "BATTERY_STATUS")
                .withAttribute(Domain.INDICATOR.ATTR_COLOR, "aaaaaa")
                .withAttribute("thresholds", "40|aaaaaa")
                .withAttribute("thresholds", "20|bbaaaa")
                .withAttribute("thresholds", "10|ccaaaa")
                .withExpectedStatus(HttpStatus.OK)
                .getAsObject(new TypeReference<IndicatorTemplate>() {
                });

        // check new IndicatorTemplate
        assertNotNull(indicatorTemplate);
        assertEquals(
                "Indicator [id=1, examTemplateId=1, name=New Indicator Test, type=BATTERY_STATUS, defaultColor=aaaaaa, defaultIcon=null, tags=null, " +
                        "thresholds=[" +
                        "Threshold [value=40.0, color=aaaaaa, icon=null], " +
                        "Threshold [value=20.0, color=bbaaaa, icon=null], " +
                        "Threshold [value=10.0, color=ccaaaa, icon=null]]]",
                indicatorTemplate.toString());
        // check whole ExamTemplate
        ExamTemplate examTemplate = new RestAPITestHelper()
                .withAccessToken(sebAdminAccess)
                .withPath(API.EXAM_TEMPLATE_ENDPOINT)
                .withPath("/1")
                .withMethod(HttpMethod.GET)
                .withExpectedStatus(HttpStatus.OK)
                .getAsObject(new TypeReference<ExamTemplate>() {
                });
        assertNotNull(examTemplate.indicatorTemplates);
        assertEquals(2, examTemplate.indicatorTemplates.size());
        Iterator<IndicatorTemplate> iterator = examTemplate.indicatorTemplates.iterator();
        assertEquals("Indicator [id=0, examTemplateId=1, name=Indicator 1, type=BATTERY_STATUS, defaultColor=ffffff, defaultIcon=null, tags=null, thresholds=[Threshold [value=40.0, color=ffaaff, icon=null]]]", iterator.next().toString());
        assertEquals("Indicator [id=1, examTemplateId=1, name=New Indicator Test, type=BATTERY_STATUS, defaultColor=aaaaaa, defaultIcon=null, tags=null, " +
                "thresholds=[" +
                "Threshold [value=40.0, color=aaaaaa, icon=null], " +
                "Threshold [value=20.0, color=bbaaaa, icon=null], " +
                "Threshold [value=10.0, color=ccaaaa, icon=null]]]",
                iterator.next().toString());

        // modify Indicator Template on existing Exam Template
        final IndicatorTemplate modifyIndicator = new IndicatorTemplate(
                1L,
                1L,
                "New Indicator Test Modified!!!",
                Indicator.IndicatorType.BATTERY_STATUS,
                "ffffff",
                null,
                null,
                List.of(new Indicator.Threshold(40d, "ffaaff", null))
        );
        IndicatorTemplate modifiedIndicator = new RestAPITestHelper()
                .withAccessToken(sebAdminAccess)
                .withPath(API.EXAM_TEMPLATE_ENDPOINT + API.EXAM_INDICATOR_ENDPOINT)
                .withMethod(HttpMethod.PUT)
                .withBodyJson(modifyIndicator)
                .withExpectedStatus(HttpStatus.OK)
                .getAsObject(new TypeReference<IndicatorTemplate>() {
                });
        assertNotNull(modifiedIndicator);
        assertEquals("Indicator [id=1, examTemplateId=1, name=New Indicator Test Modified!!!, type=BATTERY_STATUS, defaultColor=ffffff, defaultIcon=null, tags=null, thresholds=[Threshold [value=40.0, color=ffaaff, icon=null]]]", modifiedIndicator.toString());
        // check whole ExamTemplate
        examTemplate = new RestAPITestHelper()
                .withAccessToken(sebAdminAccess)
                .withPath(API.EXAM_TEMPLATE_ENDPOINT)
                .withPath("/1")
                .withMethod(HttpMethod.GET)
                .withExpectedStatus(HttpStatus.OK)
                .getAsObject(new TypeReference<ExamTemplate>() {
                });
        assertNotNull(examTemplate.indicatorTemplates);
        assertEquals(2, examTemplate.indicatorTemplates.size());
        iterator = examTemplate.indicatorTemplates.iterator();
        assertEquals("Indicator [id=0, examTemplateId=1, name=Indicator 1, type=BATTERY_STATUS, defaultColor=ffffff, defaultIcon=null, tags=null, thresholds=[Threshold [value=40.0, color=ffaaff, icon=null]]]", iterator.next().toString());
        assertEquals("Indicator [id=1, examTemplateId=1, name=New Indicator Test Modified!!!, type=BATTERY_STATUS, defaultColor=ffffff, defaultIcon=null, tags=null, " +
                "thresholds=[" +
                "Threshold [value=40.0, color=ffaaff, icon=null]]]",
                iterator.next().toString());


        // delete Indicator Template from existing Exam Template
        EntityKey deletedKey = new RestAPITestHelper()
                .withAccessToken(sebAdminAccess)
                .withPath(API.EXAM_TEMPLATE_ENDPOINT)
                .withPath("/1") // delete on Exam Template 1
                .withPath(API.EXAM_TEMPLATE_INDICATOR_PATH_SEGMENT)
                .withPath("/1") // delete the Indicator Template 1
                .withMethod(HttpMethod.DELETE)
                .withExpectedStatus(HttpStatus.OK)
                .getAsObject(new TypeReference<EntityKey>() {
                });

        assertNotNull(deletedKey);
        assertSame(deletedKey.entityType, EntityType.INDICATOR);
        assertEquals("1", deletedKey.modelId);
        // check whole ExamTemplate
        examTemplate = new RestAPITestHelper()
                .withAccessToken(sebAdminAccess)
                .withPath(API.EXAM_TEMPLATE_ENDPOINT)
                .withPath("/1")
                .withMethod(HttpMethod.GET)
                .withExpectedStatus(HttpStatus.OK)
                .getAsObject(new TypeReference<ExamTemplate>() {
                });
        assertNotNull(examTemplate.indicatorTemplates);
        assertEquals(1, examTemplate.indicatorTemplates.size());
        iterator = examTemplate.indicatorTemplates.iterator();
        assertEquals("Indicator [id=0, examTemplateId=1, name=Indicator 1, type=BATTERY_STATUS, defaultColor=ffffff, defaultIcon=null, tags=null, thresholds=[Threshold [value=40.0, color=ffaaff, icon=null]]]", iterator.next().toString());
    }

    @Test
    @Order(5)
    public void test_05_create_modify_ClientGroup() throws Exception {
        final String sebAdminAccess = getSebAdminAccess();
        when(screenProctoringAPIBinding.testConnection(any())).thenReturn(Result.EMPTY);

        // create new Indicator Template for Existing Exam Template
        ClientGroupTemplate groupTemplate = new RestAPITestHelper()
                .withAccessToken(sebAdminAccess)
                .withPath(API.EXAM_TEMPLATE_ENDPOINT + API.EXAM_CLIENT_GROUP_ENDPOINT)
                .withMethod(HttpMethod.POST)
                .withAttribute(IndicatorTemplate.ATTR_EXAM_TEMPLATE_ID, "1")
                .withAttribute(Domain.CLIENT_GROUP.ATTR_NAME, "Mac New")
                .withAttribute(Domain.CLIENT_GROUP.ATTR_TYPE, "CLIENT_OS")
                .withAttribute(ClientGroupTemplate.ATTR_CLIENT_OS, "MAC_OS")
                .withAttribute(ClientGroupTemplate.ATTR_NAME_SPS_ENABLED, "true") // Note: since SPS is not enabled at all, this will be ignored
                .withExpectedStatus(HttpStatus.OK)
                .getAsObject(new TypeReference<ClientGroupTemplate>() {
                });

        // check new IndicatorTemplate
        assertNotNull(groupTemplate);
        assertEquals(
                "ClientGroupTemplate{id=2, examTemplateId=1, name='Mac New', type=CLIENT_OS, color='null', icon='null', ipRangeStart='null', ipRangeEnd='null', clientOS=MAC_OS, nameRangeStartLetter=null, nameRangeEndLetter=null, screenProctoringEnabled=false}",
                groupTemplate.toString());
        // check whole ExamTemplate
        ExamTemplate examTemplate = new RestAPITestHelper()
                .withAccessToken(sebAdminAccess)
                .withPath(API.EXAM_TEMPLATE_ENDPOINT)
                .withPath("/1")
                .withMethod(HttpMethod.GET)
                .withExpectedStatus(HttpStatus.OK)
                .getAsObject(new TypeReference<ExamTemplate>() {
                });
        assertNotNull(examTemplate.clientGroupTemplates);
        assertEquals(3, examTemplate.clientGroupTemplates.size());
        Iterator<ClientGroupTemplate> iterator = examTemplate.clientGroupTemplates.iterator();
        assertEquals("ClientGroupTemplate{id=0, examTemplateId=1, name='SEB Group 1', type=CLIENT_OS, color='ffffff', icon='null', ipRangeStart='null', ipRangeEnd='null', clientOS=WINDOWS, nameRangeStartLetter=null, nameRangeEndLetter=null, screenProctoringEnabled=false}",
                iterator.next().toString());
        assertEquals("ClientGroupTemplate{id=1, examTemplateId=1, name='SEB Group 2', type=CLIENT_OS, color='ffffff', icon='null', ipRangeStart='null', ipRangeEnd='null', clientOS=MAC_OS, nameRangeStartLetter=null, nameRangeEndLetter=null, screenProctoringEnabled=false}",
                iterator.next().toString());
        assertEquals("ClientGroupTemplate{id=2, examTemplateId=1, name='Mac New', type=CLIENT_OS, color='null', icon='null', ipRangeStart='null', ipRangeEnd='null', clientOS=MAC_OS, nameRangeStartLetter=null, nameRangeEndLetter=null, screenProctoringEnabled=false}",
                iterator.next().toString());

        // modify Indicator Template on existing Exam Template
        final ClientGroupTemplate modifyGroup = new ClientGroupTemplate(
                2L,
                1L,
                "Mac New Modified",
                ClientGroupData.ClientGroupType.CLIENT_OS,
                "ffffff",
                null,
                null,
                null,
                ClientGroupData.ClientOS.WINDOWS,
                null,
                null,
                false
        );
        ClientGroupTemplate modifiedGroup = new RestAPITestHelper()
                .withAccessToken(sebAdminAccess)
                .withPath(API.EXAM_TEMPLATE_ENDPOINT + API.EXAM_CLIENT_GROUP_ENDPOINT)
                .withMethod(HttpMethod.PUT)
                .withBodyJson(modifyGroup)
                .withExpectedStatus(HttpStatus.OK)
                .getAsObject(new TypeReference<ClientGroupTemplate>() {
                });
        assertNotNull(modifiedGroup);
        assertEquals("ClientGroupTemplate{id=2, examTemplateId=1, name='Mac New Modified', type=CLIENT_OS, color='ffffff', icon='null', ipRangeStart='null', ipRangeEnd='null', clientOS=WINDOWS, nameRangeStartLetter=null, nameRangeEndLetter=null, screenProctoringEnabled=false}", modifiedGroup.toString());
        // check whole ExamTemplate
        examTemplate = new RestAPITestHelper()
                .withAccessToken(sebAdminAccess)
                .withPath(API.EXAM_TEMPLATE_ENDPOINT)
                .withPath("/1")
                .withMethod(HttpMethod.GET)
                .withExpectedStatus(HttpStatus.OK)
                .getAsObject(new TypeReference<ExamTemplate>() {
                });
        assertNotNull(examTemplate.clientGroupTemplates);
        assertEquals(3, examTemplate.clientGroupTemplates.size());
        iterator = examTemplate.clientGroupTemplates.iterator();
        assertEquals("ClientGroupTemplate{id=0, examTemplateId=1, name='SEB Group 1', type=CLIENT_OS, color='ffffff', icon='null', ipRangeStart='null', ipRangeEnd='null', clientOS=WINDOWS, nameRangeStartLetter=null, nameRangeEndLetter=null, screenProctoringEnabled=false}",
                iterator.next().toString());
        assertEquals("ClientGroupTemplate{id=1, examTemplateId=1, name='SEB Group 2', type=CLIENT_OS, color='ffffff', icon='null', ipRangeStart='null', ipRangeEnd='null', clientOS=MAC_OS, nameRangeStartLetter=null, nameRangeEndLetter=null, screenProctoringEnabled=false}",
                iterator.next().toString());
        assertEquals("ClientGroupTemplate{id=2, examTemplateId=1, name='Mac New Modified', type=CLIENT_OS, color='ffffff', icon='null', ipRangeStart='null', ipRangeEnd='null', clientOS=WINDOWS, nameRangeStartLetter=null, nameRangeEndLetter=null, screenProctoringEnabled=false}",
                iterator.next().toString());

        // delete ClientGroup Template from existing Exam Template
        EntityKey deletedKey = new RestAPITestHelper()
                .withAccessToken(sebAdminAccess)
                .withPath(API.EXAM_TEMPLATE_ENDPOINT)
                .withPath("/1") // delete on Exam Template 1
                .withPath(API.EXAM_TEMPLATE_CLIENT_GROUP_PATH_SEGMENT)
                .withPath("/2") // delete the Indicator Template 1
                .withMethod(HttpMethod.DELETE)
                .withExpectedStatus(HttpStatus.OK)
                .getAsObject(new TypeReference<EntityKey>() {
                });

        assertNotNull(deletedKey);
        assertSame(deletedKey.entityType, EntityType.CLIENT_GROUP);
        assertEquals("2", deletedKey.modelId);

        // check whole ExamTemplate
        examTemplate = new RestAPITestHelper()
                .withAccessToken(sebAdminAccess)
                .withPath(API.EXAM_TEMPLATE_ENDPOINT)
                .withPath("/1")
                .withMethod(HttpMethod.GET)
                .withExpectedStatus(HttpStatus.OK)
                .getAsObject(new TypeReference<ExamTemplate>() {
                });
        assertNotNull(examTemplate.clientGroupTemplates);
        assertEquals(2, examTemplate.clientGroupTemplates.size());
        iterator = examTemplate.clientGroupTemplates.iterator();
        assertEquals("ClientGroupTemplate{id=0, examTemplateId=1, name='SEB Group 1', type=CLIENT_OS, color='ffffff', icon='null', ipRangeStart='null', ipRangeEnd='null', clientOS=WINDOWS, nameRangeStartLetter=null, nameRangeEndLetter=null, screenProctoringEnabled=false}",
                iterator.next().toString());
        assertEquals("ClientGroupTemplate{id=1, examTemplateId=1, name='SEB Group 2', type=CLIENT_OS, color='ffffff', icon='null', ipRangeStart='null', ipRangeEnd='null', clientOS=MAC_OS, nameRangeStartLetter=null, nameRangeEndLetter=null, screenProctoringEnabled=false}",
                iterator.next().toString());
    }

    @Test
    @Order(6)
    public void test_06_copyExamTemplate() throws Exception {
        final String sebAdminAccess = getSebAdminAccess();
        when(screenProctoringAPIBinding.testConnection(any())).thenReturn(Result.EMPTY);

        ExamTemplate copiedExamTemplate = new RestAPITestHelper()
                .withAccessToken(sebAdminAccess)
                .withPath(API.EXAM_TEMPLATE_ENDPOINT)
                .withPath("/1")
                .withPath(API.EXAM_TEMPLATE_COPY)
                .withMethod(HttpMethod.POST)
                .withExpectedStatus(HttpStatus.OK)
                .getAsObject(new TypeReference<ExamTemplate>() {
                });

        assertNotNull(copiedExamTemplate);
        assertNotNull(copiedExamTemplate.id);
        assertEquals(
                "ExamTemplate [" +
                        "id=2, " +
                        "institutionId=1, " +
                        "name=Test ExamTemplate Modify Name (copy), " +
                        "description=Test ExamTemplate Modify Name, " +
                        "examType=MANAGED, " +
                        "supporter=null, " +
                        "configTemplateId=3, " +
                        "indicatorTemplates=[" +
                        "Indicator [id=0, examTemplateId=2, name=Indicator 1, type=BATTERY_STATUS, defaultColor=ffffff, defaultIcon=null, tags=null, thresholds=[Threshold [value=40.0, color=ffaaff, icon=null]]]], " +
                        "clientGroupTemplates=[" +
                        "ClientGroupTemplate{id=0, examTemplateId=2, name='SEB Group 1', type=CLIENT_OS, color='ffffff', icon='null', ipRangeStart='null', ipRangeEnd='null', clientOS=WINDOWS, nameRangeStartLetter=null, nameRangeEndLetter=null, screenProctoringEnabled=false}, " +
                        "ClientGroupTemplate{id=1, examTemplateId=2, name='SEB Group 2', type=CLIENT_OS, color='ffffff', icon='null', ipRangeStart='null', ipRangeEnd='null', clientOS=MAC_OS, nameRangeStartLetter=null, nameRangeEndLetter=null, screenProctoringEnabled=false}], " +
                        "examAttributes={spsCollectingStrategy=APPLY_SEB_GROUPS, spsCollectingGroupName=Fallback Group, enableScreenProctoring=false, spsSEBGroupsSelection=0}, " +
                        "institutionalDefault=false, " +
                        "lmsIntegration=false]",
                copiedExamTemplate.toString());


    }


}
