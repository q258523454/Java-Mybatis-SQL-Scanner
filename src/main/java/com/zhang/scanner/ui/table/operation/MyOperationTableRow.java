package com.zhang.scanner.ui.table.operation;

import com.intellij.openapi.util.Comparing;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

public class MyOperationTableRow {

    private final UUID id;

    @NotNull
    private String tableName;

    private String insertCount;

    private String deleteCount;

    private String selectCount;

    private String updateCount;

    private String mergeCount;
    private String alterCount;
    private String createCount;
    private String dropCount;

    /**
     * 无参构造必须要有,否则反射调用无法创建新对象
     */
    public MyOperationTableRow() {
        this(UUID.randomUUID(), "",
                "0", "0", "0", "0",
                "0", "0", "0", "0");
    }

    public MyOperationTableRow(UUID id, @NotNull String tableName,
                               String insertCount, String deleteCount, String selectCount, String updateCount) {
        this(id, "",
                insertCount, deleteCount, selectCount, updateCount,
                "0", "0", "0", "0");
    }


    public MyOperationTableRow(UUID id, @NotNull String tableName,
                               String insertCount, String deleteCount, String selectCount, String updateCount,
                               String mergeCount, String alterCount, String createCount, String dropCount) {
        this.id = id;
        this.tableName = tableName;
        this.insertCount = insertCount;
        this.deleteCount = deleteCount;
        this.selectCount = selectCount;
        this.updateCount = updateCount;
        this.mergeCount = mergeCount;
        this.alterCount = alterCount;
        this.createCount = createCount;
        this.dropCount = dropCount;
    }

    public UUID getId() {
        return id;
    }

    @NotNull
    public String getTableName() {
        return tableName;
    }

    public void setTableName(@NotNull String tableName) {
        this.tableName = tableName;
    }

    public String getInsertCount() {
        return insertCount;
    }

    public void setInsertCount(String insertCount) {
        this.insertCount = insertCount;
    }

    public String getDeleteCount() {
        return deleteCount;
    }

    public void setDeleteCount(String deleteCount) {
        this.deleteCount = deleteCount;
    }

    public String getSelectCount() {
        return selectCount;
    }

    public void setSelectCount(String selectCount) {
        this.selectCount = selectCount;
    }

    public String getUpdateCount() {
        return updateCount;
    }

    public void setUpdateCount(String updateCount) {
        this.updateCount = updateCount;
    }

    public String getMergeCount() {
        return mergeCount;
    }

    public void setMergeCount(String mergeCount) {
        this.mergeCount = mergeCount;
    }

    public String getAlterCount() {
        return alterCount;
    }

    public void setAlterCount(String alterCount) {
        this.alterCount = alterCount;
    }

    public String getCreateCount() {
        return createCount;
    }

    public void setCreateCount(String createCount) {
        this.createCount = createCount;
    }

    public String getDropCount() {
        return dropCount;
    }

    public void setDropCount(String dropCount) {
        this.dropCount = dropCount;
    }


    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof MyOperationTableRow)) {
            return false;
        }

        MyOperationTableRow myOperationTableRow = (MyOperationTableRow) o;
        return getId().equals(myOperationTableRow.getId()) &&
                getTableName().equals(myOperationTableRow.tableName) &&
                getInsertCount().equals(myOperationTableRow.getInsertCount()) &&
                getDeleteCount().equals(myOperationTableRow.getDeleteCount()) &&
                getSelectCount().equals(myOperationTableRow.getSelectCount()) &&
                getUpdateCount().equals(myOperationTableRow.getUpdateCount()) &&
                Comparing.strEqual(tableName, myOperationTableRow.tableName);
    }

    @Override
    public int hashCode() {
        return getId().hashCode();
    }
}