package com.numbons.gitlabintegration.api.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
@Schema(description = "GitLab project/repository creation payload")
public class GitProject {
    @Schema(description = "Repository name", example = "order-service", requiredMode = Schema.RequiredMode.REQUIRED)
    private String name;

    @Schema(description = "Repository URL-safe path slug (defaults to lowercased name)", example = "order-service")
    private String path;

    @JsonProperty("namespace_id")
    @Schema(description = "Target group or user namespace ID", example = "42", requiredMode = Schema.RequiredMode.REQUIRED)
    private int namespaceId;

    @Schema(description = "Repository visibility level (private, internal, public)", example = "private")
    private String visibility;

    @JsonProperty("initialize_with_readme")
    @Schema(description = "Whether to initialize repository with a default README.md", example = "true")
    private boolean isInitReadMe;
}
