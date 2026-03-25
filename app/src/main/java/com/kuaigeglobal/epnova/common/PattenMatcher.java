package com.kuaigeglobal.epnova.common;

import android.util.Pair;


import com.kuaigeglobal.epnova.widget.MentionEditText;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class PattenMatcher implements MentionEditText.MentionMatcher {
        Pattern pattern;
        public PattenMatcher(String regex) {
            pattern=  Pattern.compile(regex);
        }

        @Override
        public List<Pair<Integer,String>> match(String text) {
            Matcher matcher = pattern.matcher(text);
            List<Pair<Integer,String>> result=new ArrayList<>();
            while (matcher.find()) {
               int start= matcher.start();

                String mentionText = matcher.group();

                if (!result.contains(mentionText)) {
                    result.add(new Pair(start,mentionText));
                }
            }
            return result;
        }
    }