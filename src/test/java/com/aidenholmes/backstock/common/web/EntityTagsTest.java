package com.aidenholmes.backstock.common.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.aidenholmes.backstock.common.error.VersionMismatchException;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

class EntityTagsTest {

    @ParameterizedTest(name = "{0} -> {1}")
    @CsvSource(delimiter = '|', textBlock = """
            "0"                  | 0
            "3"                  | 3
            ' "3" '              | 3
            "999999999999999999" | 999999999999999999
        """)
    void parsesStrongVersionTag(String ifMatch, long expectedVersion) {
        assertThat(EntityTags.parseIfMatch(ifMatch)).isEqualTo(expectedVersion);
    }

    @ParameterizedTest(name = "{0}")
    @NullAndEmptySource
    @ValueSource(strings = {"   "})
    void requiresEntityTag(@Nullable String ifMatch) {
        assertThatThrownBy(() -> EntityTags.parseIfMatch(ifMatch))
                .isInstanceOf(PreconditionRequiredException.class)
                .hasMessageStartingWith("If-Match header is required");
    }

    @Test
    void rejectsWildcardTag() {
        assertThatThrownBy(() -> EntityTags.parseIfMatch("*"))
                .isInstanceOf(PreconditionRequiredException.class)
                .hasMessageStartingWith("If-Match: * is not accepted");
    }

    @ParameterizedTest(name = "{0}")
    @ValueSource(
            strings = {
                "W/\"3\"",
                "3",
                "\"abc\"",
                "\"03\"",
                "\"-1\"",
                "\"9999999999999999999\"",
                "\"3\", \"4\"",
                "\"3",
                "\"\""
            })
    void rejectsTagThatCannotMatchAnyVersion(String ifMatch) {
        assertThatThrownBy(() -> EntityTags.parseIfMatch(ifMatch)).isInstanceOf(VersionMismatchException.class);
    }

    @ParameterizedTest(name = "{0} -> {1}")
    @CsvSource(delimiter = '|', textBlock = """
            0    | "0"
            42   | "42"
            9999 | "9999"
        """)
    void formatsVersionAsStrongTag(long version, String expectedTag) {
        assertThat(EntityTags.of(version)).isEqualTo(expectedTag);
    }

    @ParameterizedTest(name = "{0}")
    @ValueSource(longs = {0, 42, 999_999_999_999_999_999L})
    void parsesTagFormattedFromVersion(long version) {
        assertThat(EntityTags.parseIfMatch(EntityTags.of(version))).isEqualTo(version);
    }
}
