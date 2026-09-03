package com.numbons.gitlabintegration.api.model;

import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class GitUser {
    private String email;
    private String name;
    private String userName;
    private String groupName;
    private String password;
    private String userType;
}
