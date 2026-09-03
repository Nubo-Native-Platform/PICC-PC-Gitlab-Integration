
package com.numbons.gitlabintegration.api.model;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

import lombok.Getter;
import lombok.Setter;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({
	"id",
	"name",
	"revoked",
	"created_at",
	"description",
	"scopes",
	"user_id",
	"active",
	"expires_at",
	"token"
})
@Setter
@Getter
public class GitAccessToken {
    @JsonProperty("id")
    private Integer id;
    @JsonProperty("name")
    private String name;
    @JsonProperty("revoked")
    private Boolean revoked;
    @JsonProperty("created_at")
    private String createdAt;
    @JsonProperty("description")
    private String description;
    @JsonProperty("scopes")
    private List<String> scopes;
    @JsonProperty("user_id")
    private Integer userId;
    @JsonProperty("active")
    private Boolean active;
    @JsonProperty("expires_at")
    private String expiresAt;
    @JsonProperty("token")
    private String token;

}
