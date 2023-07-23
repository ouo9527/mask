package com.ouo.mask.core.annotation;

import lombok.Getter;

/***********************************************************
 * TODO:     脱敏场景
 * Author:   刘春
 * Date:     2023/1/29
 ***********************************************************/
@Getter
public enum SceneEnum {
    // TODO: WEB
    WEB(10),
    // TODO: LOG
    LOG(20),
    // TODO：WEB and LOG
    ALL(0);

    private int code;

    SceneEnum(int code) {
        this.code = code;
    }

    @Override
    public String toString() {
        return this.name().toLowerCase();
    }
}
