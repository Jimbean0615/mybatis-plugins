package com.jimbean.mybatis.plugin.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;


/**
 * Created by zhangjb on 2019/4/14. <br/>
 *
 * @author: zhangjb <br/>
 * @Date: 2019/4/14 <br/>
 * @Email: <a href="mailto:zhangjb@cai-inc.com">zhangjb</a> <br/>
 */
@Data
@Component
public class MySQLPluginProperties implements Comparable<MySQLPluginProperties> {

    public static final String PREFIX = "mybatis.plugin";
    public static final Integer DEFAULT_LIMIT_VALUE = 1000;

    private String stmtid;
    private Boolean autoSetup = Boolean.TRUE;
    private Integer limitValue = DEFAULT_LIMIT_VALUE;

    @Override
    public int compareTo(MySQLPluginProperties o) {
        return this.getStmtid().length() >= o.getStmtid().length() ? 1 : 0;
    }
}
