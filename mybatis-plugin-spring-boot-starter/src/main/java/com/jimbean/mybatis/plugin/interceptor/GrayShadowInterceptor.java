package com.jimbean.mybatis.plugin.interceptor;

import com.jimbean.mybatis.plugin.dialect.Dialect;
import com.jimbean.mybatis.plugin.properties.MySQLPluginsProperties;
import com.jimbean.mybatis.plugin.util.Content;
import com.jimbean.mybatis.plugin.util.SqlUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.ibatis.executor.statement.StatementHandler;
import org.apache.ibatis.mapping.BoundSql;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.plugin.*;
import org.apache.ibatis.reflection.MetaObject;
import org.apache.ibatis.reflection.SystemMetaObject;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.lang.reflect.Proxy;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.*;

/**
 * @author zhangjb
 * @ClassName: GrayShadowInterceptor <br>
 * @Description: 实现根据灰度配置进行影子表切换 <br>
 * <p>
 * 拦截方法 {@link org.apache.ibatis.executor.statement.StatementHandler#prepare(Connection, Integer)}
 */
@Slf4j
@Intercepts({@Signature(type = StatementHandler.class, method = "prepare", args = {Connection.class, Integer.class})})
public class GrayShadowInterceptor implements Interceptor {

    private static final String GRAY_SHADOW_ENABLED = "gray.shadow.enabled";
    private static final String GRAY_SHADOW_TABLES = "gray.shadow.tables";
    private static final String SOURCE_EMPTY_REGEX_SUFFIX = "[\\s+|\t|\r|\n]+";
    private static final String[] SOURCE_REGEX_SUFFIX = {",", "\\("};;
    private static final String TARGET_SUFFIX = " ";

    private MySQLPluginsProperties mySQLPluginsProperties;
    private Dialect dialect;

    public GrayShadowInterceptor(MySQLPluginsProperties mySQLPluginsProperties, Dialect dialect) {
        this.mySQLPluginsProperties = mySQLPluginsProperties;
        this.dialect = dialect;
    }

    @Override
    public Object intercept(Invocation invocation) throws Throwable {

        try {
            if (dialect.supportsLimit()) {
                processIntercept((StatementHandler) invocation.getTarget(), invocation.getArgs());
            }
        } catch (Throwable throwable) {
            log.debug("GrayShadowInterceptor processIntercept error: {}", throwable);
        }
        return invocation.proceed();
    }

    /**
     * 灰度切换影子表
     *
     * @param target
     * @param queryArgs
     * @throws NoSuchFieldException
     * @throws IllegalAccessException
     */
    @SuppressWarnings("all")
    private void processIntercept(StatementHandler target, Object[] args)
            throws NoSuchFieldException, IllegalAccessException, SQLException {

        // 灰度开关
        String grayShadowValue = System.getProperty(GRAY_SHADOW_ENABLED, "false");
        boolean enabled = Boolean.valueOf(grayShadowValue);
        if (!enabled) {
            return;
        }
        // 获取灰度表配置
        String grayShadowTables = System.getProperty(GRAY_SHADOW_TABLES, "");
        if (StringUtils.isEmpty(grayShadowTables)) {
            return;
        }

        String catalog = ((Connection) args[Content.MAPPED_STATEMENT_INDEX]).getCatalog();
        Object parameterObject = args[Content.PARAMETER_INDEX];

        StatementHandler statementHandler = getRealTarget(target);
        MetaObject metaObject = SystemMetaObject.forObject(statementHandler);
        MappedStatement ms = (MappedStatement) metaObject.getValue("delegate.mappedStatement");

//            String sql = (String) metaObject.getValue("delegate.boundSql.sql");
        BoundSql boundSql = statementHandler.getBoundSql();
        String sql = boundSql.getSql().toLowerCase();
        String newSql = sql;
        log.debug("灰度之前的sql: {}", sql);

        // 从sql中获取表名，可能存在多个表
        List<String> tableList = SqlUtil.getTableNameFromSql2(sql);
        // 未匹配到table
        if (CollectionUtils.isEmpty(tableList)) {
            return;
        }

        // 按约定格式解析
        // e.g. gray.shadow.tables=db1.tbl1|tbl1_xxx,db2.tbl2|tbl2_xxx
        String[] shadowTables = grayShadowTables.split(",");
        for (String shadowTable : shadowTables) {
            if (!shadowTable.contains("|")) {
                continue;
            }
            String[] tableRule = shadowTable.split("\\|");
            String sourceDatabaseAndTable = tableRule[0];
            String targetTable = tableRule[1].toLowerCase();

            if (!sourceDatabaseAndTable.contains(".")) {
                continue;
            }
            String[] source = sourceDatabaseAndTable.split("\\.");
            String sourceDatabase = source[0];
            String sourceTable = source[1].toLowerCase();

            // 匹配database
            if (!catalog.equalsIgnoreCase(sourceDatabase)) {
                continue;
            }

            for (String tableName : tableList) {
                if (!sourceTable.equalsIgnoreCase(tableName)) {
                    continue;
                }
                newSql = sql.replaceAll(tableName + SOURCE_EMPTY_REGEX_SUFFIX, targetTable + TARGET_SUFFIX);
                for (String suffix : SOURCE_REGEX_SUFFIX) {
                    newSql = newSql.replaceAll(tableName + suffix, targetTable + suffix);
                }
            }
        }
        if (!sql.equalsIgnoreCase(newSql)) {
            log.info("灰度之后的newSql: {}", newSql);
            metaObject.setValue("delegate.boundSql.sql", newSql);
        } else {
            log.info("未命中灰度规则sql: {}", sql);
        }
    }

    @Override
    public Object plugin(Object target) {
        return Plugin.wrap(target, this);
    }

    @Override
    public void setProperties(Properties properties) {
    }

    /**
     * 获得真正的处理对象,可能多层代理
     *
     * @param target
     * @param <T>
     * @return
     */
    public static <T> T getRealTarget(Object target) {
        if (Proxy.isProxyClass(target.getClass())) {
            MetaObject metaObject = SystemMetaObject.forObject(target);
            return getRealTarget(metaObject.getValue("h.target"));
        }
        return (T) target;
    }

}
