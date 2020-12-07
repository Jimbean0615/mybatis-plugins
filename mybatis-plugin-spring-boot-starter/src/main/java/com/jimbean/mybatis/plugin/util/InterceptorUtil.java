package com.jimbean.mybatis.plugin.util;

import org.apache.ibatis.mapping.BoundSql;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.mapping.SqlSource;

/**
 * Created by zhangjb on 2019/4/17 14:23. <br/>
 *
 * @author: zhangjb <br/>
 * @Date: 2019/4/17 <br/>
 * @Email: <a href="mailto:zhangjb@cai-inc.com">zhangjb</a> <br/>
 * @Readme: cn.gov.zcy.boot.mybatis.plugin.util.InterceptorUtil <br/>
 */
public class InterceptorUtil {

    /**
     * build new MappedStatement
     *
     * @param ms
     * @param newSqlSource
     * @return MappedStatement
     */
    public static MappedStatement copyFromMappedStatement(MappedStatement ms, SqlSource newSqlSource) {
        MappedStatement.Builder builder = new MappedStatement.Builder(ms.getConfiguration(), ms.getId(), newSqlSource, ms.getSqlCommandType());

        builder.resource(ms.getResource());
        builder.fetchSize(ms.getFetchSize());
        builder.statementType(ms.getStatementType());

        // generat and map id
        builder.keyGenerator(ms.getKeyGenerator());
        builder.keyProperty(split(ms.getKeyProperties()));
        builder.keyColumn(split(ms.getKeyColumns()));

        // setStatementTimeout()
        builder.timeout(ms.getTimeout());

        // setDatabaseId()
        builder.databaseId(ms.getDatabaseId());

        // setStatementResultMap()
        builder.parameterMap(ms.getParameterMap());

        // setStatementResultMap()
        builder.resultMaps(ms.getResultMaps());
        builder.resultSetType(ms.getResultSetType());
        builder.resultOrdered(ms.isResultOrdered());

        // setStatementCache()
        builder.useCache(ms.isUseCache());
        builder.cache(ms.getCache());
        builder.flushCacheRequired(ms.isFlushCacheRequired());
        builder.fetchSize(ms.getFetchSize());
        builder.lang(ms.getLang());

        return builder.build();
    }

    public static String split(String[] strings){
        if (strings==null){
            return null;
        }
        StringBuilder sb =new StringBuilder();
        for(String str:strings){
            sb.append(str+",");
        }
        return sb.deleteCharAt(sb.length() - 1).toString();
    }

    public static class BoundSqlSqlSource implements SqlSource {
        BoundSql boundSql;

        public BoundSqlSqlSource(BoundSql boundSql) {
            this.boundSql = boundSql;
        }

        @Override
        public BoundSql getBoundSql(Object parameterObject) {
            return boundSql;
        }
    }
}
