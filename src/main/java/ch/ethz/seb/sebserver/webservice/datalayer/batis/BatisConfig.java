/*
 * Copyright (c) 2018 ETH Zürich, Educational Development and Technology (LET)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.webservice.datalayer.batis;

import javax.sql.DataSource;

import org.apache.ibatis.session.SqlSessionFactory;
import org.mybatis.spring.SqlSessionFactoryBean;
import org.mybatis.spring.SqlSessionTemplate;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Lazy;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;

import ch.ethz.seb.sebserver.gbl.profile.WebServiceProfile;

/** The MyBatis - Spring configuration
 *
 * All mapper- and model-classes in the specified sub-packages
 * are auto-generated from DB schema by an external generator
 *
 * MyBatis is used on the lowest data - layer as an OR-Mapper with great flexibility and a good
 * SQL builder interface.
 *
 * The Datasource is auto-configured by Spring and depends on the Spring property configuration so far */
@Configuration
@MapperScan(basePackages = "ch.ethz.seb.sebserver.webservice.datalayer.batis")
@WebServiceProfile
@Import(DataSourceAutoConfiguration.class)
public class BatisConfig {

    /** Name of the transaction manager bean for MyBatis based Spring controlled transactions */
    public static final String TRANSACTION_MANAGER = "transactionManager";
    /** Name of the sql session template bean of MyBatis */
    public static final String SQL_SESSION_TEMPLATE = "sqlSessionTemplate";
    /** Name of the sql session factory bean of MyBatis */
    public static final String SQL_SESSION_FACTORY = "sqlSessionFactory";

    /** Transaction manager bean for MyBatis based Spring controlled transactions */
    @Lazy
    @Bean(name = SQL_SESSION_FACTORY)
    public SqlSessionFactory sqlSessionFactory(final DataSource dataSource) throws Exception {
        final SqlSessionFactoryBean factoryBean = new SqlSessionFactoryBean();
        factoryBean.setDataSource(dataSource);
        return factoryBean.getObject();
    }

    /** SQL session template bean of MyBatis */
    @Lazy
    @Bean(name = SQL_SESSION_TEMPLATE)
    public SqlSessionTemplate sqlSessionTemplate(final DataSource dataSource) throws Exception {
        final SqlSessionTemplate sqlSessionTemplate = new SqlSessionTemplate(sqlSessionFactory(dataSource));
        return sqlSessionTemplate;
    }

    /** SQL session factory bean of MyBatis */
    @Lazy
    @Bean(name = TRANSACTION_MANAGER)
    public DataSourceTransactionManager transactionManager(final DataSource dataSource) {
        final DataSourceTransactionManager transactionManager = new DataSourceTransactionManager();
        transactionManager.setDataSource(dataSource);
        return transactionManager;
    }

}
