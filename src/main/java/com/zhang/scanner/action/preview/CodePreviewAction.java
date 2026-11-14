package com.zhang.scanner.action.preview;

import com.intellij.find.FindModel;
import com.intellij.find.impl.FindInProjectUtil;
import com.intellij.ide.actions.GotoActionAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.impl.SimpleDataContext;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.ModalityState;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.ui.Splitter;
import com.intellij.openapi.util.Computable;
import com.intellij.openapi.util.Disposer;
import com.intellij.psi.xml.XmlElement;
import com.intellij.psi.xml.XmlFile;
import com.intellij.psi.xml.XmlTag;
import com.intellij.usageView.UsageInfo;
import com.intellij.usages.UsageViewPresentation;
import com.intellij.usages.impl.UsagePreviewPanel;
import com.zhang.scanner.executor.TreeConsoleExecutor;
import com.zhang.scanner.pojo.ClickActionEventObject;
import com.zhang.scanner.pojo.DataKeyConst;
import com.zhang.scanner.pojo.MapperFileInfo;
import com.zhang.scanner.utils.MapperXmlHelper;

import org.jetbrains.annotations.NotNull;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;

/**
 * Code Previewer Presentation 展示代码
 */
public class CodePreviewAction extends GotoActionAction {

    private static UsagePreviewPanel usagePreviewPanel = null;

    @Override
    public void actionPerformed(@NotNull AnActionEvent anActionEvent) {
        myActionPerformed(anActionEvent);
    }

    public void myActionPerformed(AnActionEvent e) {
        if (!(e.getDataContext() instanceof SimpleDataContext)) {
            return;
        }

        SimpleDataContext dataContext = (SimpleDataContext) e.getDataContext();
        ClickActionEventObject data = dataContext.getData(DataKeyConst.CLICK_ACTION_EVENT_OBJECT_C3);
        TreeConsoleExecutor treeConsoleExecutor = data.getTreeConsoleExecutor();
        // 获取 split 面板
        Splitter splitterPanel = treeConsoleExecutor.getSplitterPanel();
        //  获取project, 等价 e.getProject()
        Project project = data.getProject();
        // 首次创建 Code Previewer 展示面板 才会初始化
        if (null == usagePreviewPanel) {
            usagePreviewPanel = intiUsagePreviewPanel(project);
        }

        Disposer.register(treeConsoleExecutor, usagePreviewPanel);
        // split 面板的右边赋值为 Code Previewer 展示面板
        splitterPanel.setSecondComponent(usagePreviewPanel);

        // 获取 mapper 文件信息
        MapperFileInfo mapperFileInfo = data.getMapperFileInfo();
        String mapperName = mapperFileInfo.getXmlPluginRuleResult().getMapperName();
        String sqlNodeIdOrg = mapperFileInfo.getXmlPluginRuleResult().getSqlNodeIdOrg();
        String filePath = mapperFileInfo.getXmlPluginRuleResult().getFilePath();

        // 如果 mapper 名为空, 直接返回
        if (mapperName.isEmpty()) {
            usagePreviewPanel.updateLayout(project, null);
            return;
        }

        // 索引查询(FilenameIndex)属于慢操作, 禁止在 EDT 上执行;
        // PSI 访问与 UsageInfo 创建必须持有读锁.
        // 因此: 后台线程中通过 runReadAction 完成 索引查询 + PSI 查找 + UsageInfo 创建,
        // 结果回到 EDT 后再刷新预览面板.
        ApplicationManager.getApplication().executeOnPooledThread(() -> {
            List<UsageInfo> usageInfoList = ApplicationManager.getApplication().runReadAction((Computable<List<UsageInfo>>) () -> {
                List<UsageInfo> list = new ArrayList<>();
                // 返回 指定 mapper 的 PsiFile (调用方已持有读锁)
                XmlFile xmlFile = MapperXmlHelper.findPsiFileByMapperNameAndFilePath(project, mapperName, filePath);
                if (null == xmlFile) {
                    // 找不到 mapper 文件, 返回空列表, 由 EDT 回调弹框提示
                    return list;
                }

                XmlElement xmlElement = MapperXmlHelper.findXmlElement(xmlFile, sqlNodeIdOrg);
                if (null == xmlElement) {
                    // 找不到对应的 sql id ,则定位到mapper文件首行
                    list.add(new UsageInfo(xmlFile, 0, 0));
                } else {
                    XmlTag xmlTag = (XmlTag) xmlElement;
                    int startOffset = xmlTag.getTextOffset();
                    int endOffset = startOffset + xmlTag.getName().length();
                    // startOffset:需要跳转的位置 (startOffset,endOffset) 表示一个高亮显示区间
                    list.add(new UsageInfo(xmlFile, startOffset, endOffset));
                }
                return list;
            });

            // UI 更新必须回到 EDT 执行
            ApplicationManager.getApplication().invokeLater(() -> {
                if (usageInfoList.isEmpty()) {
                    // 找不到 mapper 文件, 弹框提示
                    Messages.showMessageDialog(project,
                            MessageFormat.format("找不到文件[{0}]", mapperName),
                            "提示", Messages.getInformationIcon());
                    usagePreviewPanel.updateLayout(project, null);
                } else {
                    usagePreviewPanel.updateLayout(project, usageInfoList);
                }
            }, ModalityState.defaultModalityState());
        });
    }

    /**
     * 初始化 Code Previewer 展示面板
     */
    public static UsagePreviewPanel intiUsagePreviewPanel(Project project) {
        // 初始化 Code Previewer Presentation
        UsageViewPresentation viewPresentation1 = FindInProjectUtil.setupViewPresentation(false, new FindModel());
        // 创建 Code Previewer 展示面板
        UsagePreviewPanel myUsagePreviewPanel = new UsagePreviewPanel(project, viewPresentation1);
        myUsagePreviewPanel.setVisible(true);
        return myUsagePreviewPanel;
    }


}
