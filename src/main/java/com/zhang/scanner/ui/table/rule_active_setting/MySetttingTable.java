package com.zhang.scanner.ui.table.rule_active_setting;

import com.intellij.util.Function;
import com.intellij.util.ui.ColumnInfo;
import com.intellij.util.ui.table.IconTableCellRenderer;
import com.intellij.util.ui.table.TableModelEditor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.JTable;
import javax.swing.event.TableModelEvent;
import javax.swing.table.TableCellRenderer;
import java.util.Comparator;
import java.util.UUID;


/**
 * 参考
 * {@link com.intellij.ide.browsers.BrowserSettingsPanel}
 */
public class MySetttingTable {

    private TableModelEditor<MySettingRow> tableEditor;

    public TableModelEditor<MySettingRow> getTableEditor() {
        return tableEditor;
    }

    private static final TableModelEditor.EditableColumnInfo<MySettingRow, Boolean> ACTIVE_COLUMN =
            new TableModelEditor.EditableColumnInfo<MySettingRow, Boolean>("Active") {
                @Override
                public Class<?> getColumnClass() {
                    return Boolean.class;
                }

                @Override
                public Boolean valueOf(MySettingRow item) {
                    return item.isActive();
                }

                @Override
                public void setValue(MySettingRow item, Boolean value) {
                    item.setActive(value);
                }

                /**
                 * 固定宽度
                 */
                @Override
                public int getWidth(JTable table) {
                    int width = 90;
                    return width;
                }

                /**
                 * 添加排序比较,支持列排序
                 */
                @NotNull
                @Override
                public Comparator<MySettingRow> getComparator() {
                    return (o1, o2) -> Boolean.compare(o1.isActive(), o2.isActive());
                }
            };

    private static final TableModelEditor.EditableColumnInfo<MySettingRow, MyRuleIcon> RULE_DEGREE_ICON_COLUMN =
            new TableModelEditor.EditableColumnInfo<MySettingRow, MyRuleIcon>("Degree") {
                @Override
                public Class<?> getColumnClass() {
                    return MyRuleIcon.class;
                }

                @Nullable
                @Override
                public MyRuleIcon valueOf(MySettingRow item) {
                    return item.getRuleIcon();
                }

                @Override
                public void setValue(MySettingRow item, MyRuleIcon value) {
                    item.setRuleIcon(value);
                }

                @Override
                public TableCellRenderer getRenderer(MySettingRow item) {
                    return IconTableCellRenderer.ICONABLE;
                }

                /**
                 * 固定宽度
                 */
                @Override
                public int getWidth(JTable table) {
                    int width = 100;
                    return width;
                }

                /**
                 * 添加排序比较,支持列排序
                 */
                @NotNull
                @Override
                public Comparator<MySettingRow> getComparator() {
                    return (o1, o2) -> Integer.compare(o1.getRuleIcon().compareTo(o2.getRuleIcon()), 0);
                }
            };


    private static final TableModelEditor.EditableColumnInfo<MySettingRow, String> RULE_CODE_COLUMN =
            new TableModelEditor.EditableColumnInfo<MySettingRow, String>("Code") {
                @Override
                public Class<?> getColumnClass() {
                    return String.class;
                }

                @Nullable
                @Override
                public String valueOf(MySettingRow item) {
                    return item.getColRuleCode();
                }

                @Override
                public void setValue(MySettingRow item, String value) {
                    // 不允许编辑
                }

                /**
                 * 固定宽度
                 */
                @Override
                public int getWidth(JTable table) {
                    int width = 50;
                    return width;
                }

                /**
                 * 添加排序比较,支持列排序
                 */
                @NotNull
                @Override
                public Comparator<MySettingRow> getComparator() {
                    return (o1, o2) -> Integer.compare(o1.getColRuleCode().compareTo(o2.getColRuleCode()), 0);
                }

                /**
                 * 不允许编辑
                 */
                @Override
                public boolean isCellEditable(MySettingRow mySettingRow) {
                    return false;
                }
            };

    private static final TableModelEditor.EditableColumnInfo<MySettingRow, String> RULE_DESCRIPTION_COLUMN =
            new TableModelEditor.EditableColumnInfo<MySettingRow, String>("Description") {
                @Override
                public String valueOf(MySettingRow item) {
                    return item.getColRuleDescription();
                }

                @Override
                public void setValue(MySettingRow item, String value) {
                    // 不允许编辑
                }

                /**
                 * Rule 描述信息不允许编辑
                 */
                @Override
                public boolean isCellEditable(MySettingRow mySettingRow) {
                    return false;
                }
            };

    private static final ColumnInfo[] COLUMNS = {
            ACTIVE_COLUMN,
            RULE_DEGREE_ICON_COLUMN,
            RULE_CODE_COLUMN,
            RULE_DESCRIPTION_COLUMN
    };


    public MySetttingTable() {
        TableModelEditor.DialogItemEditor<MySettingRow> itemEditor =
                new TableModelEditor.DialogItemEditor<MySettingRow>() {

                    /**
                     * 新增会调用
                     */
                    @NotNull
                    @Override
                    public Class<MySettingRow> getItemClass() {
                        // 反射调用, 必须有无参构造函数
                        return MySettingRow.class;
                    }

                    /**
                     * 克隆一行
                     */
                    @Override
                    public MySettingRow clone(@NotNull MySettingRow item, boolean isClickEditButton) {
                        // TableModelEditor.DataChangedListener 已经限制了不能新增和修改
                        // 因此这里只可能是在表单上直接编辑,每次都新生成 uuid. 只要uuid更改了,就认为有修改.
                        return new MySettingRow(UUID.randomUUID(), item.getRuleCodeEnum());
                    }

                    /**
                     * 可编辑的时候(isEditable=true),点击右边菜单栏窗口触发
                     */
                    @Override
                    public void edit(@NotNull MySettingRow mySettingRow, @NotNull Function<MySettingRow, MySettingRow> function, boolean b) {
                        // TODO
                    }


                    @Override
                    public void applyEdited(@NotNull MySettingRow oldItem, @NotNull MySettingRow newItem) {
                        // TODO
                    }

                    @Override
                    public boolean isEditable(@NotNull MySettingRow item) {
                        return false;
                    }

                    @Override
                    public boolean isRemovable(@NotNull MySettingRow item) {
                        return true;
                    }
                };

        tableEditor = new TableModelEditor<>(COLUMNS, itemEditor, "SQL Scanner Settings")
                .modelListener(new TableModelEditor.DataChangedListener<MySettingRow>() {

                    /**
                     * event.type:
                     * 1:新增
                     * 0:修改
                     * -1:删除,Identifies the header row,Specifies all columns in a row or rows
                     */
                    @Override
                    public void tableChanged(@NotNull TableModelEvent event) {
                        // int type = event.getType();
                        // // 不允许 新增和删除
                        // if (1 == type || -1 == type) {
                        //     Messages.showErrorDialog("Can not insert and delete.", "Tips");
                        //     throw new RuntimeException("Can not insert and delete.");
                        // }
                    }

                    @Override
                    public void dataChanged(@NotNull ColumnInfo<MySettingRow, ?> columnInfo, int rowIndex) {
                        // TODO
                    }
                });
    }
}



