package com.zhang.scanner.ai;

/**
 * 轻量 Markdown 转 HTML(自写实现, 不引第三方库)
 * 支持: 代码块(```)、行内代码(`)、标题(#~###)、粗体(**)、列表(- )
 * 未闭合代码块按进行中状态渲染, 便于流式输出
 */
public final class MarkdownRenderer {

    private MarkdownRenderer() {
    }

    /**
     * Markdown 转 HTML 片段(供 JEditorPane 使用)
     */
    public static String toHtml(String markdown) {
        StringBuilder html = new StringBuilder();
        String[] lines = markdown.split("\n", -1);
        boolean inCodeBlock = false;
        StringBuilder listBuf = null;
        for (String line : lines) {
            String trimmed = line.trim();
            if (trimmed.startsWith("```")) {
                if (inCodeBlock) {
                    html.append("</pre>");
                    inCodeBlock = false;
                } else {
                    html.append("<pre style='margin:4px 0'>");
                    inCodeBlock = true;
                }
                continue;
            }
            if (inCodeBlock) {
                html.append(escape(line)).append('\n');
                continue;
            }
            if (trimmed.startsWith("###")) {
                closeList(html, listBuf);
                listBuf = null;
                html.append("<h5 style='margin:6px 0 2px 0'>").append(inline(trimmed.substring(3))).append("</h5>");
                continue;
            }
            if (trimmed.startsWith("##")) {
                closeList(html, listBuf);
                listBuf = null;
                html.append("<h4 style='margin:8px 0 2px 0'>").append(inline(trimmed.substring(2))).append("</h4>");
                continue;
            }
            if (trimmed.startsWith("#")) {
                closeList(html, listBuf);
                listBuf = null;
                html.append("<h3 style='margin:10px 0 2px 0'>").append(inline(trimmed.substring(1))).append("</h3>");
                continue;
            }
            if (trimmed.startsWith("- ")) {
                if (null == listBuf) {
                    listBuf = new StringBuilder("<ul style='margin:2px 0'>");
                }
                listBuf.append("<li>").append(inline(trimmed.substring(2))).append("</li>");
                continue;
            }
            closeList(html, listBuf);
            listBuf = null;
            if (line.isEmpty()) {
                html.append("<p/>");
                continue;
            }
            html.append(inline(line)).append("<br/>");
        }
        if (inCodeBlock) {
            html.append("</pre>");
        }
        closeList(html, listBuf);
        return html.toString();
    }

    /**
     * 收尾未关闭的列表
     */
    private static void closeList(StringBuilder html, StringBuilder listBuf) {
        if (null != listBuf) {
            html.append(listBuf).append("</ul>");
        }
    }

    /**
     * 行内元素: 粗体、行内代码
     */
    private static String inline(String text) {
        String escaped = escape(text);
        escaped = escaped.replaceAll("\\*\\*(.+?)\\*\\*", "<b>$1</b>");
        escaped = escaped.replaceAll("`(.+?)`", "<code>$1</code>");
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
