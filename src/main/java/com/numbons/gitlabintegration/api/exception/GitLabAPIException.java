package com.numbons.gitlabintegration.api.exception;

import lombok.Getter;
import lombok.Setter;
import org.springframework.http.HttpStatus;

/**
 * Wraps a failure from a call to the real GitLab API.
 *
 * Previously this carried only a message string, with no way to recover the
 * original HTTP status GitLab actually returned (e.g. 409 Conflict for a
 * duplicate email, 404 Not Found, 400 Bad Request). Because of that,
 * IntegrationExceptionHandler had no choice but to report every
 * GitLabAPIException as a generic 502 Bad Gateway to callers, discarding an
 * actionable status/reason (like "409 — email already taken") that callers
 * could otherwise have handled directly instead of tearing down and rolling
 * back a whole user-provisioning flow.
 *
 * The {@code status} field preserves that original status so it can be
 * propagated instead of being flattened to 502.
 */
@Getter
@Setter
public class GitLabAPIException extends RuntimeException {

    private final HttpStatus status;

    public GitLabAPIException(String s) {
        this(s, HttpStatus.BAD_GATEWAY);
    }

    public GitLabAPIException(String s, Exception ex) {
        this(s, HttpStatus.BAD_GATEWAY, ex);
    }

    public GitLabAPIException(String s, HttpStatus status) {
        super(s);
        this.status = status;
    }

    public GitLabAPIException(String s, HttpStatus status, Exception ex) {
        super(s, ex);
        this.status = status;
    }

}