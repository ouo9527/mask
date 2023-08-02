package com.ouo.mask.support.web;

import org.springframework.http.server.ServerHttpRequest;

/***********************************************************
 * TODO:     获取当前用户信息进行控制脱敏，则需实现该接口，
 *           并注入Spring容器管理。
 * Author:   刘春
 * Date:     2022/12/1
 ***********************************************************/
public interface UserMaskPermission {

    /**
     * todo: 是否有非脱敏权限
     *
     * @return
     */
    boolean hasNotPermission(ServerHttpRequest request);
}
