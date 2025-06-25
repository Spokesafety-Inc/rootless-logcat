package com.luxoft.spokelogcat;

public class StringUtils {

    private static final int INDEX_NOT_FOUND = -1;

    public static boolean containsIgnoreCase(String str, String searchStr) {
        return indexOfIgnoreCase(str, searchStr, 0) >= 0;
    }

    public static boolean containsIgnoreCase(char str, String searchStr) {
        return containsIgnoreCase(String.valueOf(str), searchStr);
    }

    public static int indexOfIgnoreCase(String str, String searchString) {
        return indexOfIgnoreCase(str, searchString, 0);
    }

    public static int indexOfIgnoreCase(String str, String searchString, int startPosition) {
        int startIndex = startPosition;
        if (str == null || searchString == null) {
            return INDEX_NOT_FOUND;
        }
        if (startIndex < 0) {
            startIndex = 0;
        }
        int endLimit = str.length() - searchString.length() + 1;
        if (startIndex > endLimit) {
            return INDEX_NOT_FOUND;
        }
        if (searchString.isEmpty()) {
            return startIndex;
        }
        for (int i = startIndex; i < endLimit; i++) {
            if (str.regionMatches(true, i, searchString, 0, searchString.length())) {
                return i;
            }
        }
        return INDEX_NOT_FOUND;
    }
}