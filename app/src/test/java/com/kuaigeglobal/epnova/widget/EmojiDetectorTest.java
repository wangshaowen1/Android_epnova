package com.kuaigeglobal.epnova.widget;

import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class EmojiDetectorTest {
    @Test
    public void allowsSupplementaryLettersAndNonEmojiSymbols() {
        assertFalse(EmojiDetector.containsEmoji("中文\uD842\uDFB7\uD835\uDC9C\uD834\uDD1E♪♫♬♭♯"));
        assertFalse(EmojiDetector.containsEmoji("abc 0123456789 # *\n\t"));
        assertFalse(EmojiDetector.containsEmoji("\u200D\uFE0F\u20E3"));
        assertFalse(EmojiDetector.containsEmoji(""));
        assertFalse(EmojiDetector.containsEmoji(null));
    }

    @Test
    public void rejectsEmojiCharactersAndSequences() {
        String[] emoji = {"😀", "❤️", "❤", "©", "1️⃣", "#⃣", "*️⃣", "👍🏽", "🇨🇳", "👨‍👩‍👧‍👦",
                "\uD83E\uDEEA", "\uD83C\uDFF4\uDB40\uDC67\uDB40\uDC62\uDB40\uDC65\uDB40\uDC6E\uDB40\uDC67\uDB40\uDC7F"};
        for (String text : emoji) assertTrue(text, EmojiDetector.containsEmoji(text));
    }

    @Test
    public void keycapBasesRequireTheCombiningKeycap() {
        for (char base : "#*0123456789".toCharArray()) {
            assertFalse(EmojiDetector.containsEmoji(base + ""));
            assertFalse(EmojiDetector.containsEmoji(base + "\uFE0F"));
            assertTrue(EmojiDetector.containsEmoji(base + "\u20E3"));
            assertTrue(EmojiDetector.containsEmoji(base + "\uFE0F\u20E3"));
        }
    }

    @Test
    public void sourceSliceDoesNotReadOutsideInsertion() {
        assertFalse(inserting("😀ok😀", 2, 4, "", 0, 0));
        assertFalse(inserting("😀", 0, 1, "", 0, 0));
        assertFalse(inserting("1\uFE0F\u20E3", 0, 2, "", 0, 0));
        assertTrue(inserting("x1️⃣y", 1, 4, "", 0, 0));
    }

    @Test
    public void keycapCanBeCompletedAcrossEitherReplacementBoundary() {
        assertTrue(inserting("\u20E3", 0, 1, "1", 1, 1));
        assertTrue(inserting("\u20E3", 0, 1, "1\uFE0F", 2, 2));
        assertTrue(inserting("1", 0, 1, "\uFE0F\u20E3", 0, 0));
        assertTrue(inserting("\uFE0F", 0, 1, "1x\u20E3", 1, 2));
        assertTrue(inserting("text1", 0, 5, "x\uFE0F\u20E3", 1, 1));
        assertFalse(inserting("x", 0, 1, "1\u20E3", 1, 1));
    }

    @Test
    public void splitSurrogatePairsAreRecognizedWithoutRejectingHanCharacters() {
        assertTrue(inserting("\uDE00", 0, 1, "\uD83D", 1, 1));
        assertTrue(inserting("\uD83D", 0, 1, "\uDE00", 0, 0));
        assertFalse(inserting("\uDFB7", 0, 1, "\uD842", 1, 1));
        assertFalse(inserting("\uD842", 0, 1, "\uDFB7", 0, 0));
    }

    @Test
    public void existingEmojiDoesNotBlockOrdinaryEditsOrDeletion() {
        assertFalse(inserting("a", 0, 1, "😀", 0, 0));
        assertFalse(inserting("a", 0, 1, "😀", 2, 2));
        assertFalse(inserting("abc", 0, 3, "😀", 0, 0));
        assertFalse(inserting("a", 0, 1, "1️⃣", 3, 3));
        assertFalse(inserting("", 0, 0, "😀", 0, 2));
    }

    private boolean inserting(String source, int start, int end, String dest, int dstart, int dend) {
        return EmojiDetector.containsEmojiOnInsertion(source, start, end, dest, dstart, dend);
    }
}
