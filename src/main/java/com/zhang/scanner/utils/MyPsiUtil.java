package com.zhang.scanner.utils;

import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.LangDataKeys;
import com.intellij.openapi.actionSystem.PlatformDataKeys;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.fileEditor.OpenFileDescriptor;
import com.intellij.openapi.fileEditor.ex.FileEditorManagerEx;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiDocumentManager;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.SyntheticElement;
import com.intellij.psi.util.PsiTreeUtil;

public enum MyPsiUtil {
    ;

    /**
     * 获取当前焦点下的类文件
     */
    public static PsiFile getPsiFileFromEvent(AnActionEvent e) {
        return e.getData(PlatformDataKeys.PSI_FILE);
    }

    /**
     * 获取当前焦点下的类
     */
    public static PsiClass getPsiClassFromEvent(AnActionEvent e) {
        PsiFile psiFile = e.getData(LangDataKeys.PSI_FILE);
        Editor editor = e.getData(PlatformDataKeys.EDITOR);
        if (psiFile == null || editor == null) {
            return null;
        }
        int offset = editor.getCaretModel().getOffset();
        PsiElement element = psiFile.findElementAt(offset);
        if (element != null) {
            // 当前类
            PsiClass target = PsiTreeUtil.getParentOfType(element, PsiClass.class);
            return target instanceof SyntheticElement ? null : target;
        }
        return null;
    }


    /**
     * 根据 document 创建  PsiFile
     * @param project project
     * @param document document
     */
    public static PsiFile createPsiFileByDocument(Project project, Document document) {
        PsiFile psiFile = null;
        if (null != project) {
            psiFile = PsiDocumentManager.getInstance(project).getPsiFile(document);
        }
        return psiFile;
    }

    /**
     * 获取 psiFile 文件的全路径
     * @param psiFile psiFile文件
     */
    public static String getPsiClassPath(PsiFile psiFile) {
        String path = "";
        if (null != psiFile) {
            // 获取 psiFile 文件的路径
            path = psiFile.getVirtualFile().getPath();
        }
        return path;
    }

    /**
     * 打开跳转到指定psifile
     */
    public static void openPsiFile(Project project, PsiFile psiFile) {
        OpenFileDescriptor openFileDescriptor = new OpenFileDescriptor(project, psiFile.getVirtualFile());
        FileEditorManager fileEditorManager = FileEditorManagerEx.getInstance(project);
        Editor editor = fileEditorManager.openTextEditor(openFileDescriptor, true);
    }
}
