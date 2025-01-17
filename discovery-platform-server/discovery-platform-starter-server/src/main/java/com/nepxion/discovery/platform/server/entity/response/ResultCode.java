package com.nepxion.discovery.platform.server.entity.response;

/**
 * <p>Title: Nepxion Discovery</p>
 * <p>Description: Nepxion Discovery</p>
 * <p>Copyright: Copyright (c) 2017-2050</p>
 * <p>Company: Nepxion</p>
 *
 * @author Ning Zhang
 * @version 1.0
 */

import org.apache.commons.lang3.StringUtils;

public enum ResultCode {
    SUCCESS(200L, true, "SUCCESS"),
    ERROR(500L, false, "ERROR"),
    UN_AUTHORIZED(403L, false, "未授权(未携带token访问接口)"),
    CUSTOMIZE_ERROR(417L, false, "后台自定义服务异常(后台的业务报错都是这个，直接展示message)"),
    ILLEGAL_ACCESS(100011L, false, "非法访问(携带错误的token访问系统)"),
    UNABLE_TO_ACCESS(100012L, false, "服务暂时无法访问(微服务无法访问，某个服务宕机)"),
    DATABASE_EXCEPTION(100013L, false, "数据库访问异常(数据库异常)");

    private Long code;
    private Boolean success;
    private String description;

    ResultCode(Long code, Boolean success, String description) {
        this.code = code;
        this.success = success;
        this.description = description;
    }

    public Long getCode() {
        return code;
    }

    public Boolean getSuccess() {
        return success;
    }

    public String getDescription() {
        return description;
    }

    public static ResultCode get(Long code) {
        if (code == null) {
            return null;
        }

        for (ResultCode item : ResultCode.values()) {
            if (item.getCode().equals(code)) {
                return item;
            }
        }
        return null;
    }

    public static ResultCode get(String description) {
        if (StringUtils.isEmpty(description)) {
            return null;
        }

        for (ResultCode item : ResultCode.values()) {
            if (item.getDescription().equals(description)) {
                return item;
            }
        }
        return null;
    }
}