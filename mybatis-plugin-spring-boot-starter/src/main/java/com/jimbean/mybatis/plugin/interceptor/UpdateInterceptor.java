package com.jimbean.mybatis.plugin.interceptor;

import com.jimbean.mybatis.plugin.dialect.Dialect;
import com.jimbean.mybatis.plugin.properties.MySQLPluginsProperties;
import com.jimbean.mybatis.plugin.util.Content;
import com.jimbean.mybatis.plugin.util.InterceptorUtil;
import com.jimbean.mybatis.plugin.util.MatcherUtil;
import com.jimbean.mybatis.plugin.util.TraceUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.ibatis.executor.Executor;
import org.apache.ibatis.mapping.BoundSql;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.plugin.*;

import java.lang.reflect.Field;
import java.util.Map;
import java.util.Properties;

/**
 * @author zhangjb
 * @ClassName: UpdateInterceptor <br>
 * @Description: Mybatis更新拦截器, insert和delete最终也是调用Executor.update方法 <br>
 */
@Slf4j
@Intercepts({@Signature(type = Executor.class, method = "update", args = {MappedStatement.class, Object.class})})
public class UpdateInterceptor implements Interceptor {

    private MySQLPluginsProperties mySQLPluginsProperties;
    private Dialect dialect;

    private static Field additionalParametersField = null;

    public UpdateInterceptor(MySQLPluginsProperties mySQLPluginsProperties, Dialect dialect) {
        this.mySQLPluginsProperties = mySQLPluginsProperties;
        this.dialect = dialect;
    }

    @Override
    public Object intercept(Invocation invocation) throws Throwable {
        try {
            if (dialect.supportsLimit()) {
                processIntercept(invocation.getArgs());
            }
        } catch (Throwable throwable) {
            // do nothing
        }
        return invocation.proceed();
    }

    private void processIntercept(final Object[] queryArgs) {
        if (mySQLPluginsProperties.getAutoAddTraceid() || mySQLPluginsProperties.getAutoAddStmtid()) {
            MappedStatement ms = (MappedStatement) queryArgs[Content.MAPPED_STATEMENT_INDEX];
            Object parameter = queryArgs[Content.PARAMETER_INDEX];
            BoundSql boundSql = ms.getBoundSql(parameter);
            String sql = boundSql.getSql().trim().toLowerCase();
            if (mySQLPluginsProperties.getAutoAddStmtid()) {
                sql = MatcherUtil.appendStmtid(ms.getId(), sql);
            }
            if (mySQLPluginsProperties.getAutoAddTraceid()) {
                sql = "/* Traceid: " + TraceUtil.getCurrentTraceId() + " */ " + sql;
            }

            BoundSql newBoundSql = new BoundSql(ms.getConfiguration(), sql,
                    boundSql.getParameterMappings(), boundSql.getParameterObject());
            //动态SQL支持
            setDynamicSQLParameters(boundSql, newBoundSql);
            MappedStatement newMs = InterceptorUtil.copyFromMappedStatement(ms, new InterceptorUtil.BoundSqlSqlSource(newBoundSql));
            queryArgs[Content.MAPPED_STATEMENT_INDEX] = newMs;
        }
    }

    private void setDynamicSQLParameters(BoundSql boundSql, BoundSql newBoundSql) {
        try {

            if (additionalParametersField == null) {
                additionalParametersField = boundSql.getClass().getDeclaredField("additionalParameters");
                additionalParametersField.setAccessible(Boolean.TRUE);
            }
            Map<String, Object> additionalParameters = (Map) additionalParametersField.get(boundSql);
            for (Map.Entry<String, Object> entry : additionalParameters.entrySet()) {
                newBoundSql.setAdditionalParameter(entry.getKey(), entry.getValue());
            }
        } catch (NoSuchFieldException | IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public Object plugin(Object target) {
        return Plugin.wrap(target, this);
    }

    @Override
    public void setProperties(Properties properties) {
    }

}
