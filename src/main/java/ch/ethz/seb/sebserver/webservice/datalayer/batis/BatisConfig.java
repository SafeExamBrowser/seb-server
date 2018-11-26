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

@Configuration
@MapperScan(basePackages = "ch.ethz.seb.sebserver.webservice.batis")
@WebServiceProfile
@Import(DataSourceAutoConfiguration.class)
public class BatisConfig {

    public static final String TRANSACTION_MANAGER = "transactionManager";
    public static final String SQL_SESSION_TEMPLATE = "sqlSessionTemplate";
    public static final String SQL_SESSION_FACTORY = "sqlSessionFactory";

    @Lazy
    @Bean(name = SQL_SESSION_FACTORY)
    public SqlSessionFactory sqlSessionFactory(final DataSource dataSource) throws Exception {
        final SqlSessionFactoryBean factoryBean = new SqlSessionFactoryBean();
        factoryBean.setDataSource(dataSource);
        return factoryBean.getObject();
    }

    @Lazy
    @Bean(name = SQL_SESSION_TEMPLATE)
    public SqlSessionTemplate sqlSessionTemplate(final DataSource dataSource) throws Exception {
        final SqlSessionTemplate sqlSessionTemplate = new SqlSessionTemplate(sqlSessionFactory(dataSource));
        return sqlSessionTemplate;
    }

    @Lazy
    @Bean(name = TRANSACTION_MANAGER)
    public DataSourceTransactionManager transactionManager(final DataSource dataSource) {
        final DataSourceTransactionManager transactionManager = new DataSourceTransactionManager();
        transactionManager.setDataSource(dataSource);
        return transactionManager;
    }

}
