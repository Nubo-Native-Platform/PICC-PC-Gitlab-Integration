package com.numbons.gitlabintegration.exception;

import com.numbons.gitlabintegration.api.exception.GitLabAPIException;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import lombok.extern.slf4j.Slf4j;

@Order(Ordered.HIGHEST_PRECEDENCE)
@ControllerAdvice
@Slf4j
public class IntegrationExceptionHandler extends ResponseEntityExceptionHandler {

    @ExceptionHandler(IntegrationException.class)
    public ResponseEntity<Object> handleIntegrationException(IntegrationException exception) {
        log.error("IntegrationException occurred: message=[{}], code=[{}]",
                exception.getMessage(),
                exception.getIntegrationExceptionMessage() != null ? exception.getIntegrationExceptionMessage().getCode() : 500,
                exception);

        IntegrationExceptionMessage message = exception.getIntegrationExceptionMessage();
        if (message == null) {
            message = new IntegrationExceptionMessage();
            message.setCode(500);
            message.setMessage(exception.getMessage());
        }
        return new ResponseEntity<>(message, HttpStatusCode.valueOf(message.getCode()));
    }

    @ExceptionHandler(GitLabAPIException.class)
    public ResponseEntity<Object> handleGitLabAPIException(GitLabAPIException exception) {
        log.error("GitLabAPIException occurred: {}", exception.getMessage(), exception);

        // Previously this always returned 502 Bad Gateway regardless of what
        // GitLab actually said (e.g. 409 Conflict for "email already taken"),
        // which made the real reason invisible to callers and made every
        // GitLab-side validation failure look like an infrastructure outage.
        // GitLabAPIException now carries the real upstream status — use it.
        HttpStatus status = exception.getStatus() != null ? exception.getStatus() : HttpStatus.BAD_GATEWAY;

        IntegrationExceptionMessage message = new IntegrationExceptionMessage();
        message.setCode(status.value());
        message.setMessage("GitLab API Error: " + exception.getMessage());
        return new ResponseEntity<>(message, status);
    }

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<Object> handleResponseStatusException(ResponseStatusException exception) {
        log.error("ResponseStatusException occurred: status=[{}], reason=[{}]",
                exception.getStatusCode(), exception.getReason(), exception);

        IntegrationExceptionMessage message = new IntegrationExceptionMessage();
        message.setCode(exception.getStatusCode().value());
        message.setMessage(exception.getMessage());
        return new ResponseEntity<>(message, exception.getStatusCode());
    }

    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<Object> handleRuntimeException(RuntimeException exception) {
        log.error("Unhandled RuntimeException occurred: {}", exception.getMessage(), exception);

        IntegrationExceptionMessage message = new IntegrationExceptionMessage();
        message.setCode(500);
        message.setMessage(exception.getMessage());
        return new ResponseEntity<>(message, HttpStatus.INTERNAL_SERVER_ERROR);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Object> handleGeneralException(Exception exception) {
        log.error("Unhandled Exception occurred: {}", exception.getMessage(), exception);

        IntegrationExceptionMessage message = new IntegrationExceptionMessage();
        message.setCode(500);
        message.setMessage(exception.getMessage());
        return new ResponseEntity<>(message, HttpStatus.INTERNAL_SERVER_ERROR);
    }
}