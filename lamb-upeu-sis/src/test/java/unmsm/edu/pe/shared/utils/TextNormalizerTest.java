package unmsm.edu.pe.shared.utils;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class TextNormalizerTest {

    private TextNormalizer textNormalizer;

    @BeforeEach
    void setUp() {
        textNormalizer = new TextNormalizer();
    }

    // --- normalize() ---

    @Test
    void normalize_withNull_returnsNull() {
        assertNull(textNormalizer.normalize(null));
    }

    @Test
    void normalize_withLowercase_returnsUppercaseTrimmed() {
        assertEquals("HELLO WORLD", textNormalizer.normalize("  hello world  "));
    }

    @Test
    void normalize_withMultipleSpaces_collapsesToSingleSpace() {
        assertEquals("HELLO WORLD", textNormalizer.normalize("hello   world"));
    }

    @Test
    void normalize_withAlreadyUppercase_returnsSame() {
        assertEquals("HELLO", textNormalizer.normalize("HELLO"));
    }

    @Test
    void normalize_withEmptyString_returnsEmpty() {
        assertEquals("", textNormalizer.normalize(""));
    }

    @Test
    void normalize_withOnlySpaces_returnsEmpty() {
        assertEquals("", textNormalizer.normalize("   "));
    }

    // --- normalizeSpaces() ---

    @Test
    void normalizeSpaces_withNull_returnsNull() {
        assertNull(textNormalizer.normalizeSpaces(null));
    }

    @Test
    void normalizeSpaces_withLeadingAndTrailingSpaces_trimsThem() {
        assertEquals("hello world", textNormalizer.normalizeSpaces("  hello world  "));
    }

    @Test
    void normalizeSpaces_withMultipleSpaces_collapsesToOne() {
        assertEquals("hello world", textNormalizer.normalizeSpaces("hello   world"));
    }

    @Test
    void normalizeSpaces_preservesOriginalCase() {
        assertEquals("Hello World", textNormalizer.normalizeSpaces("  Hello   World  "));
    }

    @Test
    void normalizeSpaces_withEmptyString_returnsEmpty() {
        assertEquals("", textNormalizer.normalizeSpaces(""));
    }

    // --- toUpperCase() ---

    @Test
    void toUpperCase_withNull_returnsNull() {
        assertNull(textNormalizer.toUpperCase(null));
    }

    @Test
    void toUpperCase_withLowercase_returnsUppercase() {
        assertEquals("HELLO", textNormalizer.toUpperCase("hello"));
    }

    @Test
    void toUpperCase_withLeadingTrailingSpaces_trimsAndUppercases() {
        assertEquals("HELLO", textNormalizer.toUpperCase("  hello  "));
    }

    @Test
    void toUpperCase_withMixedCase_returnsAllUppercase() {
        assertEquals("HELLO WORLD", textNormalizer.toUpperCase("Hello World"));
    }
}