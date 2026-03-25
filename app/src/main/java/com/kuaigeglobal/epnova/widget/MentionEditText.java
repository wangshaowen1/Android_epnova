/*
 * Copyright 2016 Andy
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.kuaigeglobal.epnova.widget;

import android.content.Context;
import android.graphics.Color;
import android.text.Editable;
import android.text.InputType;
import android.text.Spannable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.text.style.ForegroundColorSpan;
import android.util.AttributeSet;
import android.util.Pair;
import android.view.KeyEvent;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputConnection;
import android.view.inputmethod.InputConnectionWrapper;
import android.widget.EditText;


import com.kuaigeglobal.epnova.common.PattenMatcher;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


/**
 * MentionEditText adds some useful features for mention string(@xxxx), such as highlight,
 * intelligent deletion, intelligent selection and '@' input detection, etc.
 *
 * @author Andy
 */
public class MentionEditText extends XEditText {
    public static final String DEFAULT_METION_TAG = "@";
    public static final String DEFAULT_MENTION_PATTERN = "@[\\u4e00-\\u9fa5\\w\\-]+";

    private Map<String, MentionMatcher> mMatcherMap = new HashMap<>();
    private Runnable mAction;

    public interface MentionMatcher {
        /**
         * return  the index of the first character in the text and text
         * @param text
         * @return
         */
        List<Pair<Integer,String>> match(String text);
    }


    public interface SelectionChangeListener{
        public void onSelectionChanged(int selStart, int selEnd);
    }
    private SelectionChangeListener selectionChangeListener;

    public void setSelectionChangeListener(SelectionChangeListener selectionChangeListener) {
        this.selectionChangeListener = selectionChangeListener;
    }


    private int mMentionTextColor;

    private boolean mIsSelected;
    private Range mLastSelectedRange;
    private List<Range> mRangeArrayList;

    private OnMentionInputListener mOnMentionInputListener;

    public MentionEditText(Context context) {
        super(context);
        init();
    }

    public MentionEditText(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public MentionEditText(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    @Override
    public InputConnection onCreateInputConnection(EditorInfo outAttrs) {
        return new HackInputConnection(super.onCreateInputConnection(outAttrs), true, this);
    }

    @Override
    public void setText(final CharSequence text, BufferType type) {
        super.setText(text, type);
        //hack, put the cursor at the end of text after calling setText() method
        if (mAction == null) {
            mAction = new Runnable() {
                @Override
                public void run() {
                    setSelection(getText().length());
                }
            };
        }
        post(mAction);
    }

    @Override
    protected void onTextChanged(CharSequence text, int start, int lengthBefore, int lengthAfter) {
        colorMentionString();
    }


    @Override
    protected void onSelectionChanged(int selStart, int selEnd) {
        super.onSelectionChanged(selStart, selEnd);
        //avoid infinite recursion after calling setSelection()
        if(selectionChangeListener!=null) {
            selectionChangeListener.onSelectionChanged(selStart, selEnd);
        }
        if (mLastSelectedRange != null && mLastSelectedRange.isEqual(selStart, selEnd)) {
            return;
        }

        //if user cancel a selection of mention string, reset the state of 'mIsSelected'
        Range closestRange = getRangeOfClosestMentionString(selStart, selEnd);
        if (closestRange != null && closestRange.to == selEnd) {
            mIsSelected = false;
        }

        Range nearbyRange = getRangeOfNearbyMentionString(selStart, selEnd);
        //if there is no mention string nearby the cursor, just skip
        if (nearbyRange == null) {
            return;
        }

        //forbid cursor located in the mention string.
        if (selStart == selEnd) {
            setSelection(nearbyRange.getAnchorPosition(selStart));
        } else {
            if (selEnd < nearbyRange.to) {
                setSelection(selStart, nearbyRange.to);
            }
            if (selStart > nearbyRange.from) {
                setSelection(nearbyRange.from, selEnd);
            }
        }
    }


    public void setMatcher(String tag, MentionMatcher matcher) {
        mMatcherMap.clear();
        mMatcherMap.put(tag,matcher);
    }


    public void addMatcher(String tag, MentionMatcher matcher) {
        mMatcherMap.put(tag,matcher);
    }


    /**
     * set regularExpression by tag
     *
     * @param pattern regularExpression
     */
    public void setPattern(String tag, String pattern) {
        mMatcherMap.clear();
        addPattern(tag, pattern);
    }

    /**
     * add regularExpression by tag
     * @param tag   set tag for regularExpression
     * @param pattern   regularExpression
     */
    public void addPattern(String tag, String pattern) {
        mMatcherMap.put(tag,new PattenMatcher(pattern));
    }

    /**
     * set highlight color of mention string
     *
     * @param color value from 'getResources().getColor()' or 'Color.parseColor()' etc.
     */
    public void setMentionTextColor(int color) {
        mMentionTextColor = color;
    }

    /**
     * get a list of mention string
     *
     * @return list of mention string
     */
    public List<String> getMentionList() {
        List<String> mentionList = new ArrayList<>();
        if (TextUtils.isEmpty(getText().toString())) {
            return mentionList;
        }
        for (Map.Entry<String, MentionMatcher> entry : mMatcherMap.entrySet()) {
            List<Pair<Integer,String>> matched = entry.getValue().match(getText().toString());
            if(matched!=null){
                for(Pair<Integer,String> pair:matched){
                    mentionList.add(pair.second);
                }
            }
        }
        return mentionList;
    }

    /**
     * get a list of mention string by tag
     *
     * @return list of mention string
     */
    public List<String> getMentionList(String tag) {
        List<String> mentionList = new ArrayList<>();
        if (TextUtils.isEmpty(getText().toString())) {
            return mentionList;
        }
        for (Map.Entry<String, MentionMatcher> entry : mMatcherMap.entrySet()) {
            if (entry.getKey().equals(tag)){
                List<Pair<Integer,String>> matched = entry.getValue().match(getText().toString());
                if(matched!=null){
                    for(Pair<Integer,String> pair:matched){
                        mentionList.add(pair.second);
                    }
                }
                break;
            }
        }
        return mentionList;
    }

    /**
     * set listener for mention character('@')
     *
     * @param onMentionInputListener MentionEditText.OnMentionInputListener
     */
    public void setOnMentionInputListener(OnMentionInputListener onMentionInputListener) {
        mOnMentionInputListener = onMentionInputListener;
    }

    private void init() {
        mRangeArrayList = new ArrayList<>(5);
        setPattern(DEFAULT_METION_TAG, DEFAULT_MENTION_PATTERN);
        mMentionTextColor = Color.RED;
        //disable suggestion
        setInputType(getInputType()|InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS);
        addTextChangedListener(new MentionTextWatcher());
    }

    private void colorMentionString() {
        //reset state
        mIsSelected = false;
        if (mRangeArrayList != null) {
            mRangeArrayList.clear();
        }

        Editable spannableText = getText();
        if (spannableText == null || TextUtils.isEmpty(spannableText.toString())) {
            return;
        }

        //remove previous spans
        ForegroundColorSpan[] oldSpans = spannableText.getSpans(0, spannableText.length(), ForegroundColorSpan.class);
        for (ForegroundColorSpan oldSpan : oldSpans) {
            spannableText.removeSpan(oldSpan);
        }

        //find mention string and color it
        String text = spannableText.toString();
        for (Map.Entry<String, MentionMatcher> entry : mMatcherMap.entrySet()) {
            int lastMentionIndex = -1;
            List<Pair<Integer,String>> matched = entry.getValue().match(getText().toString());
            if(matched!=null){
                for(Pair<Integer,String> pair:matched){

                    int end = pair.first + pair.second.length();
                    spannableText.setSpan(new ForegroundColorSpan(mMentionTextColor), pair.first, end, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                    lastMentionIndex = end;
                    //record all mention-string's position
                    mRangeArrayList.add(new Range(pair.first, end));
                }
            }


        }
    }

    private Range getRangeOfClosestMentionString(int selStart, int selEnd) {
        if (mRangeArrayList == null) {
            return null;
        }
        for (Range range : mRangeArrayList) {
            if (range.contains(selStart, selEnd)) {
                return range;
            }
        }
        return null;
    }

    private Range getRangeOfNearbyMentionString(int selStart, int selEnd) {
        if (mRangeArrayList == null) {
            return null;
        }
        for (Range range : mRangeArrayList) {
            if (range.isWrappedBy(selStart, selEnd)) {
                return range;
            }
        }
        return null;
    }

    //text watcher for mention character('@')
    private class MentionTextWatcher implements TextWatcher {
        @Override
        public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {
        }

        @Override
        public void onTextChanged(CharSequence charSequence, int index, int i1, int count) {
            if (count == 1 && !TextUtils.isEmpty(charSequence)) {
                char mentionChar = charSequence.toString().charAt(index);
                for (Map.Entry<String, MentionMatcher> entry : mMatcherMap.entrySet()) {
                    if (entry.getKey().equals(String.valueOf(mentionChar)) && mOnMentionInputListener != null) {
                        mOnMentionInputListener.onMentionCharacterInput(entry.getKey());
                        break;
                    }
                }
            }
        }

        @Override
        public void afterTextChanged(Editable editable) {
        }
    }

    //handle the deletion action for mention string, such as '@test'
    private class HackInputConnection extends InputConnectionWrapper {
        private EditText editText;

        HackInputConnection(InputConnection target, boolean mutable, MentionEditText editText) {
            super(target, mutable);
            this.editText = editText;
        }

        @Override
        public boolean sendKeyEvent(KeyEvent event) {
            if (event.getAction() == KeyEvent.ACTION_DOWN && event.getKeyCode() == KeyEvent.KEYCODE_DEL) {
                int selectionStart = editText.getSelectionStart();
                int selectionEnd = editText.getSelectionEnd();
                Range closestRange = getRangeOfClosestMentionString(selectionStart, selectionEnd);
                if (closestRange == null) {
                    mIsSelected = false;
                    return super.sendKeyEvent(event);
                }
                //if mention string has been selected or the cursor is at the beginning of mention string, just use default action(delete)
                if (mIsSelected || selectionStart == closestRange.from) {
                    mIsSelected = false;
                    return super.sendKeyEvent(event);
                } else {
                    //select the mention string
                    mIsSelected = true;
                    mLastSelectedRange = closestRange;
                    setSelection(closestRange.to, closestRange.from);
                }
                return true;
            }
            return super.sendKeyEvent(event);
        }

        @Override
        public boolean deleteSurroundingText(int beforeLength, int afterLength) {
            if (beforeLength == 1 && afterLength == 0) {
                return sendKeyEvent(new KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_DEL))
                        && sendKeyEvent(new KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_DEL));
            }
            return super.deleteSurroundingText(beforeLength, afterLength);
        }
    }

    //helper class to record the position of mention string in EditText
    private class Range {
        int from;
        int to;

        Range(int from, int to) {
            this.from = from;
            this.to = to;
        }

        boolean isWrappedBy(int start, int end) {
            return (start > from && start < to) || (end > from && end < to);
        }

        boolean contains(int start, int end) {
            return from <= start && to >= end;
        }

        boolean isEqual(int start, int end) {
            return (from == start && to == end) || (from == end && to == start);
        }

        int getAnchorPosition(int value) {
            if ((value - from) - (to - value) >= 0) {
                return to;
            } else {
                return from;
            }
        }
    }

    public void notifyMentionChanged(){
        if(getText()!=null&&getText().length()>0) {
            colorMentionString();
        }
    }

    /**
     * Listener for '@' character
     */
    public interface OnMentionInputListener {
        /**
         * call when '@' character is inserted into EditText
         */
        void onMentionCharacterInput(String tag);
    }

}
