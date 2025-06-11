package com.luxoft.spokelogcat;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Line {

    public char level = 'D';
    public String tag = null;
    public String content;

    public Line(String content) {
        this.content = content;
        Matcher matcher = linePattern.matcher(content);
        if (matcher.matches()) {
            String group1 = matcher.group(1);
            if (group1 != null && !group1.isEmpty()) {
                level = group1.charAt(0);
            }

            String group2 = matcher.group(2);
            if (group2 != null) {
                tag = group2.trim();
            }
        }
    }

    private static Pattern linePattern = Pattern.compile("\\d\\d-\\d\\d \\d\\d:\\d\\d:\\d\\d\\.\\d\\d\\d (\\w)/(\\w+).*");

}
