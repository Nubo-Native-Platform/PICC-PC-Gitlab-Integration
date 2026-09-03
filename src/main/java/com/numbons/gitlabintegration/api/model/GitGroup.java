package com.numbons.gitlabintegration.api.model;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class GitGroup {
    private String name;
    private String path;
    //@JsonProperty("project_creation_level")
    private String canCreateProject;
    //@JsonProperty("subgroup_creation_level")
    private String canCreateSubgrp;
    @JsonProperty("parent_id")
    private int id;
}
