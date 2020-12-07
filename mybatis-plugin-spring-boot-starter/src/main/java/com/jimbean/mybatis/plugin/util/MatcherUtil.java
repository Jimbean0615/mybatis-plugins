package com.jimbean.mybatis.plugin.util;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by zhangjianbin on 2019-12-27 16:09. <br/>
 *
 * @author: zhangjianbin <br/>
 * @Date: 2019-12-27 <br/>
 * @Email: <a href="mailto:zhangjb@cai-inc.com">zhangjb</a> <br/>
 * @Readme: cn.gov.zcy.boot.mybatis.plugin.util.MatcherUtil <br/>
 */
public class MatcherUtil {

    /**
     * 追加stmt id
     *
     * @param stmtL
     * @param sql
     * @return
     */
    public static String appendStmtid(String stmtL, String sql) {
        if (sql.toLowerCase().contains(stmtL.toLowerCase())) {
            return sql;
        }

        String[] stmts = stmtL.split("\\.");
        if (stmts == null || stmts.length == 0) {
            return sql;
        }

        String stmtS = stmts[stmts.length - 1];
        // 匹配规则
        String reg = "\\/\\*(.*?)\\*\\/";
        Pattern pattern = Pattern.compile(reg);

        Matcher matcher = pattern.matcher(sql);

        while (matcher.find()) {
            // 不包含前后的两个字符
            String s = matcher.group(1);
            if (stmtS.equalsIgnoreCase(s.trim())) {
                return sql;
            }
        }

        // 未match到stmtid信息
        return "/* " + stmtL + " */ " + sql;
    }

    public static void main(String[] args) {
        String s = "/*    updateByPrimaryKey*/ update tb_content  set category_id = ?,title = ?,sub_title = ?,title_desc = ?,url = ?,pic = ?,pic2 = ?,content = ?,created = ?,updated = ? where  id = ?";
        String ss = appendStmtid("cn.gov.zcy.security.oauth2.resource.mapper.TbContentMapper.updateByPrimaryKey", s);
        System.out.println(ss);
    }

}
