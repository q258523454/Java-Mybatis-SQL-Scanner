package com.zhang.scanner.action;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.Project;
import com.zhang.scanner.ConsoleToolRunner;
import org.jetbrains.annotations.NotNull;

public class ShowRunAction extends AnAction {

    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        run(e.getProject());
    }

    public void run(Project project) {
        if (project == null) {
            return;
        }
        ConsoleToolRunner executor = new ConsoleToolRunner(project, new RunAction(), null);
        executor.run();
    }
}
