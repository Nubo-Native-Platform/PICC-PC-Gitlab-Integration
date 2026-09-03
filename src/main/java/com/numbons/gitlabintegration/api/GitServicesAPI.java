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
public class GitServicesAPI {
        @Autowired
        private GitLabService gitLabService;

        @Autowired
        private CacheService cacheService;

        @Autowired
        private ApplicationEventMulticaster applicationEventMulticaster;

        @PostMapping(path = "/createUser")
        public ResponseEntity<User> createUser(@RequestBody GitUser gitUser) {
                return gitLabService.createUser(gitUser);
        }

        @GetMapping(path = "/getUser")
        public ResponseEntity<User> getUser(@RequestParam String username) {
                List<User> users = gitLabService.getUser(username).getBody();
                if (users.size() > 0) {
                        return new ResponseEntity(users.get(0), HttpStatus.OK);
                } else {
                        return new ResponseEntity(new User(), HttpStatus.EXPECTATION_FAILED);
                }
        }

        @DeleteMapping(path = "/deleteUser")
        public ResponseEntity<String> deleteUser(@RequestParam Integer id) {
                return gitLabService.deleteUser(id);
        }

        @PostMapping(path = "/createGroups")
        public ResponseEntity<Group> createGroup(@RequestBody GitGroup gitGroup) {
                return gitLabService.createGroup(gitGroup);
        }

        @DeleteMapping(path = "/deleteGroup/")
        public ResponseEntity<String> deleteGroup(@RequestParam String id) {
                return gitLabService.deleteGroup(id);
        }

        @PostMapping(value = "/associateUser")
        public ResponseEntity<String> associateUserWithGroup(MemberGroupAssociation memberGroupAssociation) {
                return gitLabService.associateUserWithGroup(memberGroupAssociation);
        }

        @GetMapping(value = "/getListOfMembers/{groupId}")
        public ResponseEntity<String> getListOfMembersOfGroup(@PathVariable String groupId) {
                return gitLabService.getUserGroupRelations(groupId);
        }

        @PostMapping(value = "/v1/onboard/user")
        public ResponseEntity<String> onboardV1(@RequestBody GitUser gitUser) {

                int statusCode;
                String userType = gitUser.getUserType();

                // create a user
                ResponseEntity<User> respEntityUser = gitLabService.createUser(gitUser);
                User user = respEntityUser.getBody();
                statusCode = respEntityUser.getStatusCode().value();

                // Get group by name
                Group group = gitLabService.getGroupByGroupName(gitUser.getGroupName()).getBody();

                // assign user to group

                MemberGroupAssociation memberGrAssociation = new MemberGroupAssociation();
                memberGrAssociation.setId(String.valueOf(group.getId()));
                memberGrAssociation.setUserId(String.valueOf(user.getId()));
                // memberGrAssociation.setAccessLevel("40");//Maintainer access
                if (userType.equals("superAdmin") || userType.equals("admin")) {
                        memberGrAssociation.setAccessLevel("40");// Maintainer access
                } else {
                        memberGrAssociation.setAccessLevel("30");// Developer access
                }
                ResponseEntity<String> associationResp = gitLabService.associateUserWithGroup(memberGrAssociation);
                statusCode = associationResp.getStatusCode().value();

                return associationResp;
        }

        @GetMapping(path = { "/getGroups/{userId}", "/getSubGroups/{userId}/{groupName}" })
        public ResponseEntity<List<Group>> getGroupsByUser(@PathVariable String userId,
                        @PathVariable(required = false) String groupName) {
                return gitLabService.getGroupsByUser(userId, groupName);
        }

        @GetMapping(path = { "/getAllGroups" })
        public ResponseEntity<List<Group>> getAllGroups() {
                return gitLabService.getAllGroups();
        }

        @GetMapping(path = { "/exist/group/{groupName}" })
        public ResponseEntity<Boolean> isGroupExistsInGit(@PathVariable String groupName) {
                return gitLabService.isGroupExistsInGit(groupName);
        }

        @GetMapping(path = { "/getGroupDetails/{groupName}" })
        ResponseEntity<Group> getGroupByGroupName(@PathVariable String groupName) {
                return gitLabService.getGroupByGroupName(groupName);
        }

        @GetMapping(path = { "/getLcncGitPAT/{userId}" })
        public ResponseEntity<String> getTokenForIngPullsecret(@PathVariable String userId) {
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
                        iem.setMessage("Ony one user should be available against a userId. But received more !! "
                                        + userId);
                        throw new IntegrationException(iem);
                }
                List<GitAccessToken> gitAccTokens = gitLabService.getToken(users.get(0)).getBody();
                if (gitAccTokens == null || gitAccTokens.isEmpty()) {
                        token = gitLabService.createToken(users.get(0)).getBody();
                } else {
                        token = gitAccTokens.get(0);// considering only one token will present for a user with a
                                                    // specific name and status active
                        /*
                         * If token exist, we can not get the value of the token.
                         * So only option to revoke and create a new token with same name.
                         */
                        gitLabService.revokeToken(token.getId());
                        token = gitLabService.createToken(users.get(0)).getBody();

                }
                return ResponseEntity.ok(token.getToken());
        }

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
                         *
                         * accessToken has already been validated above.
                         * This check also makes the method safe if the code is
                         * modified later.
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
                        if (null != groupRes) {
                                gitLabService.deleteGroup(String.valueOf(groupRes.getBody().getId()));
                                log.info("Root Group {} deleted", groupRes.getBody().getName());
                        }
                        if (null != userRes) {
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

        @GetMapping(path = { "/refreshCache" })
        public ResponseEntity<Void> refreshCache() {
                log.info("Refreshing all-group-members cache on demand....");
                cacheService.initCache();
                return new ResponseEntity<Void>(HttpStatus.OK);
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