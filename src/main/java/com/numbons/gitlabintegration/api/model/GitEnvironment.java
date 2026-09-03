package com.numbons.gitlabintegration.api.model;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class GitEnvironment {
	
	private String email;
	private String name ;
    private String envName;  //group name
    private String envPath;  //group path
    private String userName;
    private String password;

}
