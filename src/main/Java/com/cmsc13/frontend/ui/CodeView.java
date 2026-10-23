package com.cmsc13.frontend.ui;

import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;

import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * A read-only, IDE-style code block with line numbers and simple syntax
 * highlighting for C, Java, Python, JavaScript and HTML snippets.
 */
public class CodeView extends HBox {

    private static final Set<String> KEYWORDS = Set.of(
            // C / Java / JS
            "int", "double", "float", "char", "void", "long", "short", "bool", "boolean", "byte", "unsigned",
            "const", "static", "final", "public", "private", "protected", "class", "interface", "extends",
            "implements", "abstract", "new", "return", "if", "else", "for", "while", "do", "switch", "case",
            "break", "continue", "default", "struct", "typedef", "enum", "this", "super", "try", "catch",
            "throw", "throws", "finally", "import", "package", "var", "let", "function", "async", "await",
            "null", "true", "false", "instanceof", "typeof", "sizeof", "String", "record", "of",
            // Python
            "def", "lambda", "in", "is", "not", "and", "or", "None", "True", "False", "elif", "pass",
            "yield", "from", "as", "with", "self", "print", "range", "nonlocal", "global");

    private static final Pattern TOKENS = Pattern.compile(
            "(?<COMMENT>//.*|#(?!include|define).*|<!--.*?-->)"
                    + "|(?<STRING>\"(?:\\\\.|[^\"\\\\])*\"|'(?:\\\\.|[^'\\\\])*'|`[^`]*`)"
                    + "|(?<PRE>#include|#define)"
                    + "|(?<NUMBER>\\b\\d+(?:\\.\\d+)?\\b)"
                    + "|(?<BLANK>_{3,})"
                    + "|(?<WORD>[A-Za-z_][A-Za-z0-9_]*)");

    public CodeView(String code) {
        getStyleClass().add("code-view");
        String[] lines = code.split("\n", -1);

        // One row per source line keeps the line numbers perfectly aligned.
        VBox rows = new VBox(0);
        for (int i = 0; i < lines.length; i++) {
            Text n = new Text(String.format("%2d", i + 1));
            n.getStyleClass().add("code-line-no");
            TextFlow flow = new TextFlow();
            flow.getStyleClass().add("code-flow");
            highlight(flow, lines[i].isEmpty() ? " " : lines[i]);
            HBox row = new HBox(12, n, flow);
            rows.getChildren().add(row);
        }
        getChildren().add(rows);
    }

    private static void highlight(TextFlow flow, String line) {
        Matcher m = TOKENS.matcher(line);
        int last = 0;
        while (m.find()) {
            if (m.start() > last) {
                flow.getChildren().add(styled(line.substring(last, m.start()), "code-plain"));
            }
            String token = m.group();
            String style;
            if (m.group("COMMENT") != null) {
                style = "code-comment";
            } else if (m.group("STRING") != null) {
                style = "code-string";
            } else if (m.group("PRE") != null) {
                style = "code-keyword";
            } else if (m.group("NUMBER") != null) {
                style = "code-number";
            } else if (m.group("BLANK") != null) {
                style = "code-blank";
            } else if (KEYWORDS.contains(token)) {
                style = "code-keyword";
            } else if (m.end() < line.length() && line.charAt(m.end()) == '(') {
                style = "code-call";
            } else {
                style = "code-plain";
            }
            flow.getChildren().add(styled(token, style));
            last = m.end();
        }
        if (last < line.length()) {
            flow.getChildren().add(styled(line.substring(last), "code-plain"));
        }
    }

    private static Text styled(String s, String styleClass) {
        Text t = new Text(s);
        t.getStyleClass().addAll("code-text", styleClass);
        return t;
    }
}
