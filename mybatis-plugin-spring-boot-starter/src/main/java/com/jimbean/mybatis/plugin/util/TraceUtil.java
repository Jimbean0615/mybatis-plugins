package com.jimbean.mybatis.plugin.util;

import org.slf4j.MDC;
import org.springframework.util.StringUtils;

import java.util.UUID;

/**
 * @author zhangjb <br/>
 * @date 2020-12-07 20:38 <br/>
 * @email: <a href="mailto:zhangjb@cai-inc.com">zhangjb</a> <br/>
 */
public class TraceUtil {

    private static final String TRACE_ID_KEY = "traceId";

    public static String getCurrentTraceId() {
        String contextTraceId = MDC.get(TRACE_ID_KEY);
        if (StringUtils.isEmpty(contextTraceId)) {
            contextTraceId = createTraceId();
        }

        MDC.put(TRACE_ID_KEY, contextTraceId);
        return contextTraceId;
    }

    private static String createTraceId() {
        String id = UUID.randomUUID().toString().toLowerCase().replace("-", "");
        return id;
    }

}
