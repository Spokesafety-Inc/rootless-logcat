package com.luxoft.spokelogcat;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Line {
    public static final String DEBUG_TAG ="[DBG_TAG]";
    public char level = 'D';
    public String tag = null;
    public String content;
    public boolean isDebugTag = false;

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
        } else {
            isDebugTag = dbgPattern.matcher(content).matches();
        }
    }

    private static final Pattern linePattern = Pattern.compile("\\d\\d-\\d\\d \\d\\d:\\d\\d:\\d\\d\\.\\d\\d\\d (\\w)/(\\w*).*");
    private static final Pattern dbgPattern = Pattern.compile("\\d\\d-\\d\\d \\d\\d:\\d\\d:\\d\\d\\.\\d\\d\\d (\\w) \\" + DEBUG_TAG + ".*");
}
