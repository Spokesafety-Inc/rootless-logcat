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
package com.luxoft.spokelogcat.view;

import android.content.Context;

import com.google.android.material.textfield.TextInputEditText;
import com.luxoft.spokelogcat.Settings;
import android.view.LayoutInflater;
import android.view.View;
import com.luxoft.spokelogcat.R;

public class FilterOptionsController {

    public View baseView = null;
    private Context context = null;
    private TextInputEditText inputLevel = null;
    private TextInputEditText inputKeyword = null;
    private final String[] levelHistory;
    private final String[] keywordHistory;

    public FilterOptionsController(Context ct) {
        context = ct;
        baseView = LayoutInflater.from(context).inflate(R.layout.dialog_filter, null);
        inputLevel = baseView.findViewById(R.id.level);
        inputKeyword = baseView.findViewById(R.id.keyword);
        levelHistory = Settings.getLevelHistory(context);
        keywordHistory = Settings.getKeywordHistory(context);
    }

    public String getLevel() {
        return inputLevel.getText().toString();
    }
    public void setLevel(String level) {
        inputLevel.setText(level);
    }

    public String getKeyword() {
        return inputKeyword.getText().toString();
    }
    public void setKeyword(String keyword) {
        inputKeyword.setText(keyword);
    }

    public void saveLevelKeyword(String level, String keyword) {
        if (level != null && keyword != null) {
            Settings.appendLevelHistory(context, levelHistory, keywordHistory, level, keyword);
        }
    }

}
