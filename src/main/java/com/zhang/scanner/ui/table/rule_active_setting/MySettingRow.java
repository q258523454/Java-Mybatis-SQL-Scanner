package com.zhang.scanner.ui.table.rule_active_setting;

import com.zhang.scanner.utils.LanguageUtil;
import com.zhang.zmain.enums.DegreeEnum;
import com.zhang.zmain.enums.RuleCodeEnum;
import com.zhang.scanner.utils.MyRuleUtil;

import java.io.Serializable;
import java.util.Objects;
import java.util.UUID;

/**
 * 需要序列化
 * {@link Serializable}
 */
public class MySettingRow implements Serializable {

    private final UUID id;

    /**
     * 规则
     */
    private RuleCodeEnum ruleCodeEnum;

    /**
     * 无参构造必须要有,否则反射调用无法创建新对象
     */
    public MySettingRow() {
        this(UUID.randomUUID(), null);
    }

    public MySettingRow(UUID id, RuleCodeEnum ruleCodeEnum) {
        this.id = id;
        this.ruleCodeEnum = ruleCodeEnum;
    }

    public UUID getId() {
        return id;
    }


    public String getColRuleCode() {
        return this.ruleCodeEnum.name();
    }

    public String getColRuleDescription() {
        return this.ruleCodeEnum.getDesc(LanguageUtil.isChinese());
    }


    public RuleCodeEnum getRuleCodeEnum() {
        return this.ruleCodeEnum;
    }


    public boolean isActive() {
        if (null == ruleCodeEnum) {
            return false;
        }
        return this.ruleCodeEnum.isActive();
    }

    /**
     * 编辑 当前规则是否开启 active
     */
    public void setActive(boolean active) {
        this.ruleCodeEnum.setActive(active);
    }

    /**
     * icon 是根据 DegreeEnum 类型来的
     */
    public MyRuleIcon getRuleIcon() {
        if (null == ruleCodeEnum) {
            return MyRuleIcon.BLOCKED;
        }
        return MyRuleUtil.convertDegreeRuleToRuleIcon(this.ruleCodeEnum.getDegreeEnum());
    }

    /**
     * 编辑 rule icon 同时修改 RuleCodeEnum
     */
    public void setRuleIcon(MyRuleIcon ruleIcon) {
        DegreeEnum degreeEnum = MyRuleUtil.convertRuleIconToDegreeEnum(ruleIcon);
        this.ruleCodeEnum.setDegreeEnum(degreeEnum);
    }


    /**
     * equals, 表单在判断是否有变动的时候, 会调用这个方法.
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof MySettingRow)) {
            return false;
        }

        // 注意不能用 ruleCodeEnum 比较, 因为它是枚举, 是通一个对象, 永远相等.
        MySettingRow that = (MySettingRow) o;
        return this.id == that.id;
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, ruleCodeEnum);
    }
}