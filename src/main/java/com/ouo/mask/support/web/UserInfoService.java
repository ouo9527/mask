package com.ouo.mask.support.web;

import org.springframework.http.server.ServerHttpRequest;

/***********************************************************
 * TODO:     获取当前用户信息，若需要通过用户信息进行控制脱敏，
 *  则需实现该接口，并注入Spring容器管理
 *  {@link org.springframework.web.bind.annotation.ResponseBody}
 * Author:   刘春
 * Date:     2022/12/1
 ***********************************************************/
public interface UserInfoService {

    /**
     * todo: 获取系统当前登录用户
     *
     * @return
     */
    String[] getCurrentUserRoleIds(ServerHttpRequest request);
}
