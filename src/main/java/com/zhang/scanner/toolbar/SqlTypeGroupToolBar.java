package com.zhang.scanner.toolbar;


import com.intellij.openapi.actionSystem.ActionGroup;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.zhang.scanner.action.SqlTypeAction;
import com.zhang.scanner.action.SqlTypeActionGroup;
import com.zhang.scanner.utils.MyIconUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public class SqlTypeGroupToolBar extends ActionGroup {

    private SqlTypeActionGroup group = null;

    @NotNull
    @Override
    public AnAction[] getChildren(@Nullable AnActionEvent anActionEvent) {

        if (null == group) {
            // 创建 action group 的 各个子 action
            List<AnAction> subActionList = new ArrayList<>();
            SqlTypeAction mysql = new SqlTypeAction("MySQL", "", MyIconUtil.DataBase.MYSQL);
            SqlTypeAction oracle = new SqlTypeAction("Oracle", "", MyIconUtil.DataBase.ORACLE);
            SqlTypeAction gauss = new SqlTypeAction("Gauss", "", MyIconUtil.DataBase.GAUSS);
            SqlTypeAction db2 = new SqlTypeAction("DB2", "", MyIconUtil.DataBase.DB2);
            SqlTypeAction postgreSQL = new SqlTypeAction("PostgreSQL", "", MyIconUtil.DataBase.POSTGRESQL);

            subActionList.add(mysql);
            subActionList.add(oracle);
            subActionList.add(gauss);
            subActionList.add(db2);
            subActionList.add(postgreSQL);

            // 创建一个group把上面的所有action包在一起
            group = new SqlTypeActionGroup(subActionList);
        }

        List<AnAction> actionList = new ArrayList<>();
        actionList.add(group);
        int size = actionList.size();
        return actionList.toArray(new AnAction[size]);
    }

    @Override
    public void update(@NotNull AnActionEvent e) {
        super.update(e);
    }
}
