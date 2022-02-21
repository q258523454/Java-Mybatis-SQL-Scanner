package com.zhang.scanner.utils;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import com.intellij.pom.Navigatable;
import com.intellij.psi.PsiFile;
import com.intellij.psi.impl.source.xml.XmlFileImpl;
import com.intellij.psi.search.PsiShortNamesCache;
import com.intellij.psi.xml.XmlElement;
import com.intellij.psi.xml.XmlFile;
import com.intellij.psi.xml.XmlTag;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.MessageFormat;

public enum MapperXmlHelper {
    ;

    /**
     * 返回 指定 mapper 的 PsiFile
     * 如果存在重复命名的话,会弹框提示.
     */
    public static XmlFile findPsiFileByMapperName(Project project, String mapperName) {
        // 查找名称为mapperName的文件
        PsiFile[] files = PsiShortNamesCache.getInstance(project).getFilesByName(mapperName);
        if (files.length == 0) {
            String message = MessageFormat.format("找不到文件[{0}]", mapperName);
            Messages.showMessageDialog(project, message, "提示", Messages.getInformationIcon());
            return null;
        }

        if (files.length > 1) {
            String message = MessageFormat.format("有多个同名文件[{0}]", mapperName);
            Messages.showMessageDialog(project, message, "提示", Messages.getInformationIcon());
            return null;
        }

        // 获取 psiFile 文件的全路径 (当存在多个同名文件时候,可以用来辅助精准定位.)
        // String path = MyPsiUtil.getPsiClassPath(xmlFile);

        PsiFile file = files[0];
        XmlFile xmlFile = (XmlFile) file;
        // 非 xml文件,直接返回
        if (null == xmlFile.getDocument()) {
            return null;
        }
        return xmlFile;
    }

    /**
     * 返回 指定路径下 mapper 的 PsiFile
     */
    public static XmlFile findPsiFileByMapperNameAndFilePath(Project project, String mapperName, String filePath) {
        // 查找名称为mapperName的文件
        PsiFile[] files = PsiShortNamesCache.getInstance(project).getFilesByName(mapperName);

        boolean isExist = true;
        if (files.length > 1) {
            for (PsiFile file : files) {
                XmlFile xmlFile = (XmlFile) file;
                // 获取 psiFile 文件的全路径 (当存在多个同名文件时候,可以用来辅助精准定位.)
                String psiFilePath = MyPsiUtil.getPsiClassPath(xmlFile);

                Path path = Paths.get(psiFilePath);
                Path targetPath = Paths.get(filePath);
                // 是同一个文件
                if (targetPath.equals(path)) {
                    return xmlFile;
                }
            }
            isExist = false;
        }

        if (files.length == 0 || !isExist) {
            String message = MessageFormat.format("找不到文件[{0}]", mapperName);
            Messages.showMessageDialog(project, message, "提示", Messages.getInformationIcon());
            return null;
        }

        PsiFile file = files[0];
        return (XmlFile) file;
    }


    /**
     * 返回 指定 mapper 的指定 sqlNodeId 位置的  XmlElement
     * 规则:
     * 存在多个重复的id,只定位到第一个位置.
     */
    public static XmlElement findXmlElement(XmlFile xmlFile, String sqlNodeId) {
        if (null == xmlFile || null == sqlNodeId) {
            return null;
        }

        XmlFileImpl xmlFileImpl = (XmlFileImpl) xmlFile;
        // mybatis/ibatis 的 sql tag 都是二级节点.
        XmlTag rootTag = xmlFileImpl.getDocument().getRootTag();
        XmlTag[] subTags = rootTag.getSubTags();
        XmlTag targetTag = null;
        for (XmlTag subTag : subTags) {
            String value = subTag.getAttribute("id").getValue();
            if (sqlNodeId.equals(value)) {
                targetTag = subTag;
                break;
            }
        }
        return targetTag;
    }


    /**
     * 跳转到指定 mapper 的指定 sqlNodeId
     * @param project project
     * @param mapperName mapperName
     * @param sqlNodeId sqlNodeId
     */
    public static void navigate(Project project, String mapperName, String sqlNodeId) {
        XmlFile xmlFile = findPsiFileByMapperName(project, mapperName);
        XmlElement xmlElement = findXmlElement(xmlFile, sqlNodeId);
        XmlTag targetTag = (XmlTag) xmlElement;
        // 直接跳转到指定位置
        if (null != targetTag) {
            ((Navigatable) targetTag).navigate(true);
        }
    }

}
