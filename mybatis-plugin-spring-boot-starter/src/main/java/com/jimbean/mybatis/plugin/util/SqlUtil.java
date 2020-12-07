package com.jimbean.mybatis.plugin.util;

import com.alibaba.druid.sql.ast.SQLStatement;
import com.alibaba.druid.sql.ast.statement.*;
import com.alibaba.druid.sql.dialect.mysql.ast.statement.MySqlSelectQueryBlock;
import com.alibaba.druid.sql.dialect.mysql.parser.MySqlStatementParser;
import com.alibaba.fastjson.JSON;
import lombok.extern.slf4j.Slf4j;
import net.sf.jsqlparser.parser.CCJSqlParserUtil;
import net.sf.jsqlparser.statement.Statement;
import net.sf.jsqlparser.statement.delete.Delete;
import net.sf.jsqlparser.statement.insert.Insert;
import net.sf.jsqlparser.statement.replace.Replace;
import net.sf.jsqlparser.statement.select.Select;
import net.sf.jsqlparser.statement.update.Update;
import net.sf.jsqlparser.util.TablesNamesFinder;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author zhangjb <br/>
 * @date 2020-11-26 11:30 <br/>
 * @email: <a href="mailto:zhangjb@cai-inc.com">zhangjb</a> <br/>
 */
@Slf4j
public class SqlUtil {

    private static final Pattern pattern = Pattern.compile("[\\s+|\t|\r|\n|,]+");

    /**
     * 通过Druid获取表名
     *
     * @param sql
     * @return
     */
    public static String getTableNameFromSql1(String sql){
        String tableName = null;
        try {
            MySqlStatementParser mySqlStatementParser = new MySqlStatementParser(sql);
            SQLStatement statement = mySqlStatementParser.parseStatement();
            SQLTableSourceImpl sqlTableSource = null;
            if (statement instanceof SQLSelectStatement) {
                SQLSelect selectQuery = ((SQLSelectStatement) statement).getSelect();
                MySqlSelectQueryBlock sqlSelectQuery = (MySqlSelectQueryBlock) selectQuery.getQuery();
                sqlTableSource = (SQLTableSourceImpl) sqlSelectQuery.getFrom();
            } else if (statement instanceof SQLInsertStatement) {
                SQLInsertStatement sqlInsertStatement = (SQLInsertStatement) statement;
                sqlTableSource = sqlInsertStatement.getTableSource();
            } else if (statement instanceof SQLUpdateStatement) {
                SQLUpdateStatement sqlUpdateStatement = (SQLUpdateStatement) statement;
                sqlTableSource = (SQLTableSourceImpl) sqlUpdateStatement.getTableSource();
            } else if (statement instanceof SQLDeleteStatement) {
                SQLDeleteStatement sqlUpdateStatement = (SQLDeleteStatement) statement;
                sqlTableSource = (SQLTableSourceImpl) sqlUpdateStatement.getTableSource();
            }
            tableName = sqlTableSource.toString().trim().toLowerCase();
        } catch (Exception e) {
            log.debug("获取表名失败: {}", e);
        }
        return tableName;
    }

    /**
     * 通过jsqlparser获取表名
     *
     * @param sql
     * @return
     */
    public static List<String> getTableNameFromSql2(String sql){
        List<String> tableList = new ArrayList<>();
        try {
            Statement statement = CCJSqlParserUtil.parse(sql);
            TablesNamesFinder tablesNamesFinder = new TablesNamesFinder();
            if (statement instanceof Select) {
                tableList = tablesNamesFinder.getTableList((Select) statement);
            } else if (statement instanceof Delete) {
                tableList = tablesNamesFinder.getTableList((Delete) statement);
            } else if (statement instanceof Insert) {
                tableList = tablesNamesFinder.getTableList((Insert) statement);
            } else if (statement instanceof Replace) {
                tableList = tablesNamesFinder.getTableList((Replace) statement);
            } else if (statement instanceof Update) {
                tableList = tablesNamesFinder.getTableList((Update) statement);
            } else {
                // match nothing
            }
            log.debug("解析出来的表名: {}", JSON.toJSONString(tableList));
        } catch (Exception e) {
            log.debug("获取表名失败: {}", e);
        }
        return tableList;
    }

    /**
     * 判断是否单一词汇
     *
     * @param str
     * @return
     */
    public static boolean checkSingleWord(String str){
        // 表名中含有空格，回车，换行，制表符, 逗号
        Matcher matcher = pattern.matcher(str);
        return matcher.find();
    }
}
