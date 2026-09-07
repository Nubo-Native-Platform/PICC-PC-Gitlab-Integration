package com.numbons.gitlabintegration.api;

import com.numbons.gitlabintegration.api.exception.GitLabAPIException;
import com.numbons.gitlabintegration.api.model.GitAccessToken;
import com.numbons.gitlabintegration.api.model.GitEnvironment;
import com.numbons.gitlabintegration.api.model.GitGroup;
import com.numbons.gitlabintegration.api.model.GitProject;
import com.numbons.gitlabintegration.api.model.GitUser;
import com.numbons.gitlabintegration.api.model.Group;
import com.numbons.gitlabintegration.api.model.MemberGroupAssociation;
import com.numbons.gitlabintegration.api.model.Project;
import com.numbons.gitlabintegration.api.model.User;
import com.numbons.gitlabintegration.events.CacheRefreshEvent;
import com.numbons.gitlabintegration.exception.IntegrationException;
import com.numbons.gitlabintegration.exception.IntegrationExceptionMessage;
import com.numbons.gitlabintegration.service.CacheService;
import com.numbons.gitlabintegration.service.GitLabService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.ApplicationEventMulticaster;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import static com.numbons.gitlabintegration.utils.LogUtils.sanitizeForLog;

@RestController
@RequestMapping(path = "/api")
@Slf4j
@Tag(name = "GitLab Integration", description = "Core REST API endpoints for GitLab users, groups, projects, PAT tokens, and environment orchestration")
public class GitServicesAPI {
        @Autowired
        private GitLabService gitLabService;

        @Autowired
        private CacheService cacheService;

        @Autowired
        private ApplicationEventMulticaster applicationEventMulticaster;

        @Operation(summary = "Create GitLab User", description = "Provisions a new GitLab user with standard security constraints (projects_limit=0, external=true, can_create_group=false)")
        @ApiResponses(value = {
                @ApiResponse(responseCode = "200", description = "User created successfully"),
                @ApiResponse(responseCode = "400", description = "Invalid request payload"),
                @ApiResponse(responseCode = "409", description = "Username or email already exists")
        })
        @PostMapping(path = "/createUser")
        public ResponseEntity<User> createUser(@RequestBody GitUser gitUser) {
                return gitLabService.createUser(gitUser);
        }

        @Operation(summary = "Get User by Username", description = "Retrieves user details from GitLab for a given username")
        @ApiResponses(value = {
                @ApiResponse(responseCode = "200", description = "User retrieved successfully"),
                @ApiResponse(responseCode = "417", description = "User not found")
        })
        @GetMapping(path = "/getUser")
        public ResponseEntity<User> getUser(
                @Parameter(description = "GitLab username", example = "janedoe", required = true) @RequestParam String username) {
                List<User> users = gitLabService.getUser(username).getBody();
                if (users != null && !users.isEmpty()) {
                        return new ResponseEntity<>(users.get(0), HttpStatus.OK);
                } else {
                        return new ResponseEntity<>(new User(), HttpStatus.EXPECTATION_FAILED);
                }
        }

        @Operation(summary = "Delete User", description = "Permanently deletes a GitLab user account by user ID")
        @ApiResponses(value = {
                @ApiResponse(responseCode = "200", description = "User deleted successfully"),
                @ApiResponse(responseCode = "404", description = "User ID not found")
        })
        @DeleteMapping(path = "/deleteUser")
        public ResponseEntity<String> deleteUser(
                @Parameter(description = "GitLab numerical user ID", example = "42", required = true) @RequestParam Integer id) {
                return gitLabService.deleteUser(id);
        }

        @Operation(summary = "Create Group or Subgroup", description = "Creates a top-level group or subgroup under a specified parent ID")
        @ApiResponses(value = {
                @ApiResponse(responseCode = "200", description = "Group created successfully"),
                @ApiResponse(responseCode = "201", description = "Group created successfully"),
                @ApiResponse(responseCode = "409", description = "Group name or path already exists")
        })
        @PostMapping(path = "/createGroups")
        public ResponseEntity<Group> createGroup(@RequestBody GitGroup gitGroup) {
                return gitLabService.createGroup(gitGroup);
        }

        @Operation(summary = "Delete Group", description = "Deletes a GitLab group or subgroup by ID")
        @ApiResponses(value = {
                @ApiResponse(responseCode = "200", description = "Group deleted successfully"),
                @ApiResponse(responseCode = "404", description = "Group ID not found")
        })
        @DeleteMapping(path = "/deleteGroup/")
        public ResponseEntity<String> deleteGroup(
                @Parameter(description = "GitLab group ID", example = "105", required = true) @RequestParam String id) {
                return gitLabService.deleteGroup(id);
        }

        @Operation(summary = "Associate User with Group", description = "Assigns a user to a group with a specific access role (10=Guest, 20=Reporter, 30=Developer, 40=Maintainer, 50=Owner)")
        @ApiResponses(value = {
                @ApiResponse(responseCode = "200", description = "User associated with group successfully"),
                @ApiResponse(responseCode = "400", description = "Invalid group ID or user ID")
        })
        @PostMapping(value = "/associateUser")
        public ResponseEntity<String> associateUserWithGroup(MemberGroupAssociation memberGroupAssociation) {
                return gitLabService.associateUserWithGroup(memberGroupAssociation);
        }

        @Operation(summary = "Get Group Members", description = "Retrieves member-group relations and assigned roles for a given group ID")
        @ApiResponses(value = {
                @ApiResponse(responseCode = "200", description = "Member relations retrieved successfully")
        })
        @GetMapping(value = "/getListOfMembers/{groupId}")
        public ResponseEntity<String> getListOfMembersOfGroup(
                @Parameter(description = "GitLab group ID", example = "105", required = true) @PathVariable String groupId) {
                return gitLabService.getUserGroupRelations(groupId);
        }

        @Operation(summary = "Onboard User to Group (v1)", description = "Provisions a new user, resolves the target group by name, and assigns Developer or Maintainer access based on user role")
        @ApiResponses(value = {
                @ApiResponse(responseCode = "200", description = "User onboarded and associated successfully")
        })
        @PostMapping(value = "/v1/onboard/user")
        public ResponseEntity<String> onboardV1(@RequestBody GitUser gitUser) {
                String userType = gitUser.getUserType();

                // create a user
                ResponseEntity<User> respEntityUser = gitLabService.createUser(gitUser);
                User user = respEntityUser.getBody();

                // Get group by name
                Group group = gitLabService.getGroupByGroupName(gitUser.getGroupName()).getBody();

                // assign user to group
                MemberGroupAssociation memberGrAssociation = new MemberGroupAssociation();
                memberGrAssociation.setId(String.valueOf(group.getId()));
                memberGrAssociation.setUserId(String.valueOf(user.getId()));
                if ("superAdmin".equals(userType) || "admin".equals(userType)) {
                        memberGrAssociation.setAccessLevel("40"); // Maintainer access
                } else {
                        memberGrAssociation.setAccessLevel("30"); // Developer access
                }
                return gitLabService.associateUserWithGroup(memberGrAssociation);
        }

        @Operation(summary = "Get Groups Accessible by User", description = "Queries all top-level groups or subgroups accessible by a specific user ID with cache acceleration")
        @ApiResponses(value = {
                @ApiResponse(responseCode = "200", description = "Accessible groups retrieved successfully")
        })
        @GetMapping(path = { "/getGroups/{userId}", "/getSubGroups/{userId}/{groupName}" })
        public ResponseEntity<List<Group>> getGroupsByUser(
                @Parameter(description = "Username or user ID", example = "janedoe", required = true) @PathVariable String userId,
                @Parameter(description = "Parent group name filter (optional)", example = "engineering") @PathVariable(required = false) String groupName) {
                return gitLabService.getGroupsByUser(userId, groupName);
        }

        @Operation(summary = "Get All Top-Level Groups", description = "Retrieves all top-level GitLab groups")
        @ApiResponses(value = {
                @ApiResponse(responseCode = "200", description = "All top-level groups retrieved successfully")
        })
        @GetMapping(path = { "/getAllGroups" })
        public ResponseEntity<List<Group>> getAllGroups() {
                return gitLabService.getAllGroups();
        }

        @Operation(summary = "Check Group Existence", description = "Verifies whether a group exists in GitLab by name")
        @ApiResponses(value = {
                @ApiResponse(responseCode = "200", description = "Existence check result (true/false)")
        })
        @GetMapping(path = { "/exist/group/{groupName}" })
        public ResponseEntity<Boolean> isGroupExistsInGit(
                @Parameter(description = "Group name to check", example = "core-services", required = true) @PathVariable String groupName) {
                return gitLabService.isGroupExistsInGit(groupName);
        }

        @Operation(summary = "Get Group Details by Name", description = "Retrieves group entity by exact group name")
        @ApiResponses(value = {
                @ApiResponse(responseCode = "200", description = "Group details retrieved successfully")
        })
        @GetMapping(path = { "/getGroupDetails/{groupName}" })
        public ResponseEntity<Group> getGroupByGroupName(
                @Parameter(description = "Group name", example = "core-services", required = true) @PathVariable String groupName) {
                return gitLabService.getGroupByGroupName(groupName);
        }

        @Operation(summary = "Get or Rotate Personal Access Token", description = "Retrieves or rotates an active Personal Access Token for image pull secrets and CI/CD automation")
        @ApiResponses(value = {
                @ApiResponse(responseCode = "200", description = "PAT token string successfully retrieved or rotated"),
                @ApiResponse(responseCode = "500", description = "User resolution or token generation error")
        })
        @GetMapping(path = { "/getLcncGitPAT/{userId}" })
        public ResponseEntity<String> getTokenForIngPullsecret(
                @Parameter(description = "Username or user ID", example = "devuser", required = true) @PathVariable String userId) {
                GitAccessToken token;
                List<User> users = gitLabService.getUser(userId).getBody();
                if (users == null) {
                        IntegrationExceptionMessage iem = new IntegrationExceptionMessage();
                        iem.setCode(500);
                        iem.setMessage("Should present a user against username - " + userId);
                        throw new IntegrationException(iem);
                }
                if (users.size() > 1) {
                        IntegrationExceptionMessage iem = new IntegrationExceptionMessage();
                        iem.setCode(500);
                        iem.setMessage("Ony one user should be available against a userId. But received more !! " + userId);
                        throw new IntegrationException(iem);
                }
                List<GitAccessToken> gitAccTokens = gitLabService.getToken(users.get(0)).getBody();
                if (gitAccTokens == null || gitAccTokens.isEmpty()) {
                        token = gitLabService.createToken(users.get(0)).getBody();
                } else {
                        token = gitAccTokens.get(0);
                        gitLabService.revokeToken(token.getId());
                        token = gitLabService.createToken(users.get(0)).getBody();
                }
                return ResponseEntity.ok(token.getToken());
        }

        @Operation(summary = "Create Multi-Tier Environment", description = "Orchestrates end-to-end environment creation including root groups, subgroups (gitops, 3pp-comp, code), user provisioning, PAT generation, ArgoCD repos, and sync projects")
        @ApiResponses(value = {
                @ApiResponse(responseCode = "200", description = "Environment successfully orchestrated; returns generated PAT token string"),
                @ApiResponse(responseCode = "400", description = "Invalid environment payload"),
                @ApiResponse(responseCode = "500", description = "GitLab orchestration or configuration error")
        })
        @PostMapping(path = { "/createEnvironment" })
        public ResponseEntity<String> createEnvironment(
                        @RequestBody GitEnvironment gitEnvironment) throws IntegrationException {

                ResponseEntity<Group> groupRes = null;
                ResponseEntity<User> userRes = null;
                GitAccessToken accessToken = null;

                try {
                        /*
                         * Validate request
                         */
                        if (gitEnvironment == null) {
                                throw createIntegrationException(
                                                400,
                                                "Git environment request cannot be null");
                        }

                        /*
                         * 1. Create root group
                         */
                        GitGroup gitGroup = new GitGroup();
                        gitGroup.setName(gitEnvironment.getEnvName());
                        gitGroup.setPath(gitEnvironment.getEnvPath());
                        gitGroup.setCanCreateProject("owner");
                        gitGroup.setCanCreateSubgrp("maintainer");

                        groupRes = gitLabService.createGroup(gitGroup);

                        if (groupRes == null) {
                                throw createIntegrationException(
                                                500,
                                                "GitLab returned a null response while creating root group");
                        }

                        if (groupRes.getBody() == null) {
                                throw createIntegrationException(
                                                500,
                                                "GitLab returned an empty root group");
                        }

                        if (groupRes.getStatusCode() != HttpStatus.CREATED
                                        && groupRes.getStatusCode() != HttpStatus.OK) {

                                throw createIntegrationException(
                                                500,
                                                "Failed to create root GitLab group. Status: "
                                                                + groupRes.getStatusCode());
                        }

                        log.info("Root group created - {}", groupRes.getBody().getName());

                        /*
                         * 2. Create GitLab user
                         */
                        GitUser gitUser = new GitUser();
                        gitUser.setEmail(gitEnvironment.getEmail());
                        gitUser.setName(gitEnvironment.getName());
                        gitUser.setUserName(gitEnvironment.getUserName());
                        gitUser.setPassword(gitEnvironment.getPassword());

                        userRes = gitLabService.createUser(gitUser);

                        if (userRes == null) {
                                cleanUpActivity(groupRes, userRes);

                                throw createIntegrationException(
                                                500,
                                                "GitLab returned a null response while creating user");
                        }

                        if (userRes.getBody() == null) {
                                cleanUpActivity(groupRes, userRes);

                                throw createIntegrationException(
                                                500,
                                                "GitLab returned an empty user response");
                        }

                        if (userRes.getStatusCode() != HttpStatus.CREATED
                                        && userRes.getStatusCode() != HttpStatus.OK) {

                                cleanUpActivity(groupRes, userRes);

                                throw createIntegrationException(
                                                500,
                                                "Failed to create GitLab user. Status: "
                                                                + userRes.getStatusCode());
                        }

                        log.info("User created - {}", userRes.getBody().getName());

                        /*
                         * 3. Associate user with root group
                         */
                        MemberGroupAssociation memberGrAssociation = new MemberGroupAssociation();

                        memberGrAssociation.setId(
                                        String.valueOf(groupRes.getBody().getId()));

                        memberGrAssociation.setUserId(
                                        String.valueOf(userRes.getBody().getId()));

                        // Maintainer access
                        memberGrAssociation.setAccessLevel("40");

                        ResponseEntity<String> associationResp = gitLabService.associateUserWithGroup(
                                        memberGrAssociation);

                        if (associationResp == null) {
                                cleanUpActivity(groupRes, userRes);

                                throw createIntegrationException(
                                                500,
                                                "GitLab returned a null response while associating user with group");
                        }

                        if (!associationResp.getStatusCode().is2xxSuccessful()) {
                                cleanUpActivity(groupRes, userRes);

                                throw createIntegrationException(
                                                500,
                                                "Failed to associate GitLab user with group. Status: "
                                                                + associationResp.getStatusCode());
                        }

                        log.info(
                                        "User {} associated with group {}",
                                        userRes.getBody().getName(),
                                        groupRes.getBody().getName());

                        /*
                         * 4. Create access token
                         */
                        ResponseEntity<GitAccessToken> tokenResponse = gitLabService.createToken(userRes.getBody());

                        if (tokenResponse == null) {
                                cleanUpActivity(groupRes, userRes);

                                throw createIntegrationException(
                                                500,
                                                "GitLab returned a null response while creating access token");
                        }

                        if (tokenResponse.getBody() == null) {
                                cleanUpActivity(groupRes, userRes);

                                throw createIntegrationException(
                                                500,
                                                "GitLab returned an empty access token");
                        }

                        accessToken = tokenResponse.getBody();

                        if (accessToken.getToken() == null
                                        || accessToken.getToken().isBlank()) {

                                cleanUpActivity(groupRes, userRes);

                                throw createIntegrationException(
                                                500,
                                                "GitLab access token is empty");
                        }

                        /*
                         * 5. Create gitops subgroup
                         */
                        GitGroup gitGroupArgo = new GitGroup();
                        gitGroupArgo.setName("gitops");
                        gitGroupArgo.setPath("gitops");
                        gitGroupArgo.setCanCreateProject("owner");
                        gitGroupArgo.setCanCreateSubgrp("owner");
                        gitGroupArgo.setId(groupRes.getBody().getId());

                        ResponseEntity<Group> groupArgoRes = gitLabService.createGroup(gitGroupArgo);

                        if (groupArgoRes == null || groupArgoRes.getBody() == null) {
                                cleanUpActivity(groupRes, userRes);

                                throw createIntegrationException(
                                                500,
                                                "Failed to create gitops subgroup");
                        }

                        if (groupArgoRes.getStatusCode() != HttpStatus.CREATED
                                        && groupArgoRes.getStatusCode() != HttpStatus.OK) {

                                cleanUpActivity(groupRes, userRes);

                                throw createIntegrationException(
                                                500,
                                                "Failed to create gitops subgroup. Status: "
                                                                + groupArgoRes.getStatusCode());
                        }

                        log.info(
                                        "gitops-repo subgroup created - {}",
                                        groupArgoRes.getBody().getName());

                        /*
                         * 6. Create 3pp-comp subgroup
                         */
                        GitGroup gitGroupComp = new GitGroup();
                        gitGroupComp.setName("3pp-comp");
                        gitGroupComp.setPath("3pp-comp");
                        gitGroupComp.setCanCreateProject("maintainer");
                        gitGroupComp.setCanCreateSubgrp("owner");
                        gitGroupComp.setId(groupRes.getBody().getId());

                        ResponseEntity<Group> groupCompRes = gitLabService.createGroup(gitGroupComp);

                        if (groupCompRes == null || groupCompRes.getBody() == null) {
                                cleanUpActivity(groupRes, userRes);

                                throw createIntegrationException(
                                                500,
                                                "Failed to create 3pp-comp subgroup");
                        }

                        if (groupCompRes.getStatusCode() != HttpStatus.CREATED
                                        && groupCompRes.getStatusCode() != HttpStatus.OK) {

                                cleanUpActivity(groupRes, userRes);

                                throw createIntegrationException(
                                                500,
                                                "Failed to create 3pp-comp subgroup. Status: "
                                                                + groupCompRes.getStatusCode());
                        }

                        log.info(
                                        "img-component subgroup created - {}",
                                        groupCompRes.getBody().getName());

                        /*
                         * 7. Create application subgroup
                         */
                        GitGroup gitGroupApp = new GitGroup();

                        gitGroupApp.setName(
                                        gitEnvironment.getEnvName() + "-code");

                        gitGroupApp.setPath(
                                        gitEnvironment.getEnvName() + "-code");

                        gitGroupApp.setCanCreateSubgrp("owner");
                        gitGroupApp.setId(groupRes.getBody().getId());

                        ResponseEntity<Group> groupAppRes = gitLabService.createGroup(gitGroupApp);

                        if (groupAppRes == null || groupAppRes.getBody() == null) {
                                cleanUpActivity(groupRes, userRes);

                                throw createIntegrationException(
                                                500,
                                                "Failed to create application subgroup");
                        }

                        if (groupAppRes.getStatusCode() != HttpStatus.CREATED
                                        && groupAppRes.getStatusCode() != HttpStatus.OK) {

                                cleanUpActivity(groupRes, userRes);

                                throw createIntegrationException(
                                                500,
                                                "Failed to create application subgroup. Status: "
                                                                + groupAppRes.getStatusCode());
                        }

                        log.info(
                                        "application subgroup created - {}",
                                        groupAppRes.getBody().getName());

                        /*
                         * 8. Create ArgoCD structure
                         */
                        Group infraOpsSubGr = null;

                        if (groupArgoRes.getStatusCode() == HttpStatus.CREATED
                                        || groupArgoRes.getStatusCode() == HttpStatus.OK) {

                                if (groupCompRes.getStatusCode() == HttpStatus.CREATED
                                                || groupCompRes.getStatusCode() == HttpStatus.OK) {

                                        if (groupAppRes.getStatusCode() == HttpStatus.CREATED
                                                        || groupAppRes.getStatusCode() == HttpStatus.OK) {

                                                ResponseEntity<Group> infraOpsResponse = createArgoCDStructure(
                                                                groupArgoRes.getBody(),
                                                                "infra-ops",
                                                                "gitops-sync-repo",
                                                                "owner");

                                                if (infraOpsResponse == null
                                                                || infraOpsResponse.getBody() == null) {

                                                        cleanUpActivity(groupRes, userRes);

                                                        throw createIntegrationException(
                                                                        500,
                                                                        "Failed to create infra-ops subgroup");
                                                }

                                                infraOpsSubGr = infraOpsResponse.getBody();

                                                createArgoCDStructure(
                                                                infraOpsSubGr,
                                                                "deployments",
                                                                "git-infra-comp-apps-repo",
                                                                "developer");

                                                CacheRefreshEvent cacheRefreshEvent = new CacheRefreshEvent(this);

                                                applicationEventMulticaster
                                                                .multicastEvent(cacheRefreshEvent);
                                        }
                                }
                        }

                        /*
                         * 9. Get environment replication group
                         */
                        ResponseEntity<Group> envReplicationResponse = gitLabService.getGroupByGroupName(
                                        "env-replication-gitops");

                        if (envReplicationResponse == null
                                        || envReplicationResponse.getBody() == null) {

                                cleanUpActivity(groupRes, userRes);

                                throw createIntegrationException(
                                                500,
                                                "Environment replication GitLab group not found");
                        }

                        Group gitGroupEnvRepl = envReplicationResponse.getBody();

                        /*
                         * 10. Create environment synchronization repository
                         */
                        GitProject gitOpsSynchProj = new GitProject();

                        gitOpsSynchProj.setName(
                                        gitEnvironment.getEnvName() + "-synch");

                        gitOpsSynchProj.setPath(
                                        gitEnvironment.getEnvName().toLowerCase()
                                                        + "-synch");

                        gitOpsSynchProj.setNamespaceId(
                                        gitGroupEnvRepl.getId());

                        gitOpsSynchProj.setVisibility("private");
                        gitOpsSynchProj.setInitReadMe(true);

                        ResponseEntity<Project> gitOpsProjRes = gitLabService.createProject(gitOpsSynchProj);

                        if (gitOpsProjRes == null || gitOpsProjRes.getBody() == null) {

                                cleanUpActivity(groupRes, userRes);

                                throw createIntegrationException(
                                                500,
                                                "Failed to create environment synchronization repository");
                        }

                        if (!gitOpsProjRes.getStatusCode().is2xxSuccessful()) {

                                cleanUpActivity(groupRes, userRes);

                                throw createIntegrationException(
                                                500,
                                                "Failed to create environment synchronization repository. Status: "
                                                                + gitOpsProjRes.getStatusCode());
                        }

                        /*
                         * 11. Final safety check.
                         */
                        if (accessToken == null
                                        || accessToken.getToken() == null
                                        || accessToken.getToken().isBlank()) {

                                cleanUpActivity(groupRes, userRes);

                                throw createIntegrationException(
                                                500,
                                                "GitLab access token is not available");
                        }

                        return ResponseEntity.ok(accessToken.getToken());

                } catch (GitLabAPIException e) {

                        log.error(
                                        "GitLab API error during environment creation: {}",
                                        sanitizeForLog(e.getMessage()), e);

                        cleanUpActivity(groupRes, userRes);

                        IntegrationExceptionMessage iem = new IntegrationExceptionMessage();

                        iem.setCode(500);
                        iem.setMessage(
                                        e.getMessage() != null
                                                        ? e.getMessage()
                                                        : "GitLab API error during environment creation");

                        throw new IntegrationException(iem, e);
                }
        }

        private void cleanUpActivity(ResponseEntity<Group> groupRes, ResponseEntity<User> userRes) {
                try {
                        if (null != groupRes && groupRes.getBody() != null) {
                                gitLabService.deleteGroup(String.valueOf(groupRes.getBody().getId()));
                                log.info("Root Group {} deleted", groupRes.getBody().getName());
                        }
                        if (null != userRes && userRes.getBody() != null) {
                                gitLabService.deleteUser(userRes.getBody().getId());
                                log.info("User {} deleted", userRes.getBody().getName());
                        }
                } catch (GitLabAPIException e) {
                        log.error("Error in cleanup: {}", e.getMessage(), e);
                }
        }

        private ResponseEntity<Group> createArgoCDStructure(Group groupArgo, String subGrName, String projRepoName,
                        String projCreatorRole) throws GitLabAPIException {
                // create infra-ops group
                GitGroup gitInfraSubGr = new GitGroup();
                gitInfraSubGr.setName(subGrName);
                gitInfraSubGr.setPath(subGrName);
                gitInfraSubGr.setCanCreateProject(projCreatorRole);
                gitInfraSubGr.setCanCreateSubgrp("owner");
                gitInfraSubGr.setId(groupArgo.getId());
                ResponseEntity<Group> gitInfraSubGrRes = gitLabService.createGroup(gitInfraSubGr);

                log.info(gitInfraSubGrRes.getBody().getFullName() + " Sub Group created.");

                // create gitops-synch repo
                GitProject gitOpsSynchProj = new GitProject();
                gitOpsSynchProj.setName(projRepoName);
                gitOpsSynchProj.setNamespaceId(groupArgo.getId());
                gitOpsSynchProj.setVisibility("private");
                gitOpsSynchProj.setInitReadMe(true);
                ResponseEntity<Project> gitOpsProjRes = gitLabService.createProject(gitOpsSynchProj);

                log.info(gitOpsProjRes.getBody().getPath() + "/" + gitOpsProjRes.getBody().getName()
                                + " Project Repo created.");

                return gitInfraSubGrRes;
        }

        @Operation(summary = "Refresh In-Memory Cache", description = "Manually triggers immediate synchronization of the Hazelcast all-group-members cache")
        @ApiResponses(value = {
                @ApiResponse(responseCode = "200", description = "Cache refreshed successfully")
        })
        @GetMapping(path = { "/refreshCache" })
        public ResponseEntity<Void> refreshCache() {
                log.info("Refreshing all-group-members cache on demand....");
                cacheService.initCache();
                return new ResponseEntity<>(HttpStatus.OK);
        }

        private IntegrationException createIntegrationException(
                        int code,
                        String message) {

                IntegrationExceptionMessage iem = new IntegrationExceptionMessage();

                iem.setCode(code);
                iem.setMessage(message);

                return new IntegrationException(iem);
        }
}