package com.numbons.gitlabintegration.api.model;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
@Schema(description = "GitLab user creation payload")
public class GitUser {
    @Schema(description = "User email address", example = "developer@example.com", requiredMode = Schema.RequiredMode.REQUIRED)
    private String email;

    @Schema(description = "User display name", example = "Jane Doe", requiredMode = Schema.RequiredMode.REQUIRED)
    private String name;

    @Schema(description = "User login username", example = "janedoe", requiredMode = Schema.RequiredMode.REQUIRED)
    private String userName;

    @Schema(description = "Associated group name for initial membership", example = "engineering-team")
    private String groupName;

    @Schema(description = "Initial user password", example = "SecurePassword#2026", requiredMode = Schema.RequiredMode.REQUIRED)
    private String password;

    @Schema(description = "User role type (e.g., admin, superAdmin, developer)", example = "developer")
    private String userType;
}
