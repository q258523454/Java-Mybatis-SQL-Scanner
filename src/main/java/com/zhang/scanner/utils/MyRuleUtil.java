package com.zhang.scanner.utils;

import com.cmb.zmain.enums.DegreeEnum;
import com.zhang.scanner.ui.table.rule_active_setting.MyRuleIcon;

public enum MyRuleUtil {
    ;

    /**
     * 规则等级转换
     * DegreeEnum -> icon
     */
    public static MyRuleIcon convertDegreeRuleToRuleIcon(DegreeEnum degreeEnum) {
        if (degreeEnum.equals(DegreeEnum.BLOCKED)) {
            return com.zhang.scanner.ui.table.rule_active_setting.MyRuleIcon.BLOCKED;
        }
        if (degreeEnum.equals(DegreeEnum.CRITICAL)) {
            return com.zhang.scanner.ui.table.rule_active_setting.MyRuleIcon.CRITICAL;
        }
        if (degreeEnum.equals(DegreeEnum.MAJOR)) {
            return com.zhang.scanner.ui.table.rule_active_setting.MyRuleIcon.MAJOR;
        }
        return null;
    }


    /**
     * 规则等级转换
     * icon -> DegreeEnum
     */
    public static DegreeEnum convertRuleIconToDegreeEnum(MyRuleIcon myRuleIcon) {
        if (myRuleIcon.equals(MyRuleIcon.BLOCKED)) {
            return DegreeEnum.BLOCKED;
        }
        if (myRuleIcon.equals(MyRuleIcon.CRITICAL)) {
            return DegreeEnum.CRITICAL;
        }
        if (myRuleIcon.equals(MyRuleIcon.MAJOR)) {
            return DegreeEnum.MAJOR;
        }
        throw new RuntimeException("DegreeIconColumn Not Matched.");
    }

}
