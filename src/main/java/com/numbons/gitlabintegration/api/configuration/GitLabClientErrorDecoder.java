package com.numbons.gitlabintegration.api.configuration;

import com.numbons.gitlabintegration.api.exception.GitLabAPIException;
import com.numbons.gitlabintegration.api.exception.GitLabAPIExceptionMessage;
import com.fasterxml.jackson.databind.ObjectMapper;
import feign.Response;
import feign.Util;
import feign.codec.ErrorDecoder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
public class GitLabClientErrorDecoder implements ErrorDecoder {
    private final ErrorDecoder errorDecoder = new Default();

    @Override
    public Exception decode(String methodKey, Response response) {
        String bodyString = "";
        GitLabAPIExceptionMessage message = null;

        if (response.body() != null) {
            try {
                bodyString = Util.toString(response.body().asReader(StandardCharsets.UTF_8));
                if (bodyString != null && !bodyString.isBlank()) {
                    ObjectMapper mapper = new ObjectMapper();
                    message = mapper.readValue(bodyString, GitLabAPIExceptionMessage.class);
                }
            } catch (IOException e) {
                log.error("Failed to parse GitLab error response body for methodKey [{}]: {}", methodKey, e.getMessage(), e);
            }
        }

        String requestUrl = (response.request() != null) ? response.request().url() : "N/A";
        String requestMethod = (response.request() != null) ? response.request().httpMethod().name() : "N/A";
        int status = response.status();

        log.error("GitLab API Error: MethodKey=[{}], HTTP Method=[{}], Request URL=[{}], Status=[{}], Response Body=[{}]",
                methodKey, requestMethod, requestUrl, status, bodyString);

        String formattedMessage = (message != null) ? formatMessage(message.getMessage()) : null;
        String detailMsg = (formattedMessage != null && !formattedMessage.isBlank())
                ? formattedMessage
                : (!bodyString.isBlank() ? bodyString : "HTTP " + status + " error from GitLab");

        return switch (status) {
            case 400 -> new GitLabAPIException("Bad Request: " + detailMsg, HttpStatus.BAD_REQUEST);
            case 404 -> new GitLabAPIException("Not Found: " + detailMsg, HttpStatus.NOT_FOUND);
            case 409 -> new GitLabAPIException("Conflict: " + detailMsg, HttpStatus.CONFLICT);
            default -> {
                Exception defaultEx = errorDecoder.decode(methodKey, response);
                log.error("Default Feign decoder exception for methodKey [{}]:", methodKey, defaultEx);
                // Unrecognized upstream status: no safe specific mapping, so this
                // genuinely is treated as an opaque gateway failure — but note
                // that's now the exception, not the default for every GitLab error.
                yield new GitLabAPIException("GitLab Call Failed (" + status + "): " + detailMsg, HttpStatus.BAD_GATEWAY, defaultEx);
            }
        };
    }

    /**
     * Renders GitLab's error {@code message} field as readable text,
     * regardless of which of the two shapes GitLab actually sent:
     *   - a plain string, e.g. "Email has already been taken"
     *   - a field-validation object, e.g. {"password": ["must not contain
     *     commonly used combinations of words and letters"]}
     *
     * Jackson deserializes the latter into a Map<String, Object> (values
     * typically Lists of strings); this flattens that into
     * "field: error1, error2; field2: error3" so the real per-field reason
     * reaches logs and callers instead of a raw, unparsed JSON blob.
     */
    private String formatMessage(Object rawMessage) {
        return switch (rawMessage) {
            case null -> null;
            case String s -> s;
            case Map<?, ?> fieldErrors -> fieldErrors.entrySet().stream()
                    .map(entry -> {
                        Object value = entry.getValue();
                        String joined = (value instanceof Collection<?> values)
                                ? values.stream().map(String::valueOf).collect(Collectors.joining(", "))
                                : String.valueOf(value);
                        return entry.getKey() + ": " + joined;
                    })
                    .collect(Collectors.joining("; "));
            default ->
                // Any other shape GitLab might send in the future — don't lose it.
                    String.valueOf(rawMessage);
        };
    }
}