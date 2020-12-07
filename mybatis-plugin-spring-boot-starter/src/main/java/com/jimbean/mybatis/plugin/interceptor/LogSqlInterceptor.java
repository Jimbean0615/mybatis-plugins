package com.jimbean.mybatis.plugin.interceptor;

import com.jimbean.mybatis.plugin.properties.MySQLPluginsProperties;
import com.jimbean.mybatis.plugin.util.MatcherUtil;
import org.apache.ibatis.executor.statement.StatementHandler;
import org.apache.ibatis.mapping.BoundSql;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.mapping.ParameterMapping;
import org.apache.ibatis.mapping.ParameterMode;
import org.apache.ibatis.plugin.*;
import org.apache.ibatis.reflection.MetaObject;
import org.apache.ibatis.reflection.SystemMetaObject;
import org.apache.ibatis.scripting.defaults.DefaultParameterHandler;
import org.apache.ibatis.session.Configuration;
import org.apache.ibatis.session.ResultHandler;
import org.apache.ibatis.type.TypeHandlerRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.Ordered;
import org.springframework.util.ReflectionUtils;
import org.springframework.util.StringUtils;

import java.lang.reflect.Field;
import java.sql.Statement;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * MyBatis插件
 *
 * @author 虚竹
 */
@Intercepts({@Signature(type = StatementHandler.class, method = "query", args = {Statement.class, ResultHandler.class}),
        @Signature(type = StatementHandler.class, method = "update", args = {Statement.class}),
        @Signature(type = StatementHandler.class, method = "batch", args = {Statement.class})})
public class LogSqlInterceptor implements Interceptor, Ordered {
    private static final Logger LOGGER = LoggerFactory.getLogger(LogSqlInterceptor.class);

    private Configuration configuration = null;

    private static final ThreadLocal<SimpleDateFormat> DATE_FORMAT_THREAD_LOCAL = ThreadLocal.withInitial(() -> new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS"));

    private MySQLPluginsProperties offsetLimitsProperties;

    public LogSqlInterceptor(MySQLPluginsProperties offsetLimitsProperties) {
        this.offsetLimitsProperties = offsetLimitsProperties;
    }

    /**
     * 数据库操作拦截
     *
     * @param invocation
     * @return
     * @throws
     */
    @Override
    public Object intercept(Invocation invocation) throws Throwable {

        Object target = invocation.getTarget();
        long startTime = System.currentTimeMillis();
        try {
            return invocation.proceed();
        } finally {
            if (offsetLimitsProperties.getAutoLogSQL()) {
                long endTime = System.currentTimeMillis();
                long sqlCost = endTime - startTime;
                // 配置了rt，判断rt是否满足需求
                if (sqlCost >= offsetLimitsProperties.getAutoLogSQLRT().longValue()) {
                    try {
                        StatementHandler statementHandler = (StatementHandler) target;
                        BoundSql boundSql = statementHandler.getBoundSql();

                        MappedStatement ms = null;
                        // statementHandler是代理对象时
                        if (statementHandler instanceof java.lang.reflect.Proxy) {
                            Field h = statementHandler.getClass().getSuperclass().getDeclaredField("h");
                            h.setAccessible(true);
                            Plugin plugin = (Plugin) h.get(statementHandler);
                            ms = (MappedStatement) SystemMetaObject.forObject(plugin).getValue("target.delegate.mappedStatement");
                        } else {
                            // 非代理对象
                            ms = (MappedStatement) SystemMetaObject.forObject(statementHandler).getValue("delegate.mappedStatement");
                        }

                        String currentStmtid = ms.getId().toLowerCase();
                        String stmtPrefix = offsetLimitsProperties.getAutoLogSQLStmtPrefix();
                        if (!StringUtils.isEmpty(stmtPrefix)) {
                            if ("*".equalsIgnoreCase(stmtPrefix) || matchStmtId(currentStmtid, stmtPrefix.toLowerCase())) {
                                if (configuration == null) {
                                    final DefaultParameterHandler parameterHandler = (DefaultParameterHandler) statementHandler.getParameterHandler();
                                    Field configurationField = ReflectionUtils.findField(parameterHandler.getClass(), "configuration");
                                    ReflectionUtils.makeAccessible(configurationField);
                                    this.configuration = (Configuration) configurationField.get(parameterHandler);
                                }
                                //替换参数格式化Sql语句，去除换行符
                                String sql = formatSql(boundSql, configuration);
                                if (offsetLimitsProperties.getAutoAddStmtid()) {
                                    sql = MatcherUtil.appendStmtid(ms.getId(), sql);
                                }
                                LOGGER.info("spring-boot-starter-mybatis-plugin, SQL：[" + sql + "], 执行耗时[" + sqlCost + "ms]");
                            }
                        }
                    } catch (Exception e) {
                    }
                }
            }
        }
    }

    /**
     * 匹配stmtid
     *
     * @param stmtId
     * @param stmtPrefix
     * @return
     */
    private boolean matchStmtId(String stmtId, String stmtPrefix) {
        String[] prefixs = stmtPrefix.split(",");
        //倒序，精度高的配置优先匹配
        Collections.sort(Arrays.asList(prefixs), Collections.reverseOrder());
        for (String prefix : prefixs) {
            if (prefix.endsWith(".*")) {
                return stmtId.startsWith(prefix.substring(0, prefix.length() - 2));
            } else {
                return stmtId.equalsIgnoreCase(prefix);
            }
        }
        return false;
    }

    @Override
    public Object plugin(Object target) {
        return Plugin.wrap(target, this);
    }

    @Override
    public void setProperties(Properties properties) {
    }

    /**
     * 获取完整的sql实体的信息
     *
     * @param boundSql
     * @return
     */
    private String formatSql(BoundSql boundSql, Configuration configuration) {
        String sql = boundSql.getSql();
        List<ParameterMapping> parameterMappings = boundSql.getParameterMappings();
        Object parameterObject = boundSql.getParameterObject();
        // 输入sql字符串空判断
        if (sql == null || sql.length() == 0) {
            return "";
        }

        if (configuration == null) {
            return "";
        }

        TypeHandlerRegistry typeHandlerRegistry = configuration.getTypeHandlerRegistry();

        // 美化sql
        sql = beautifySql(sql);
        /**
         * @see org.apache.ibatis.scripting.defaults.DefaultParameterHandler 参考Mybatis 参数处理
         */
        if (parameterMappings != null) {
            for (ParameterMapping parameterMapping : parameterMappings) {
                if (parameterMapping.getMode() != ParameterMode.OUT) {
                    Object value;
                    String propertyName = parameterMapping.getProperty();
                    if (boundSql.hasAdditionalParameter(propertyName)) {
                        value = boundSql.getAdditionalParameter(propertyName);
                    } else if (parameterObject == null) {
                        value = null;
                    } else if (typeHandlerRegistry.hasTypeHandler(parameterObject.getClass())) {
                        value = parameterObject;
                    } else {
                        MetaObject metaObject = configuration.newMetaObject(parameterObject);
                        value = metaObject.getValue(propertyName);
                    }
                    String paramValueStr = "";
                    if (value instanceof String) {
                        paramValueStr = "'" + value + "'";
                    } else if (value instanceof Date) {
                        paramValueStr = "'" + DATE_FORMAT_THREAD_LOCAL.get().format(value) + "'";
                    } else {
                        paramValueStr = value + "";
                    }
                    // mybatis generator 中的参数不打印出来
//                    if (!propertyName.contains("frch_criterion")) {
//                        paramValueStr = "/*" + propertyName + "*/" + paramValueStr;
//                    }
                    sql = sql.replaceFirst("\\?", paramValueStr);
                }
            }
        }
        return sql;
    }

    /**
     * 美化Sql
     */
    private String beautifySql(String sql) {
        sql = sql.replaceAll("[\\s\n ]+", " ");
        return sql;
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE;
    }
}
