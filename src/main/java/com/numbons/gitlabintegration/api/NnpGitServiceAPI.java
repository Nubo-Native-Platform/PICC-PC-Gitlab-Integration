package com.numbons.gitlabintegration.api;

import java.util.List;

import com.numbons.gitlabintegration.service.NnpGitLabService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.numbons.gitlabintegration.api.model.GitAccessToken;
import com.numbons.gitlabintegration.api.model.User;
import com.numbons.gitlabintegration.exception.IntegrationException;
import com.numbons.gitlabintegration.exception.IntegrationExceptionMessage;

import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping(path = "/nnp/api")
@Slf4j
@Tag(name = "NNP Scoped Operations", description = "Scoped GitLab operations and Personal Access Token management for NNP environments")
public class NnpGitServiceAPI {

    @Autowired
    private NnpGitLabService nnpGitLabService;

    @Operation(summary = "Get or Rotate Scoped NNP PAT", description = "Retrieves or generates an active Personal Access Token for a given user ID and target environment slug.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Personal Access Token successfully retrieved or rotated"),
            @ApiResponse(responseCode = "500", description = "User resolution failure or token generation error")
    })
    @GetMapping(path = {"/getNNPGitLabPAT/{userId}/{env}"})
    public ResponseEntity<String> getTokenForIngPullsecret(
            @Parameter(description = "Target user ID or username", example = "devuser", required = true) @PathVariable String userId,
            @Parameter(description = "Target environment identifier", example = "prod", required = true) @PathVariable String env) {
        GitAccessToken token;
        List<User> users = nnpGitLabService.getUser(userId).getBody();
        if (users == null) {
            IntegrationExceptionMessage iem = new IntegrationExceptionMessage();
            iem.setCode(500);
            iem.setMessage("Should present a user against username - " + userId);
            throw new IntegrationException(iem);
        }
        if (users.size() > 1) {
            IntegrationExceptionMessage iem = new IntegrationExceptionMessage();
            iem.setCode(500);
            iem.setMessage("Ony one user should be available against a userId. But received more !! " + userId);
            throw new IntegrationException(iem);
        }
        List<GitAccessToken> gitAccTokens = nnpGitLabService.getToken(users.get(0), env).getBody();
        if (gitAccTokens == null || gitAccTokens.isEmpty()) {
            token = nnpGitLabService.createToken(users.get(0), env).getBody();
        } else {
            token = gitAccTokens.get(0);
            nnpGitLabService.revokeToken(token.getId());
            token = nnpGitLabService.createToken(users.get(0), env).getBody();
        }
        return ResponseEntity.ok(token.getToken());
    }

}
