package com.numbons.gitlabintegration.api.model;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class GitProject {
	private String name;
	private String path;
	@JsonProperty("namespace_id")
	private int namespaceId;
	private String visibility;
	@JsonProperty("initialize_with_readme")
	private boolean isInitReadMe;
}
