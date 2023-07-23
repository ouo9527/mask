package com.ouo.mask.core;

import lombok.Getter;

/***********************************************************
 * TODO:     脱敏模式
 * Author:   刘春
 * Date:     2023/1/29
 ***********************************************************/
@Getter
public enum ModeEnum {
    // TODO: 置空
    //gson枚举映射: @SerializedName(value = "empty", alternate = "EMPTY")
    EMPTY(10),
    // TODO: 哈希
    HASH(20),
    // TODO：正则
    REGEX(30),
    // TODO：替换
    REPLACE(40),
    // TODO：掩盖
    MASK(50);

    private int code;

    ModeEnum(int code) {
        this.code = code;
    }

    @Override
    public String toString() {
        return this.name().toLowerCase();
    }
}
