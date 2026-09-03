package com.numbons.gitlabintegration.api.model;

import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class MemberGroupAssociation {
    private String id;
    private String userId;
    private String accessLevel;
}
