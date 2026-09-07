package com.numbons.gitlabintegration.api.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
@Schema(description = "GitLab group and subgroup creation payload")
public class GitGroup {
    @Schema(description = "Group display name", example = "Core Services", requiredMode = Schema.RequiredMode.REQUIRED)
    private String name;

    @Schema(description = "URL-safe group path slug (defaults to lowercased name)", example = "core-services")
    private String path;

    @Schema(description = "Role permitted to create projects in this group (noone, maintainer, developer, owner)", example = "owner")
    private String canCreateProject;

    @Schema(description = "Role permitted to create subgroups (owner, maintainer)", example = "maintainer")
    private String canCreateSubgrp;

    @JsonProperty("parent_id")
    @Schema(description = "Parent group ID for nested subgroups (0 for top-level groups)", example = "105")
    private int id;
}
