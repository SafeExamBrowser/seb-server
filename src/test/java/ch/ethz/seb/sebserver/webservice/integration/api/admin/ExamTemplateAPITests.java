package ch.ethz.seb.sebserver.webservice.integration.api.admin;

import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;
import org.springframework.core.annotation.Order;
import org.springframework.test.context.jdbc.Sql;


@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class ExamTemplateAPITests extends AdministrationAPIIntegrationTester {

    @Test
    @Order(1)
    @Sql(scripts = { "classpath:schema-test.sql", "classpath:data-test.sql" })
    public void test_01_createExamTemplate() throws Exception {

    }

    @Test
    @Order(2)
    public void test_02_modifyBaseData() throws Exception {

    }
}
