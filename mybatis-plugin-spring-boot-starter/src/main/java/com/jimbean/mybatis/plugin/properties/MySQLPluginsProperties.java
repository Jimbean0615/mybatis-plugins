package com.jimbean.mybatis.plugin.properties;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 配置注入类
 * 
 * @author zhangjb
 */
@Data
@Slf4j
@ConfigurationProperties(prefix = MySQLPluginProperties.PREFIX, ignoreInvalidFields = true, ignoreUnknownFields = true)
public class MySQLPluginsProperties {

    private Map<String, MySQLPluginProperties> limits = new ConcurrentHashMap<String, MySQLPluginProperties>();
    private Boolean autoAddTraceid = Boolean.FALSE;
    private Boolean autoAddStmtid = Boolean.FALSE;
    private Boolean autoLogSQL= Boolean.FALSE;
    private String autoLogSQLStmtPrefix = "*";
    private Long autoLogSQLRT = 0L;

    private static final String NAMESPACE = "application";
    private static final String PATH = "mybatis.plugin.";

}
