package com.numbons.gitlabintegration.api.exception;

import lombok.Getter;
import lombok.Setter;

/**
 * Mirrors the error body GitLab's real API returns.
 *
 * GitLab does not use one consistent shape for {@code message}:
 *   - Plain conflict/not-found style errors send a string, e.g.
 *     {@code {"message": "Email has already been taken"}}
 *   - Field-validation errors (e.g. password policy) send an object of
 *     field name -> list of error strings, e.g.
 *     {@code {"message": {"password": ["must not contain ..."]}}}
 *
 * {@code message} was previously typed as {@code String}, so the second
 * shape failed Jackson deserialization (MismatchedInputException) on every
 * validation-style error. The decoder caught that failure and fell back to
 * the raw response body, so no information was ultimately lost, but every
 * such error logged a spurious ERROR-level stack trace that had nothing to
 * do with the actual problem — noise that sends whoever's debugging it
 * chasing a parsing bug instead of the real "your password doesn't meet
 * GitLab's policy" issue.
 *
 * Typing this as {@code Object} lets Jackson deserialize either shape
 * (String, or a Map<String, List<String>> for field errors) without error;
 * see GitLabClientErrorDecoder#extractDetailMessage for how each shape is
 * turned into a human-readable string.
 */
@Setter
@Getter
public class GitLabAPIExceptionMessage {
    private String timestamp;
    private int status;
    private String error;
    private Object message;
    private String path;
}