package com.kuaigeglobal.epnova.widget;

/*
 * UNICODE LICENSE V3
 *
 * COPYRIGHT AND PERMISSION NOTICE
 *
 * Copyright © 1991-2026 Unicode, Inc.
 *
 * NOTICE TO USER: Carefully read the following legal agreement. BY
 * DOWNLOADING, INSTALLING, COPYING OR OTHERWISE USING DATA FILES, AND/OR
 * SOFTWARE, YOU UNEQUIVOCALLY ACCEPT, AND AGREE TO BE BOUND BY, ALL OF THE
 * TERMS AND CONDITIONS OF THIS AGREEMENT. IF YOU DO NOT AGREE, DO NOT
 * DOWNLOAD, INSTALL, COPY, DISTRIBUTE OR USE THE DATA FILES OR SOFTWARE.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a
 * copy of data files and any associated documentation (the "Data Files") or
 * software and any associated documentation (the "Software") to deal in the
 * Data Files or Software without restriction, including without limitation
 * the rights to use, copy, modify, merge, publish, distribute, and/or sell
 * copies of the Data Files or Software, and to permit persons to whom the
 * Data Files or Software are furnished to do so, provided that either (a)
 * this copyright and permission notice appear with all copies of the Data
 * Files or Software, or (b) this copyright and permission notice appear in
 * associated Documentation.
 *
 * THE DATA FILES AND SOFTWARE ARE PROVIDED "AS IS", WITHOUT WARRANTY OF ANY
 * KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
 * MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT OF
 * THIRD PARTY RIGHTS.
 *
 * IN NO EVENT SHALL THE COPYRIGHT HOLDER OR HOLDERS INCLUDED IN THIS NOTICE
 * BE LIABLE FOR ANY CLAIM, OR ANY SPECIAL INDIRECT OR CONSEQUENTIAL DAMAGES,
 * OR ANY DAMAGES WHATSOEVER RESULTING FROM LOSS OF USE, DATA OR PROFITS,
 * WHETHER IN AN ACTION OF CONTRACT, NEGLIGENCE OR OTHER TORTIOUS ACTION,
 * ARISING OUT OF OR IN CONNECTION WITH THE USE OR PERFORMANCE OF THE DATA
 * FILES OR SOFTWARE.
 *
 * Except as contained in this notice, the name of a copyright holder shall
 * not be used in advertising or otherwise to promote the sale, use or other
 * dealings in these Data Files or Software without prior written
 * authorization of the copyright holder.
 */

/**
 * Unicode 17.0 Emoji 属性表；按码点识别，不依赖 Android 系统的 Unicode 版本。
 * Emoji 属性中的文本样式字符（如爱心、版权符号）也受限制；数字、#、* 只匹配键帽序列。
 * 数据来源：https://www.unicode.org/Public/17.0.0/ucd/emoji/emoji-data.txt
 * 更新时从 Emoji 属性重新生成合并区间，并用对应版本 emoji-test.txt 验证。
 */
final class EmojiDetector {
    private EmojiDetector() {}

    static boolean containsEmoji(CharSequence text) {
        return text != null && containsEmoji(text, 0, text.length(), 0, text.length());
    }

    /** 只检查本次新增内容及与前后文组合的字符，不追溯拒绝原有 emoji 或删除操作。 */
    static boolean containsEmojiOnInsertion(CharSequence source, int start, int end,
                                            CharSequence dest, int dstart, int dend) {
        if (start == end) return false;
        if (containsEmoji(source, start, end, start, end)) return true;
        int length = end - start;
        // 一个码点最多 2 个 UTF-16 单元；键帽最长为数字/#/* + VS16 + U+20E3。
        // 仅拼接边界附近字符，避免每次输入复制或扫描整个 Editable。
        if (dstart > 0) {
            StringBuilder boundary = new StringBuilder(6);
            boundary.append(dest, Math.max(0, dstart - 2), dstart);
            int insertedStart = boundary.length();
            boundary.append(source, start, Math.min(end, start + 2));
            int insertedEnd = boundary.length();
            if (length <= 2) boundary.append(dest, dend, Math.min(dest.length(), dend + 2));
            if (containsEmoji(boundary, 0, boundary.length(), insertedStart, insertedEnd)) return true;
        }
        if (dend < dest.length() && (dstart == 0 || length > 2)) {
            StringBuilder boundary = new StringBuilder(4);
            boundary.append(source, Math.max(start, end - 2), end);
            int insertedEnd = boundary.length();
            boundary.append(dest, dend, Math.min(dest.length(), dend + 2));
            if (containsEmoji(boundary, 0, boundary.length(), 0, insertedEnd)) return true;
        }
        return false;
    }

    private static boolean containsEmoji(CharSequence text, int start, int end,
                                         int changedStart, int changedEnd) {
        for (int i = start; i < end;) {
            char ch = text.charAt(i);
            int codePoint = ch;
            int next = i + 1;
            if (Character.isHighSurrogate(ch) && next < end && Character.isLowSurrogate(text.charAt(next))) {
                codePoint = Character.toCodePoint(ch, text.charAt(next++));
            }
            int emojiEnd = next;
            boolean emoji;
            if (codePoint == '#' || codePoint == '*' || (codePoint >= '0' && codePoint <= '9')) {
                if (emojiEnd < end && text.charAt(emojiEnd) == '\uFE0F') emojiEnd++;
                emoji = emojiEnd < end && text.charAt(emojiEnd) == '\u20E3';
                if (emoji) emojiEnd++;
            } else {
                emoji = isEmojiCodePoint(codePoint);
            }
            if (emoji && i < changedEnd && emojiEnd > changedStart) return true;
            i = next;
        }
        return false;
    }

    private static boolean isEmojiCodePoint(int codePoint) {
        if (codePoint < EMOJI_RANGES[0]) return false;
        int low = 0;
        int high = EMOJI_RANGES.length / 2 - 1;
        while (low <= high) {
            int middle = (low + high) >>> 1;
            int index = middle * 2;
            if (codePoint < EMOJI_RANGES[index]) high = middle - 1;
            else if (codePoint > EMOJI_RANGES[index + 1]) low = middle + 1;
            else return true;
        }
        return false;
    }

    // Emoji=Yes，排除单独的键帽基础字符；区间已排序、合并。
    private static final int[] EMOJI_RANGES = {
        0xA9, 0xA9, 0xAE, 0xAE, 0x203C, 0x203C, 0x2049, 0x2049,
        0x2122, 0x2122, 0x2139, 0x2139, 0x2194, 0x2199, 0x21A9, 0x21AA,
        0x231A, 0x231B, 0x2328, 0x2328, 0x23CF, 0x23CF, 0x23E9, 0x23F3,
        0x23F8, 0x23FA, 0x24C2, 0x24C2, 0x25AA, 0x25AB, 0x25B6, 0x25B6,
        0x25C0, 0x25C0, 0x25FB, 0x25FE, 0x2600, 0x2604, 0x260E, 0x260E,
        0x2611, 0x2611, 0x2614, 0x2615, 0x2618, 0x2618, 0x261D, 0x261D,
        0x2620, 0x2620, 0x2622, 0x2623, 0x2626, 0x2626, 0x262A, 0x262A,
        0x262E, 0x262F, 0x2638, 0x263A, 0x2640, 0x2640, 0x2642, 0x2642,
        0x2648, 0x2653, 0x265F, 0x2660, 0x2663, 0x2663, 0x2665, 0x2666,
        0x2668, 0x2668, 0x267B, 0x267B, 0x267E, 0x267F, 0x2692, 0x2697,
        0x2699, 0x2699, 0x269B, 0x269C, 0x26A0, 0x26A1, 0x26A7, 0x26A7,
        0x26AA, 0x26AB, 0x26B0, 0x26B1, 0x26BD, 0x26BE, 0x26C4, 0x26C5,
        0x26C8, 0x26C8, 0x26CE, 0x26CF, 0x26D1, 0x26D1, 0x26D3, 0x26D4,
        0x26E9, 0x26EA, 0x26F0, 0x26F5, 0x26F7, 0x26FA, 0x26FD, 0x26FD,
        0x2702, 0x2702, 0x2705, 0x2705, 0x2708, 0x270D, 0x270F, 0x270F,
        0x2712, 0x2712, 0x2714, 0x2714, 0x2716, 0x2716, 0x271D, 0x271D,
        0x2721, 0x2721, 0x2728, 0x2728, 0x2733, 0x2734, 0x2744, 0x2744,
        0x2747, 0x2747, 0x274C, 0x274C, 0x274E, 0x274E, 0x2753, 0x2755,
        0x2757, 0x2757, 0x2763, 0x2764, 0x2795, 0x2797, 0x27A1, 0x27A1,
        0x27B0, 0x27B0, 0x27BF, 0x27BF, 0x2934, 0x2935, 0x2B05, 0x2B07,
        0x2B1B, 0x2B1C, 0x2B50, 0x2B50, 0x2B55, 0x2B55, 0x3030, 0x3030,
        0x303D, 0x303D, 0x3297, 0x3297, 0x3299, 0x3299, 0x1F004, 0x1F004,
        0x1F0CF, 0x1F0CF, 0x1F170, 0x1F171, 0x1F17E, 0x1F17F, 0x1F18E, 0x1F18E,
        0x1F191, 0x1F19A, 0x1F1E6, 0x1F1FF, 0x1F201, 0x1F202, 0x1F21A, 0x1F21A,
        0x1F22F, 0x1F22F, 0x1F232, 0x1F23A, 0x1F250, 0x1F251, 0x1F300, 0x1F321,
        0x1F324, 0x1F393, 0x1F396, 0x1F397, 0x1F399, 0x1F39B, 0x1F39E, 0x1F3F0,
        0x1F3F3, 0x1F3F5, 0x1F3F7, 0x1F4FD, 0x1F4FF, 0x1F53D, 0x1F549, 0x1F54E,
        0x1F550, 0x1F567, 0x1F56F, 0x1F570, 0x1F573, 0x1F57A, 0x1F587, 0x1F587,
        0x1F58A, 0x1F58D, 0x1F590, 0x1F590, 0x1F595, 0x1F596, 0x1F5A4, 0x1F5A5,
        0x1F5A8, 0x1F5A8, 0x1F5B1, 0x1F5B2, 0x1F5BC, 0x1F5BC, 0x1F5C2, 0x1F5C4,
        0x1F5D1, 0x1F5D3, 0x1F5DC, 0x1F5DE, 0x1F5E1, 0x1F5E1, 0x1F5E3, 0x1F5E3,
        0x1F5E8, 0x1F5E8, 0x1F5EF, 0x1F5EF, 0x1F5F3, 0x1F5F3, 0x1F5FA, 0x1F64F,
        0x1F680, 0x1F6C5, 0x1F6CB, 0x1F6D2, 0x1F6D5, 0x1F6D8, 0x1F6DC, 0x1F6E5,
        0x1F6E9, 0x1F6E9, 0x1F6EB, 0x1F6EC, 0x1F6F0, 0x1F6F0, 0x1F6F3, 0x1F6FC,
        0x1F7E0, 0x1F7EB, 0x1F7F0, 0x1F7F0, 0x1F90C, 0x1F93A, 0x1F93C, 0x1F945,
        0x1F947, 0x1F9FF, 0x1FA70, 0x1FA7C, 0x1FA80, 0x1FA8A, 0x1FA8E, 0x1FAC6,
        0x1FAC8, 0x1FAC8, 0x1FACD, 0x1FADC, 0x1FADF, 0x1FAEA, 0x1FAEF, 0x1FAF8,
    };
}
