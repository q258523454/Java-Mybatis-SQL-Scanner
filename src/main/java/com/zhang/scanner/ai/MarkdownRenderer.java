package com.zhang.scanner.ai;

import java.util.ArrayList;
import java.util.List;

/**
 * 轻量 Markdown 转 HTML(自写实现, 不引第三方库)
 * 输出针对 JEditorPane(HTMLEditorKit, HTML 3.2 子集) 优化:
 * 标题(h2/h3/h4 有字号与粗细层级)、无序列表(ul/li 圆点)、
 * 有序列表(HTMLEditorKit 不支持 ol 自动递增编号, 故保留原文编号渲染为块级行)、
 * 代码块(等宽字体)、表格(带边框)、粗体、斜体、行内代码、分隔线
 * 未闭合代码块按进行中状态渲染, 便于流式输出
 */
public final class MarkdownRenderer {

    private MarkdownRenderer() {
    }

    /**
     * Markdown 转 HTML body 片段(供 JEditorPane 使用)
     */
    public static String toHtml(String markdown) {
        StringBuilder html = new StringBuilder();
        String[] lines = markdown.split("\n", -1);
        boolean inCodeBlock = false;
        boolean inUnorderedList = false;
        List<String> tableLines = new ArrayList<>();

        for (String line : lines) {
            String trimmed = line.trim();

            // 代码块边界
            if (trimmed.startsWith("```")) {
                flushTable(html, tableLines);
                closeUnorderedList(html, inUnorderedList);
                inUnorderedList = false;
                if (inCodeBlock) {
                    html.append("</font></pre>");
                    inCodeBlock = false;
                } else {
                    html.append("<pre><font face=\"Monospaced\">");
                    inCodeBlock = true;
                }
                continue;
            }
            if (inCodeBlock) {
                html.append(escape(line)).append('\n');
                continue;
            }

            // 表格行(连续收集, 遇到非表格行时整体输出)
            if (isTableLine(trimmed)) {
                closeUnorderedList(html, inUnorderedList);
                inUnorderedList = false;
                tableLines.add(trimmed);
                continue;
            }
            if (!tableLines.isEmpty()) {
                flushTable(html, tableLines);
            }

            // 分隔线
            if (trimmed.matches("^(-{3,}|\\*{3,})$")) {
                closeUnorderedList(html, inUnorderedList);
                inUnorderedList = false;
                html.append("<hr>");
                continue;
            }

            // 标题
            if (trimmed.startsWith("####")) {
                closeUnorderedList(html, inUnorderedList);
                inUnorderedList = false;
                html.append("<h5>").append(inline(trimmed.substring(4))).append("</h5>");
                continue;
            }
            if (trimmed.startsWith("###")) {
                closeUnorderedList(html, inUnorderedList);
                inUnorderedList = false;
                html.append("<h4>").append(inline(trimmed.substring(3))).append("</h4>");
                continue;
            }
            if (trimmed.startsWith("##")) {
                closeUnorderedList(html, inUnorderedList);
                inUnorderedList = false;
                html.append("<h3>").append(inline(trimmed.substring(2))).append("</h3>");
                continue;
            }
            if (trimmed.startsWith("#")) {
                closeUnorderedList(html, inUnorderedList);
                inUnorderedList = false;
                html.append("<h2>").append(inline(trimmed.substring(1))).append("</h2>");
                continue;
            }

            // 有序列表: HTMLEditorKit 不支持 ol 自动递增编号, 保留原文编号渲染为块级行
            if (trimmed.matches("^\\d+[.)].+")) {
                closeUnorderedList(html, inUnorderedList);
                inUnorderedList = false;
                html.append("<div>").append(inline(trimmed)).append("</div>");
                continue;
            }
            // 无序列表
            if (trimmed.startsWith("- ") || trimmed.startsWith("* ")) {
                if (!inUnorderedList) {
                    html.append("<ul>");
                    inUnorderedList = true;
                }
                html.append("<li>").append(inline(trimmed.substring(2))).append("</li>");
                continue;
            }

            closeUnorderedList(html, inUnorderedList);
            inUnorderedList = false;

            // 空行忽略(不产生额外间距)
            if (trimmed.isEmpty()) {
                continue;
            }
            // 普通行: 连续拼接为一段文本(不换行, 由窗口宽度自然回绕)
            html.append(inline(trimmed)).append(' ');
        }

        if (inCodeBlock) {
            html.append("</font></pre>");
        }
        flushTable(html, tableLines);
        closeUnorderedList(html, inUnorderedList);
        return html.toString();
    }

    /**
     * 是否为表格行(以 | 开头且以 | 结尾)
     */
    private static boolean isTableLine(String trimmed) {
        return trimmed.startsWith("|") && trimmed.endsWith("|") && trimmed.length() > 1;
    }

    /**
     * 是否为表格分隔行(如 |---|---| 或 |:---|:---:|)
     */
    private static boolean isTableSeparator(String line) {
        String inner = line.substring(1, line.length() - 1);
        String[] parts = inner.split("\\|", -1);
        for (String part : parts) {
            if (!part.trim().matches("^:?-{3,}:?$")) {
                return false;
            }
        }
        return true;
    }

    /**
     * 输出表格(HTML 3.2 table, 首行为表头)
     */
    private static void flushTable(StringBuilder html, List<String> tableLines) {
        if (tableLines.isEmpty()) {
            return;
        }
        html.append("<table border=\"1\" cellspacing=\"0\" cellpadding=\"4\">");
        boolean first = true;
        for (String line : tableLines) {
            // 分隔行跳过
            if (isTableSeparator(line)) {
                continue;
            }
            String inner = line.substring(1, line.length() - 1);
            String[] cells = inner.split("\\|", -1);
            html.append("<tr>");
            for (String cell : cells) {
                if (first) {
                    html.append("<th>").append(inline(cell.trim())).append("</th>");
                } else {
                    html.append("<td>").append(inline(cell.trim())).append("</td>");
                }
            }
            html.append("</tr>");
            first = false;
        }
        html.append("</table>");
        tableLines.clear();
    }

    /**
     * 收尾未关闭的无序列表
     */
    private static void closeUnorderedList(StringBuilder html, boolean inUnorderedList) {
        if (inUnorderedList) {
            html.append("</ul>");
        }
    }

    /**
     * 行内元素: 粗体、斜体、行内代码
     */
    private static String inline(String text) {
        String escaped = escape(text);
        // 粗体
        escaped = escaped.replaceAll("\\*\\*(.+?)\\*\\*", "<b>$1</b>");
        // 斜体(单个星号, 排除已处理的粗体)
        escaped = escaped.replaceAll("(?<!\\*)\\*([^*\\n]+)\\*(?!\\*)", "<i>$1</i>");
        // 行内代码
        escaped = escaped.replaceAll("`([^`\\n]+)`", "<font face=\"Monospaced\">$1</font>");
        return escaped;
    }

    /**
     * HTML 转义
     */
    private static String escape(String s) {
        return s.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;");
    }
}
