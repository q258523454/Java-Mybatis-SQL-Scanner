package com.zhang.scanner.ui.table.operation;

import com.zhang.zmain.pojo.BaseResult;
import com.zhang.zmain.util.ResultUtil;
import com.intellij.util.Function;
import com.intellij.util.ui.ColumnInfo;
import com.intellij.util.ui.ListTableModel;
import com.intellij.util.ui.table.TableModelEditor;
import org.jetbrains.annotations.NotNull;

import javax.swing.JComponent;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;


/**
 * 参考
 * {@link com.intellij.ide.browsers.BrowserSettingsPanel}
 */
public class MyOperationTable {

    private TableModelEditor<MyOperationTableRow> tableEditor;

    private JComponent table;

    public JComponent getTable() {
        return table;
    }


    private static final TableModelEditor.EditableColumnInfo<MyOperationTableRow, String> tableNameColumn =
            new TableModelEditor.EditableColumnInfo<MyOperationTableRow, String>("Table") {
                @Override
                public String valueOf(MyOperationTableRow item) {
                    return item.getTableName();
                }

                @Override
                public void setValue(MyOperationTableRow item, String value) {
                    item.setTableName(value);
                }

                /**
                 * 添加排序比较,支持列排序
                 */
                @NotNull
                @Override
                public Comparator<MyOperationTableRow> getComparator() {
                    return (o1, o2) -> Integer.compare(o1.getTableName().compareTo(o2.getTableName()), 0);
                }
            };

    private static final TableModelEditor.EditableColumnInfo<MyOperationTableRow, String> insertCountColumn =
            new TableModelEditor.EditableColumnInfo<MyOperationTableRow, String>("Insert") {
                @Override
                public String valueOf(MyOperationTableRow item) {
                    return item.getInsertCount();
                }

                @Override
                public void setValue(MyOperationTableRow item, String value) {
                    item.setInsertCount(value);
                }

                /**
                 * 添加排序比较,支持列排序
                 */
                @NotNull
                @Override
                public Comparator<MyOperationTableRow> getComparator() {
                    return (o1, o2) -> Integer.compare(o1.getInsertCount().compareTo(o2.getInsertCount()), 0);
                }
            };
    private static final TableModelEditor.EditableColumnInfo<MyOperationTableRow, String> deleteCountColumn =
            new TableModelEditor.EditableColumnInfo<MyOperationTableRow, String>("Delete") {
                @Override
                public String valueOf(MyOperationTableRow item) {
                    return item.getDeleteCount();
                }

                @Override
                public void setValue(MyOperationTableRow item, String value) {
                    item.setDeleteCount(value);
                }

                /**
                 * 添加排序比较,支持列排序
                 */
                @NotNull
                @Override
                public Comparator<MyOperationTableRow> getComparator() {
                    return (o1, o2) -> Integer.compare(o1.getDeleteCount().compareTo(o2.getDeleteCount()), 0);
                }
            };
    private static final TableModelEditor.EditableColumnInfo<MyOperationTableRow, String> selectCountColumn =
            new TableModelEditor.EditableColumnInfo<MyOperationTableRow, String>("Select") {
                @Override
                public String valueOf(MyOperationTableRow item) {
                    return item.getSelectCount();
                }

                @Override
                public void setValue(MyOperationTableRow item, String value) {
                    item.setSelectCount(value);
                }

                /**
                 * 添加排序比较,支持列排序
                 */
                @NotNull
                @Override
                public Comparator<MyOperationTableRow> getComparator() {
                    return (o1, o2) -> Integer.compare(o1.getSelectCount().compareTo(o2.getSelectCount()), 0);
                }
            };
    private static final TableModelEditor.EditableColumnInfo<MyOperationTableRow, String> updateCountColumn =
            new TableModelEditor.EditableColumnInfo<MyOperationTableRow, String>("Update") {
                @Override
                public String valueOf(MyOperationTableRow item) {
                    return item.getUpdateCount();
                }

                @Override
                public void setValue(MyOperationTableRow item, String value) {
                    item.setUpdateCount(value);
                }

                /**
                 * 添加排序比较,支持列排序
                 */
                @NotNull
                @Override
                public Comparator<MyOperationTableRow> getComparator() {
                    return (o1, o2) -> Integer.compare(o1.getUpdateCount().compareTo(o2.getUpdateCount()), 0);
                }
            };

    private static final TableModelEditor.EditableColumnInfo<MyOperationTableRow, String> mergeCountColumn =
            new TableModelEditor.EditableColumnInfo<MyOperationTableRow, String>("Merge") {
                @Override
                public String valueOf(MyOperationTableRow item) {
                    return item.getMergeCount();
                }

                @Override
                public void setValue(MyOperationTableRow item, String value) {
                    item.setUpdateCount(value);
                }

                /**
                 * 添加排序比较,支持列排序
                 */
                @NotNull
                @Override
                public Comparator<MyOperationTableRow> getComparator() {
                    return (o1, o2) -> Integer.compare(o1.getMergeCount().compareTo(o2.getMergeCount()), 0);
                }
            };
    private static final TableModelEditor.EditableColumnInfo<MyOperationTableRow, String> alterCountColumn =
            new TableModelEditor.EditableColumnInfo<MyOperationTableRow, String>("Alter") {
                @Override
                public String valueOf(MyOperationTableRow item) {
                    return item.getAlterCount();
                }

                @Override
                public void setValue(MyOperationTableRow item, String value) {
                    item.setUpdateCount(value);
                }

                /**
                 * 添加排序比较,支持列排序
                 */
                @NotNull
                @Override
                public Comparator<MyOperationTableRow> getComparator() {
                    return (o1, o2) -> Integer.compare(o1.getAlterCount().compareTo(o2.getAlterCount()), 0);
                }
            };
    private static final TableModelEditor.EditableColumnInfo<MyOperationTableRow, String> createCountColumn =
            new TableModelEditor.EditableColumnInfo<MyOperationTableRow, String>("Create") {
                @Override
                public String valueOf(MyOperationTableRow item) {
                    return item.getCreateCount();
                }

                @Override
                public void setValue(MyOperationTableRow item, String value) {
                    item.setUpdateCount(value);
                }

                /**
                 * 添加排序比较,支持列排序
                 */
                @NotNull
                @Override
                public Comparator<MyOperationTableRow> getComparator() {
                    return (o1, o2) -> Integer.compare(o1.getCreateCount().compareTo(o2.getCreateCount()), 0);
                }
            };

    private static final TableModelEditor.EditableColumnInfo<MyOperationTableRow, String> dropCountColumn =
            new TableModelEditor.EditableColumnInfo<MyOperationTableRow, String>("Drop") {
                @Override
                public String valueOf(MyOperationTableRow item) {
                    return item.getDropCount();
                }

                @Override
                public void setValue(MyOperationTableRow item, String value) {
                    item.setUpdateCount(value);
                }

                /**
                 * 添加排序比较,支持列排序
                 */
                @NotNull
                @Override
                public Comparator<MyOperationTableRow> getComparator() {
                    return (o1, o2) -> Integer.compare(o1.getDropCount().compareTo(o2.getDropCount()), 0);
                }
            };
    private static final ColumnInfo[] COLUMNS = {
            tableNameColumn,
            insertCountColumn,
            deleteCountColumn,
            selectCountColumn,
            updateCountColumn,
            mergeCountColumn,
            alterCountColumn,
            createCountColumn,
            dropCountColumn
    };


    public MyOperationTable() {
        TableModelEditor.DialogItemEditor<MyOperationTableRow> itemEditor = new TableModelEditor.DialogItemEditor<MyOperationTableRow>() {
            @NotNull
            @Override
            public Class<MyOperationTableRow> getItemClass() {
                return MyOperationTableRow.class;
            }

            @Override
            public MyOperationTableRow clone(@NotNull MyOperationTableRow item, boolean forInPlaceEditing) {
                UUID uuid = forInPlaceEditing ? item.getId() : UUID.randomUUID();
                return new MyOperationTableRow(uuid, item.getTableName(), item.getInsertCount(), item.getDeleteCount(), item.getSelectCount(), item.getUpdateCount());
            }

            @Override
            public void edit(@NotNull MyOperationTableRow browser, @NotNull Function<MyOperationTableRow, MyOperationTableRow> mutator, boolean isAdd) {
                // TODO
            }


            @Override
            public void applyEdited(@NotNull MyOperationTableRow oldItem, @NotNull MyOperationTableRow newItem) {
                // TODO
            }

            @Override
            public boolean isEditable(@NotNull MyOperationTableRow browser) {
                return true;
            }

        };

        tableEditor = new TableModelEditor<>(COLUMNS, itemEditor, "Table Operation Statistics");
        table = tableEditor.createComponent();
    }

    public void init(List<BaseResult> result) {
        if (null == result || result.isEmpty()) {
            return;
        }
        // tableResult<表名,List<增-删-查-改 操作次数>>
        Map<String, Map<String, Integer>> tableOptCountMap = ResultUtil.getTableOptCount(result);
        if (!tableOptCountMap.isEmpty()) {
            ListTableModel<MyOperationTableRow> model = this.tableEditor.getModel();
            for (Map.Entry<String, Map<String, Integer>> entry : tableOptCountMap.entrySet()) {
                String tableName = entry.getKey();
                Map<String, Integer> optNameAndCountMap = entry.getValue();
                String insertCount = optNameAndCountMap.get("insert") + "";
                String deleteCount = optNameAndCountMap.get("delete") + "";
                String selectCount = optNameAndCountMap.get("select") + "";
                String updateCount = optNameAndCountMap.get("update") + "";
                String mergeCount = optNameAndCountMap.get("merge") + "";
                String alterCount = optNameAndCountMap.get("alter") + "";
                String createCount = optNameAndCountMap.get("create") + "";
                String dropCount = optNameAndCountMap.get("drop") + "";

                MyOperationTableRow myOperationTableRow = new MyOperationTableRow(UUID.randomUUID(), tableName, insertCount, deleteCount, selectCount, updateCount,
                        mergeCount, alterCount, createCount, dropCount);
                model.addRow(myOperationTableRow);
            }
        }
    }
}



