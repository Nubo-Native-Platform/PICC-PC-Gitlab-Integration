package com.numbons.gitlabintegration.service.impl;

import com.numbons.gitlabintegration.api.client.GitLabClient;
import com.numbons.gitlabintegration.api.model.GitAccessToken;
import com.numbons.gitlabintegration.api.model.GitGroup;
import com.numbons.gitlabintegration.api.model.GitProject;
import com.numbons.gitlabintegration.api.model.GitUser;
import com.numbons.gitlabintegration.api.model.Group;
import com.numbons.gitlabintegration.api.model.MemberGroupAssociation;
import com.numbons.gitlabintegration.api.model.Project;
import com.numbons.gitlabintegration.api.model.User;
import com.numbons.gitlabintegration.service.CacheService;
import com.numbons.gitlabintegration.service.GitLabService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
public class GitLabServiceImpl implements GitLabService {
    @Autowired
    private GitLabClient gitLabClient;

    @Autowired
    private CacheService cacheService;

    @Value("${feign.url}")
    private String gitUrl;

    @Override
    public ResponseEntity<User> createUser(GitUser gitUser) {
        Map<String, String> queryMap = new HashMap<>();
        queryMap.put("email", gitUser.getEmail());
        queryMap.put("name", gitUser.getName());
        queryMap.put("username", gitUser.getUserName());
        queryMap.put("password", gitUser.getPassword());
        //queryMap.put("reset_password", "fasle");
        //queryMap.put("force_random_password", "false");
        queryMap.put("can_create_group", "false");//no group or subgroup can be created by the user outside group
        queryMap.put("projects_limit", "0"); //no project can be created by the user outside the assigned group
        queryMap.put("external", "true"); //user is external to restrict view of other group only public group can be viewed

        ResponseEntity<User> user = gitLabClient.createUser(queryMap);
        // log.debug("User {}", user.getStatusCode().value());
        return user;
    }

    @Override
    public ResponseEntity<String> deleteUser(Integer id) {
        ResponseEntity<String> user = gitLabClient.deleteUser(id,true);
        // log.debug("User {}", user.getStatusCode().value());
        return user;
    }

    @Override
    public ResponseEntity<List<User>> getUser(String username) {
        ResponseEntity<List<User>> users = gitLabClient.getUser(username);
       
        // log.debug("Users {}", users.getStatusCode().value());
        return users;
    }

    @Override
    public ResponseEntity<Group> createGroup(GitGroup gitGroup) {
        Map<String, String> queryMap = new HashMap<>();
        queryMap.put("name", gitGroup.getName());
        String path = (gitGroup.getPath() != null && !gitGroup.getPath().isBlank()) 
                ? gitGroup.getPath() 
                : gitGroup.getName().toLowerCase();
        queryMap.put("path", path);
        if(gitGroup.getCanCreateProject()!=null)
        	queryMap.put("project_creation_level", gitGroup.getCanCreateProject());
        if(gitGroup.getCanCreateSubgrp()!=null)
        	queryMap.put("subgroup_creation_level", gitGroup.getCanCreateSubgrp());
        if(gitGroup.getId()!=0)
        	queryMap.put("parent_id", String.valueOf(gitGroup.getId()));
        

        ResponseEntity<Group> group = gitLabClient.createGroup(queryMap);
        // log.debug("Group {}", group.getStatusCode().value());
        return group;
    }

    @Override
    public ResponseEntity<String> deleteGroup(String id) {
        ResponseEntity<String> group = gitLabClient.deleteGroup(Integer.parseInt(id));
        // log.debug("Group {}", group.getStatusCode().value());
        return group;
    }

    @Override
    public ResponseEntity<String> associateUserWithGroup(MemberGroupAssociation memberGroupAssociation) {
        Map<String, String> queryMap = new HashMap<>();
        queryMap.put("id", memberGroupAssociation.getId());
        queryMap.put("user_id", memberGroupAssociation.getUserId());
        queryMap.put("access_level", memberGroupAssociation.getAccessLevel());
        ResponseEntity<String> associateUserWithGroup = gitLabClient.associateUserWithGroup(queryMap, Integer.parseInt(memberGroupAssociation.getId()));
        // log.debug("Group associated {}", associateUserWithGroup);
        return associateUserWithGroup;
    }

    @Override
    public ResponseEntity<String> getUserGroupRelations(String id) {
        ResponseEntity<String> userGroupRelations = gitLabClient.getUserGroupRelations(Integer.parseInt(id));
        // log.debug("Relation {}", userGroupRelations);
        return userGroupRelations;
    }


    @Override

    public ResponseEntity<List<Group>> getGroupsByUser(String userId, String groupName) {
        List<Group> accessibleGroups = new ArrayList<>();
        List<Group> allGroups = (groupName != null && !groupName.isBlank()) ? gitLabClient.getAllSubGroupOfGroup(groupName, 100).getBody() : gitLabClient.getAllGroups(true, 100).getBody();
        for (Group g : allGroups) {
            User us = cacheService.getAllGroupMembers(g.getId()).stream().filter(u -> userId.equalsIgnoreCase(u.getUsername())).findAny().orElse(null);
            if (us != null) {
                accessibleGroups.add(g);
            }
        }

        return new ResponseEntity<List<Group>>(accessibleGroups, HttpStatus.OK);
    }

    public ResponseEntity<List<Group>> getAllGroups() {
        List<Group> allGroups = gitLabClient.getAllGroups(true, 100).getBody();
        return new ResponseEntity<List<Group>>(allGroups, HttpStatus.OK);
    }

    @Override
    public ResponseEntity<Group> getGroupByGroupName(String groupName) {
        List<Group> allGroups = gitLabClient.getAllGroups(true, 100).getBody();
        // log.info("getAllGroups call successful");
        Group resultGroup = allGroups.stream()
                .filter(group -> (group.getId() != null)
                        && (group.getName() != null)
                        && (group.getName().equals(groupName)))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("getGroupsByGroupName -- group not found -- " + groupName));
        return new ResponseEntity<Group>(resultGroup, HttpStatus.OK);
    }

    @Override
    public ResponseEntity<Boolean> isGroupExistsInGit(String groupName) {
        Boolean retval = false;
        List<Group> allGroups = gitLabClient.getAllGroups(true, 100).getBody();
        // log.info("getAllGroups call successful");
        if (allGroups != null) {
            List<Group> groupList = allGroups.stream()
                    .filter(group -> (group.getId() != null)
                            && (group.getName() != null)
                            && (group.getName().equals(groupName)))
                    .toList();
            if (!groupList.isEmpty()) {
                retval = true;
            }
        }
        return new ResponseEntity<Boolean>(retval, HttpStatus.OK);
    }
    @Override
    public ResponseEntity<Project> createProject(GitProject gitProject){
    	Map<String, String> queryMap = new HashMap<>();
    	queryMap.put("name", gitProject.getName());
    	String path = (gitProject.getPath() != null && !gitProject.getPath().isBlank()) 
    	        ? gitProject.getPath() 
    	        : gitProject.getName().toLowerCase();
    	queryMap.put("path", path);
    	queryMap.put("namespace_id", String.valueOf(gitProject.getNamespaceId()));
    	queryMap.put("visibility", gitProject.getVisibility());
    	queryMap.put("initialize_with_readme", String.valueOf(true));
    	
        ResponseEntity<Project> projectResp = gitLabClient.createProject(queryMap);
        // log.debug("Project {}", projectResp.getStatusCode().value());
        return projectResp;
    }

	@Override
	public ResponseEntity<GitAccessToken> createToken(User user) {
		String scopes = GitLabService.scope;
		Map<String, String> queryMap = new HashMap<>();
		queryMap.put("name", user.getUsername()+"_token");
		queryMap.put("scopes",scopes);
		return gitLabClient.generatePersonalAccessToken(user.getId(),queryMap);
	}

	@Override
	public ResponseEntity<List<GitAccessToken>> getToken(User user) {
		
		return gitLabClient.getPersonalAccessToken(user.getId(), user.getUsername()+"_token","active");
	}

	@Override
	public void revokeToken(int id) {
		gitLabClient.revokePersonalAccessToken(id);		
	}

}