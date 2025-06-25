/*
 * Copyright 2016 - 2022 Anton Tananaev (anton.tananaev@gmail.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.luxoft.spokelogcat;

import android.content.Context;
import android.content.SharedPreferences;
import android.text.TextUtils;
import java.lang.StringBuilder;

public class Settings
{
    private static final String SETTINGS = "settings";
    private static final int MAX_COUNT = 10;
    private static final String KEY_LEVEL_HISTORY = "levelHistory";
    private static final String KEY_KEYWORD_HISTORY = "keywordHistory";
    private static final String KEY_SEARCH_HISTORY = "searchHistory";
    private static final Settings INSTANCE = new Settings();

    private Settings() {
        // Private constructor prevents instantiation
    }

    public static Settings getInstance() {
        return INSTANCE;
    }

    // for filtering
    public static String[] getLevelHistory(Context context) {
        return getHistory(context, KEY_LEVEL_HISTORY);
    }

    public static String[] getKeywordHistory(Context context) {
        return getHistory(context, KEY_KEYWORD_HISTORY);
    }

    private static String[] getHistory(Context context, String key) {
        SharedPreferences pref = context.getSharedPreferences(SETTINGS, Context.MODE_PRIVATE);
        String history = pref.getString(key, "");
        if (TextUtils.isEmpty(history)) {
            return new String[0];
        } else {
            return history.split("\n");
        }
    }

    public static void appendLevelHistory(
            Context context,
            String[] levelHistory,
            String[] keywordHistory,
            String newLevel,
            String newKeyword)
    {
        String levelHistoryData = makeHistoryData(levelHistory, newLevel);
        String keywordHistoryData = makeHistoryData(keywordHistory, newKeyword);
        if (TextUtils.isEmpty(levelHistoryData) && TextUtils.isEmpty(keywordHistoryData)) {
            return;
        }
        SharedPreferences pref = context.getSharedPreferences(SETTINGS, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = pref.edit();
        if (!TextUtils.isEmpty(levelHistoryData)) {
            editor.putString(KEY_LEVEL_HISTORY, levelHistoryData);
        }
        if (!TextUtils.isEmpty(keywordHistoryData)) {
            editor.putString(KEY_KEYWORD_HISTORY, keywordHistoryData);
        }
        editor.apply();
    }

    private static String makeHistoryData(String[] history, String data) {
        if (TextUtils.isEmpty(data) || history == null) {
            return null;
        }
        if (history.length > 0 && TextUtils.equals(history[0], data)) {
            return null;
        }
        StringBuilder sb = new StringBuilder();
        sb.append(data);
        int i = 0;
        while (i < history.length && i < MAX_COUNT) {
            if (TextUtils.equals(history[i], data)) {
                i++;
                continue;
            }
            sb.append("\n").append(history[i]);
            i++;
        }
        return sb.toString();
    }

    // for highlighting
    public static String[] getSearchHistory(Context context) {
        return getHistory(context, KEY_SEARCH_HISTORY);
    }

    public static void appendSearchHistory(Context context, String[] searchHistory, String newSearchWord) {
        String searchHistoryData = makeHistoryData(searchHistory, newSearchWord);
        if (TextUtils.isEmpty(searchHistoryData)) {
            return;
        }
        SharedPreferences pref = context.getSharedPreferences(SETTINGS, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = pref.edit();
        editor.putString(KEY_SEARCH_HISTORY, searchHistoryData);
        editor.apply();
    }

}
