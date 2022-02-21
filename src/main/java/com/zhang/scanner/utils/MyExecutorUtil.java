package com.zhang.scanner.utils;

import com.intellij.execution.Executor;
import com.intellij.execution.ExecutorRegistry;

public enum MyExecutorUtil {
    ;

    /**
     * 返回正在运行的 Executor
     * @param id Executor id
     */
    public static Executor getRunExecutorInstance(String id) {
        return ExecutorRegistry.getInstance().getExecutorById(id);
    }
}
