package com.jimbean.mybatis.plugin.autoconfigure;

import com.jimbean.mybatis.plugin.dialect.MySQLDialect;
import com.jimbean.mybatis.plugin.interceptor.GrayShadowInterceptor;
import com.jimbean.mybatis.plugin.interceptor.LogSqlInterceptor;
import com.jimbean.mybatis.plugin.interceptor.LimitInterceptor;
import com.jimbean.mybatis.plugin.interceptor.UpdateInterceptor;
import com.jimbean.mybatis.plugin.properties.MySQLPluginsProperties;
import lombok.extern.slf4j.Slf4j;
import org.apache.ibatis.session.SqlSessionFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.AutoConfigureOrder;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;

import javax.annotation.PostConstruct;
import java.util.List;

/**
 * 自动化配置
 *
 * @author zhangjb
 */
@Slf4j
@Configuration
@ConditionalOnBean({SqlSessionFactory.class})
@EnableConfigurationProperties(MySQLPluginsProperties.class)
@AutoConfigureOrder(Ordered.LOWEST_PRECEDENCE)
public class MybatisPluginAutoConfiguration {

    @Autowired
    private List<SqlSessionFactory> sqlSessionFactoryList;
    @Autowired
    private MySQLPluginsProperties mySQLPluginsProperties;

    public MybatisPluginAutoConfiguration() {}

    /**
     * 初始化Mybatis插件
     */
    @PostConstruct
    public void addPageInterceptor() {
        MySQLDialect mySQLDialect = new MySQLDialect();

        for (SqlSessionFactory sqlSessionFactory : this.sqlSessionFactoryList) {
            sqlSessionFactory.getConfiguration().addInterceptor(new UpdateInterceptor(mySQLPluginsProperties, mySQLDialect));
            sqlSessionFactory.getConfiguration().addInterceptor(new LimitInterceptor(mySQLPluginsProperties, mySQLDialect));
            sqlSessionFactory.getConfiguration().addInterceptor(new GrayShadowInterceptor(mySQLPluginsProperties, mySQLDialect));
            sqlSessionFactory.getConfiguration().addInterceptor(new LogSqlInterceptor(mySQLPluginsProperties));
        }

        log.info("MyBatis Plugin Interceptor init success.");
    }

}
