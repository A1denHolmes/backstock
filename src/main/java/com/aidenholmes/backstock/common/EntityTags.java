package com.aidenholmes.backstock.common;

import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.jspecify.annotations.Nullable;

/**
 * Converts between entity versions and the strong entity tags used in {@code ETag} and {@code If-Match}.
 */
public final class EntityTags {

    private static final Pattern STRONG_VERSION_TAG = Pattern.compile("\"(0|[1-9][0-9]{0,17})\"");
    private static final String MISSING_TAG_DETAIL =
            "If-Match header is required. Send the ETag from the latest read of the resource";
    private static final String WILDCARD_DETAIL =
            "If-Match: * is not accepted. Send the ETag from the latest read of the resource";
    private static final String VERSION_MISMATCH_DETAIL = "If-Match does not match the current version of the resource";

    private EntityTags() {}

    /**
     * Returns the strong entity tag for a version, such as {@code "3"} for version 3.
     */
    public static String of(long version) {
        return "\"" + version + "\"";
    }

    /**
     * Returns the version from an {@code If-Match} value that holds exactly one strong entity tag.
     *
     * @param ifMatch header value, or {@code null} if the header is absent
     * @return version from the entity tag
     * @throws PreconditionRequiredException if the value is absent, blank or {@code *}
     * @throws VersionMismatchException if the value is not a single strong tag holding a version number without leading
     *     zeros
     */
    public static long parseIfMatch(@Nullable String ifMatch) {
        if (ifMatch == null || ifMatch.isBlank()) {
            throw new PreconditionRequiredException(MISSING_TAG_DETAIL);
        }

        String entityTag = ifMatch.strip();

        if (entityTag.equals("*")) {
            throw new PreconditionRequiredException(WILDCARD_DETAIL);
        }

        Matcher matcher = STRONG_VERSION_TAG.matcher(entityTag);
        if (!matcher.matches()) {
            throw new VersionMismatchException(VERSION_MISMATCH_DETAIL);
        }

        return Long.parseLong(Objects.requireNonNull(matcher.group(1)));
    }
}
