/*
 * Dependency-Check Plugin for SonarQube
 * Copyright (C) 2015-2025 dependency-check
 * philipp.dallig@gmail.com
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package org.sonar.dependencycheck.report.store;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Optional;

import edu.umd.cs.findbugs.annotations.Nullable;

/**
 * Builds the key a report is stored under.
 *
 * <p>Project keys may contain ':' and branch names may contain '/' or '..', so every segment is
 * encoded - otherwise the key would be a path traversal in the filesystem store and a key
 * injection in the S3 store. The encoding is a percent encoding over the UTF-8 bytes of the
 * segment that keeps only {@code A-Za-z0-9}, {@code -} and {@code _} literal. It is therefore
 * <em>injective</em>: two different project keys can never share a storage key, which a lossy
 * mapping onto '_' would allow - a user able to create a project could otherwise pick a key that
 * collides with a victim project and overwrite or read back its report.
 *
 * <p>'.' is deliberately not kept literal. That makes {@code ..} impossible in an encoded segment,
 * so the traversal question disappears, and it keeps the encoding unambiguous.
 */
public final class ReportKey {

    public static final String FILE_NAME = "dependency-check-report.html";

    /**
     * Scope of a key that belongs to neither a branch nor a pull request - the normal case for a
     * main-branch analysis. It is public because the reconciliation has to name the same scope
     * vocabulary this class writes: a second copy of these literals elsewhere could drift, and a
     * drifted scope makes every live report look orphaned.
     */
    public static final String DEFAULT_SCOPE = "default";

    private static final String BRANCH_PREFIX = "br-";
    private static final String PULL_REQUEST_PREFIX = "pr-";
    private static final char[] HEX = "0123456789ABCDEF".toCharArray();

    private ReportKey() {
    }

    public static String of(String projectKey, @Nullable String branch, @Nullable String pullRequest) {
        String scope;
        if (pullRequest != null && !pullRequest.trim().isEmpty()) {
            scope = pullRequestScope(pullRequest);
        } else if (branch != null && !branch.trim().isEmpty()) {
            scope = branchScope(branch);
        } else {
            scope = DEFAULT_SCOPE;
        }
        return prefixOf(projectKey) + scope + "/" + FILE_NAME;
    }

    /** The scope segment a report of {@code branch} is stored under, for example {@code br-main}. */
    public static String branchScope(String branch) {
        return BRANCH_PREFIX + encode(branch);
    }

    /** The scope segment a report of pull request {@code pullRequest} is stored under, e.g. {@code pr-42}. */
    public static String pullRequestScope(String pullRequest) {
        return PULL_REQUEST_PREFIX + encode(pullRequest);
    }

    /**
     * Percent-encodes the UTF-8 bytes of {@code value}, keeping only {@code A-Za-z0-9}, {@code -}
     * and {@code _} literal. The empty string encodes to a single {@code "%"}, which no non-empty
     * input can produce because a real encoding always carries two hex digits behind every
     * {@code '%'}.
     */
    public static String encode(String value) {
        if (value.isEmpty()) {
            return "%";
        }
        byte[] bytes = value.getBytes(StandardCharsets.UTF_8);
        StringBuilder encoded = new StringBuilder(bytes.length);
        for (byte b : bytes) {
            int unsigned = b & 0xFF;
            if (isLiteral(unsigned)) {
                encoded.append((char) unsigned);
            } else {
                encoded.append('%').append(HEX[unsigned >> 4]).append(HEX[unsigned & 0x0F]);
            }
        }
        return encoded.toString();
    }

    private static boolean isLiteral(int b) {
        return (b >= 'A' && b <= 'Z') || (b >= 'a' && b <= 'z') || (b >= '0' && b <= '9') || b == '-' || b == '_';
    }

    /**
     * The inverse of {@link #encode(String)}. A single {@code "%"} decodes to the empty string,
     * every other {@code '%'} must be followed by exactly two hex digits, and every remaining
     * character must be one {@link #encode(String)} keeps literal. Anything else is malformed
     * input, not a value {@link #encode(String)} could ever have produced, and is rejected with an
     * {@link IllegalArgumentException} rather than mangled.
     *
     * @throws IllegalArgumentException if {@code encoded} is not an output of {@link #encode(String)}
     */
    public static String decode(String encoded) {
        if ("%".equals(encoded)) {
            return "";
        }
        ByteArrayOutputStream decoded = new ByteArrayOutputStream(encoded.length());
        int i = 0;
        while (i < encoded.length()) {
            char c = encoded.charAt(i);
            if (c == '%') {
                if (i + 2 >= encoded.length()) {
                    throw new IllegalArgumentException("Malformed percent-encoding in \"" + encoded + "\" at index " + i);
                }
                int high = Character.digit(encoded.charAt(i + 1), 16);
                int low = Character.digit(encoded.charAt(i + 2), 16);
                if (high < 0 || low < 0) {
                    throw new IllegalArgumentException("Malformed percent-encoding in \"" + encoded + "\" at index " + i);
                }
                decoded.write((high << 4) | low);
                i += 3;
            } else {
                if (!isLiteral(c)) {
                    // encode() emits only A-Za-z0-9, '-', '_' and percent escapes. Anything else
                    // is not something it produced, and writing it into a ByteArrayOutputStream
                    // would truncate it above U+00FF - silently mangling the value instead of
                    // saying that this is not a key of ours.
                    throw new IllegalArgumentException("Unencoded character '" + c + "' in \"" + encoded
                            + "\" at index " + i);
                }
                // Every literal character encode() emits is a single ASCII byte.
                decoded.write(c);
                i += 1;
            }
        }
        return new String(decoded.toByteArray(), StandardCharsets.UTF_8);
    }

    /**
     * Whether {@code storeKey} is a key this class could have produced: exactly three segments, a
     * non-empty project segment, a scope segment that is either the default scope or carries the
     * branch or pull request prefix, and the fixed {@link #FILE_NAME} as the last segment.
     *
     * <p>Recognition has to be strict, not merely structural. The store directory may hold files
     * that have nothing to do with this plugin - {@code logs/2026-09/sonar.log} also has three
     * segments - and a cleanup that mistakes such a file for a report deletes a stranger's data.
     * Every segment therefore has to be spelled the way {@link #encode(String)} spells it, which
     * rules out a directory named {@code münchen} just as much as one named {@code logs}.
     */
    public static boolean isReportKey(String storeKey) {
        return reportKeySegments(storeKey).isPresent();
    }

    /**
     * The decoded project key of a full store key such as
     * {@code com%2Eacme%3Aweb/br-main/dependency-check-report.html}, or an empty optional if the
     * key is not a report key - see {@link #isReportKey(String)}.
     */
    public static Optional<String> projectKeyOf(String storeKey) {
        return reportKeySegments(storeKey).flatMap(segments -> {
            try {
                String projectKey = decode(segments[0]);
                // A segment of exactly "%" decodes to the empty string and encodes back to "%", so
                // a directory literally named "%" that happens to hold a file of the expected name
                // passes every structural check - and would be handed to the cleanup as a report of
                // the project with the empty key. No project has an empty key, so this is not a key
                // of ours: it is skipped rather than reconciled.
                return projectKey.isEmpty() ? Optional.empty() : Optional.of(projectKey);
            } catch (IllegalArgumentException e) {
                return Optional.empty();
            }
        });
    }

    /**
     * The raw, still encoded scope segment of a full store key, for example {@code br-main}, or an
     * empty optional if the key is not a report key - see {@link #isReportKey(String)}.
     */
    public static Optional<String> scopeOf(String storeKey) {
        return reportKeySegments(storeKey).map(segments -> segments[1]);
    }

    /** The prefix under which every report of the given project is stored. */
    public static String prefixOf(String projectKey) {
        return encode(projectKey) + "/";
    }

    private static Optional<String[]> reportKeySegments(String storeKey) {
        String[] segments = storeKey.split("/", -1);
        if (segments.length != 3 || segments[0].isEmpty() || !FILE_NAME.equals(segments[2])
                || !isEncoded(segments[0]) || !isScope(segments[1])) {
            return Optional.empty();
        }
        return Optional.of(segments);
    }

    /**
     * Whether {@code segment} is exactly what {@link #encode(String)} produces for its own decoded
     * value. Structure alone is not enough: a directory literally named {@code münchen} has the
     * shape of a project segment, and a cleanup that accepts it deletes a stranger's file.
     */
    private static boolean isEncoded(String segment) {
        try {
            return encode(decode(segment)).equals(segment);
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    /** Whether {@code scope} is a scope segment {@link #of} could have produced. */
    private static boolean isScope(String scope) {
        if (DEFAULT_SCOPE.equals(scope)) {
            return true;
        }
        if (scope.startsWith(BRANCH_PREFIX) && scope.length() > BRANCH_PREFIX.length()) {
            return isEncoded(scope.substring(BRANCH_PREFIX.length()));
        }
        if (scope.startsWith(PULL_REQUEST_PREFIX) && scope.length() > PULL_REQUEST_PREFIX.length()) {
            return isEncoded(scope.substring(PULL_REQUEST_PREFIX.length()));
        }
        return false;
    }
}
