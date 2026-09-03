package com.numbons.gitlabintegration.api.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({
        "id",
        "web_url",
        "name",
        "path",
        "description",
        "visibility",
        "share_with_group_lock",
        "require_two_factor_authentication",
        "two_factor_grace_period",
        "project_creation_level",
        "auto_devops_enabled",
        "subgroup_creation_level",
        "emails_disabled",
        "mentions_disabled",
        "lfs_enabled",
        "default_branch_protection",
        "avatar_url",
        "request_access_enabled",
        "full_name",
        "full_path",
        "created_at",
        "parent_id",
        "shared_with_groups",
        "prevent_sharing_groups_outside_hierarchy",
        "projects",
        "shared_projects"
})
@Setter
@Getter
public class Group {
    @JsonProperty("id")
    public Integer id;
    @JsonProperty("web_url")
    public String webUrl;
    @JsonProperty("name")
    public String name;
    @JsonProperty("path")
    public String path;
    @JsonProperty("description")
    public String description;
    @JsonProperty("visibility")
    public String visibility;
    @JsonProperty("share_with_group_lock")
    public Boolean shareWithGroupLock;
    @JsonProperty("require_two_factor_authentication")
    public Boolean requireTwoFactorAuthentication;
    @JsonProperty("two_factor_grace_period")
    public Integer twoFactorGracePeriod;
    @JsonProperty("project_creation_level")
    public String projectCreationLevel;
    @JsonProperty("auto_devops_enabled")
    public Object autoDevopsEnabled;
    @JsonProperty("subgroup_creation_level")
    public String subgroupCreationLevel;
    @JsonProperty("emails_disabled")
    public Object emailsDisabled;
    @JsonProperty("mentions_disabled")
    public Object mentionsDisabled;
    @JsonProperty("lfs_enabled")
    public Boolean lfsEnabled;
    @JsonProperty("default_branch_protection")
    public Integer defaultBranchProtection;
    @JsonProperty("avatar_url")
    public Object avatarUrl;
    @JsonProperty("request_access_enabled")
    public Boolean requestAccessEnabled;
    @JsonProperty("full_name")
    public String fullName;
    @JsonProperty("full_path")
    public String fullPath;
    @JsonProperty("created_at")
    public String createdAt;
    @JsonProperty("parent_id")
    public Object parentId;
    @JsonProperty("shared_with_groups")
    public List<Object> sharedWithGroups = null;
    @JsonProperty("prevent_sharing_groups_outside_hierarchy")
    public Boolean preventSharingGroupsOutsideHierarchy;
    @JsonProperty("projects")
    public List<Object> projects = null;
    @JsonProperty("shared_projects")
    public List<Object> sharedProjects = null;
}
