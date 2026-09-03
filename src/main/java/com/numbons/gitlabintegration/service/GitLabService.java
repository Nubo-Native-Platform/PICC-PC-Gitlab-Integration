package com.numbons.gitlabintegration.service;

import com.numbons.gitlabintegration.api.model.GitAccessToken;
import com.numbons.gitlabintegration.api.model.GitGroup;
import com.numbons.gitlabintegration.api.model.GitProject;
import com.numbons.gitlabintegration.api.model.GitUser;
import com.numbons.gitlabintegration.api.model.Group;
import com.numbons.gitlabintegration.api.model.MemberGroupAssociation;
import com.numbons.gitlabintegration.api.model.Project;
import com.numbons.gitlabintegration.api.model.User;

import java.util.List;

import org.springframework.http.ResponseEntity;

public interface GitLabService {
	
	String scope = "api,read_user,read_api,read_repository,write_repository,read_registry,write_registry";
	
    ResponseEntity<User> createUser(GitUser gitUser);

    ResponseEntity<String> deleteUser(Integer id);

    ResponseEntity<Group> createGroup(GitGroup gitGroup);
    
    ResponseEntity<Project> createProject(GitProject gitProject);

    ResponseEntity<String> deleteGroup(String id);

    ResponseEntity<String> associateUserWithGroup(MemberGroupAssociation memberGroupAssociation);

    ResponseEntity<String> getUserGroupRelations(String id);
    
	ResponseEntity<List<Group>> getGroupsByUser(String userId,String groupName);

    ResponseEntity<Group> getGroupByGroupName(String groupName);

    ResponseEntity<List<Group>> getAllGroups();

    ResponseEntity<Boolean> isGroupExistsInGit(String groupName);

	ResponseEntity<List<User>> getUser(String username);

	ResponseEntity<GitAccessToken> createToken(User body);
	
	ResponseEntity<List<GitAccessToken>> getToken(User body);
	
	void revokeToken(int id);
}
