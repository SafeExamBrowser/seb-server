package ch.ethz.seb.sebserver.webservice.integration.api.admin;

import ch.ethz.seb.sebserver.gbl.api.API;
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
    @Sql(scripts = { "classpath:schema-test.sql", "classpath:data-test.sql" })
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
                Arrays.asList(new Indicator.Threshold(40d, "ffaaff", null))
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
                        "configTemplateId=null, " +
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
                        "configTemplateId=null, " +
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
                        "configTemplateId=null, " +
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
                        "configTemplateId=null, " +
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

        // TODO
    }

    @Test
    @Order(5)
    public void test_05_create_modify_ClientGroup() throws Exception {
        final String sebAdminAccess = getSebAdminAccess();
        when(screenProctoringAPIBinding.testConnection(any())).thenReturn(Result.EMPTY);

        // TODO
    }
}
