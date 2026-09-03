package com.numbons.gitlabintegration.api.model;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@Schema(description = "Multi-tier environment creation and orchestration payload")
public class GitEnvironment {

    @Schema(description = "User email address", example = "operator@example.com", requiredMode = Schema.RequiredMode.REQUIRED)
    private String email;

    @Schema(description = "User display name", example = "Platform Operator", requiredMode = Schema.RequiredMode.REQUIRED)
    private String name;

    @Schema(description = "Root environment group name", example = "staging-us-east", requiredMode = Schema.RequiredMode.REQUIRED)
    private String envName;

    @Schema(description = "Root environment group path slug", example = "staging-us-east", requiredMode = Schema.RequiredMode.REQUIRED)
    private String envPath;

    @Schema(description = "User login username", example = "platform_operator", requiredMode = Schema.RequiredMode.REQUIRED)
    private String userName;

    @Schema(description = "User password", example = "SecurePassword#2026", requiredMode = Schema.RequiredMode.REQUIRED)
    private String password;
}
