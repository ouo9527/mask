package com.ouo.mask.rule;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.ArrayUtil;
import cn.hutool.core.util.StrUtil;
import lombok.Getter;
import lombok.Setter;

import java.util.Date;

@Setter
@Getter
public class DesensitizationStrategy {
    //todo: 脱敏范围即配置包路径，多个值时以英文逗号隔开；为了减少不必要的数据脱敏，否则会影响系统性能，因此推荐设置。若为空则所有路径生效
    private String[] packages;
    //todo：生效日
    private String effectDate;
    //todo：到期日
    private String expiryDate;

    public void setPackages(String[] packages) {
        this.packages = ArrayUtil.edit(packages, str -> StrUtil.trimToEmpty(str));
    }

    public Date getEffectDate() {
        return DateUtil.parse(effectDate);
    }

    public Date getExpiryDate() {
        return DateUtil.parse(expiryDate);
    }
}
