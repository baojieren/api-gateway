package com.gmsj.util;

import java.util.UUID;

/**
 * @author baojieren
 * @date 2020/4/16 17:51
 */
public class TraceIdUtil {

    public static String createTraceId() {
        return UUID.randomUUID().toString().replace("-", "");
    }
}
