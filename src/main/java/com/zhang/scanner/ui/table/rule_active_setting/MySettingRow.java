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
 *
 * 规则开关(active)与规则等级(degreeEnum)是独立字段, 直接参与 XmlSerializer 序列化;
 * 不再依赖 RuleCodeEnum 枚举实例的内存态(枚举状态重启即丢失).
 */
public class MySettingRow implements Serializable {

    private final UUID id;

    /**
     * 规则(由 ruleCode 属性反序列化恢复)
     */
    private RuleCodeEnum ruleCodeEnum;

    /**
     * 规则开关, 持久化字段
     */
    private boolean active;

    /**
     * 规则等级, 持久化字段
     */
    private DegreeEnum degreeEnum;

    /**
     * 无参构造必须要有,否则反射调用无法创建新对象
     */
    public MySettingRow() {
        this(UUID.randomUUID(), null);
    }

    public MySettingRow(UUID id, RuleCodeEnum ruleCodeEnum) {
        this.id = id;
        this.ruleCodeEnum = ruleCodeEnum;
        if (null != ruleCodeEnum) {
            // 初始状态取自枚举默认值
            this.active = ruleCodeEnum.isActive();
            this.degreeEnum = ruleCodeEnum.getDegreeEnum();
        }
    }

    public UUID getId() {
        return id;
    }

    /**
     * 表格展示列: 规则 code (ruleCodeEnum 可能为空, 防御性返回空串)
     */
    public String getColRuleCode() {
        return null == this.ruleCodeEnum ? "" : this.ruleCodeEnum.name();
    }

    /**
     * 表格展示列: 规则描述
     */
    public String getColRuleDescription() {
        return null == this.ruleCodeEnum ? "" : this.ruleCodeEnum.getDesc(LanguageUtil.isChinese());
    }

    /**
     * 规则 code 字符串, 参与序列化; 反序列化时通过 setRuleCode 恢复 ruleCodeEnum
     */
    public String getRuleCode() {
        return null == this.ruleCodeEnum ? null : this.ruleCodeEnum.name();
    }

    public void setRuleCode(String ruleCode) {
        this.ruleCodeEnum = (null == ruleCode || ruleCode.isEmpty()) ? null : RuleCodeEnum.parse(ruleCode);
    }

    public RuleCodeEnum getRuleCodeEnum() {
        return this.ruleCodeEnum;
    }

    public boolean isActive() {
        return this.active;
    }

    /**
     * 编辑 当前规则是否开启 active
     */
    public void setActive(boolean active) {
        this.active = active;
    }

    public DegreeEnum getDegreeEnum() {
        return this.degreeEnum;
    }

    public void setDegreeEnum(DegreeEnum degreeEnum) {
        this.degreeEnum = degreeEnum;
    }

    /**
     * icon 是根据 DegreeEnum 类型来的
     */
    public MyRuleIcon getRuleIcon() {
        if (null == this.degreeEnum) {
            return MyRuleIcon.BLOCKED;
        }
        return MyRuleUtil.convertDegreeRuleToRuleIcon(this.degreeEnum);
    }

    /**
     * 编辑 rule icon 同时修改 DegreeEnum
     */
    public void setRuleIcon(MyRuleIcon ruleIcon) {
        this.degreeEnum = MyRuleUtil.convertRuleIconToDegreeEnum(ruleIcon);
    }

    /**
     * equals: 表单在判断是否有变动的时候, 会调用这个方法.
     * id 相同且 active/degree 一致才算相等, 保证 TableModelEditor.isModified() 能检测到内容变化
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof MySettingRow)) {
            return false;
        }

        MySettingRow that = (MySettingRow) o;
        return Objects.equals(id, that.id)
                && active == that.active
                && degreeEnum == that.degreeEnum;
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, active, degreeEnum);
    }
}
