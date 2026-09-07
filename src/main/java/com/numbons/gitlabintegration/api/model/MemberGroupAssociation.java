package com.numbons.gitlabintegration.api.model;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
@Schema(description = "Member-to-group membership association model")
public class MemberGroupAssociation {
    @Schema(description = "Target group ID", example = "105", requiredMode = Schema.RequiredMode.REQUIRED)
    private String id;

    @Schema(description = "Target user ID", example = "42", requiredMode = Schema.RequiredMode.REQUIRED)
    private String userId;

    @Schema(description = "GitLab access level: 10 (Guest), 20 (Reporter), 30 (Developer), 40 (Maintainer), 50 (Owner)", example = "30", requiredMode = Schema.RequiredMode.REQUIRED)
    private String accessLevel;
}
