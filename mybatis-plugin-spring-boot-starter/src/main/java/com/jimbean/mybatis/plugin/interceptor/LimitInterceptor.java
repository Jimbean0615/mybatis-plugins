package com.jimbean.mybatis.plugin.interceptor;

import com.jimbean.mybatis.plugin.dialect.Dialect;
import com.jimbean.mybatis.plugin.properties.MySQLPluginProperties;
import com.jimbean.mybatis.plugin.properties.MySQLPluginsProperties;
import com.jimbean.mybatis.plugin.util.Content;
import com.jimbean.mybatis.plugin.util.MatcherUtil;
import com.jimbean.mybatis.plugin.util.TraceUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.ibatis.executor.statement.StatementHandler;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.mapping.SqlCommandType;
import org.apache.ibatis.plugin.*;
import org.apache.ibatis.reflection.MetaObject;
import org.apache.ibatis.reflection.SystemMetaObject;
import org.apache.ibatis.session.RowBounds;

import java.sql.Connection;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 
 * @ClassName: LimitInterceptor <br>
 * @Description: Mybatis拦截器限制查询最大条数,这里必须使用StatementHandler进行拦截,因为Executor可能会与其他插件冲突 <br>
 * 
 * @author zhangjb
 */
@Slf4j
@Intercepts({@Signature(type = StatementHandler.class, method = "prepare", args = {Connection.class, Integer.class})})
public class LimitInterceptor implements Interceptor {

    private MySQLPluginsProperties mySQLPluginsProperties;
    private Dialect dialect;

    public LimitInterceptor(MySQLPluginsProperties mySQLPluginsProperties, Dialect dialect) {
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
            // do nothing
        }
        return invocation.proceed();
    }

    private void processIntercept(StatementHandler target, Object[] queryArgs)
            throws NoSuchFieldException, IllegalAccessException {
        // 通过反射获取到当前RoutingStatementHandler对象的delegate属性
        // StatementHandler delegate = (StatementHandler) ReflectUtil.getFieldValue(target, "delegate");
        MetaObject metaObject = SystemMetaObject.forObject(target);
        MappedStatement ms = (MappedStatement) metaObject.getValue("delegate.mappedStatement");

        // 仅对SELECT起作用
        if (SqlCommandType.SELECT != ms.getSqlCommandType()) {
            return;
        }
        // MappedStatement ms = (MappedStatement) ReflectUtil.getFieldValue(delegate, "mappedStatement");
        // BoundSql boundSql = target.getBoundSql();
        String sql = (String) metaObject.getValue("delegate.boundSql.sql");
        int rowBoundsLimit = (Integer) metaObject.getValue("delegate.rowBounds.limit");

        if (mySQLPluginsProperties.getAutoAddStmtid()) {
            sql = MatcherUtil.appendStmtid(ms.getId(), sql);
        }
        // 增加traceid
        if (mySQLPluginsProperties.getAutoAddTraceid()) {
            sql = getTraceidPrefixSql(sql);
        }

        // 追加limit逻辑
        String currentStmtid = ms.getId().toLowerCase();
        log.debug("currentStmtid {}", currentStmtid);
        // 跳过分页,count等方法,按照方法名简单区分
        String method = currentStmtid.substring(currentStmtid.lastIndexOf("."));
        if (!skip(method)) {
            List<MySQLPluginProperties> olpCollection = new ArrayList<>(mySQLPluginsProperties.getLimits().values());
            if (olpCollection.size() != 0) {
                // 匹配规则
                List<MySQLPluginProperties> offsetLimitPropertiesList = olpCollection.stream()
                        .filter(olp -> currentStmtid.startsWith((olp.getStmtid() == null ? "" : olp.getStmtid().trim()).toLowerCase().replace("*", "")))
                        .collect(Collectors.toList());
                // 匹配成功
                if (offsetLimitPropertiesList.size() != 0) {
                    // 如果有多条匹配成功的规则,取最精确的那条规则,这里通过stmtid配置的长度判断即可
                    Collections.sort(offsetLimitPropertiesList);
                    MySQLPluginProperties offsetLimitProperties = offsetLimitPropertiesList.get(0);
                    if (offsetLimitProperties.getAutoSetup()) {
                        // 理论上offset会与limit成对出现,这里不对offset进行判断
                        if (rowBoundsLimit == RowBounds.NO_ROW_LIMIT && !sql.toLowerCase().contains(Content.LIMIT_KEYWORD)) {
                            // 默认limit规则
                            int limit = offsetLimitProperties.getLimitValue();
                            if (dialect.supportsLimitOffset()) {
                                sql = dialect.getLimitString(sql, 0, limit);
                            }
                            // log.info("generated limit sql is : " + sql);
                        }
                    }
                }
            }
        }
        // 利用反射设置当前BoundSql对应的sql属性为我们建立好的分页Sql语句
        // ReflectUtil.setFieldValue(boundSql, "sql", sql);
        metaObject.setValue("delegate.boundSql.sql", sql);
    }

    private boolean skip(String method){
        return method.contains("page") || method.contains("paging") || method.contains("pageable")
                || method.contains("pagination") || method.contains("count");
    }

    private String getTraceidPrefixSql(String sql){
        String prefix = "/* Traceid: ";
        if (!sql.contains(prefix)) {
            return prefix + TraceUtil.getCurrentTraceId() + " */ " + sql;
        } else {
            return sql;
        }
    }

    @Override
    public Object plugin(Object target) {
        return Plugin.wrap(target, this);
    }

    @Override
    public void setProperties(Properties properties) {}

}
