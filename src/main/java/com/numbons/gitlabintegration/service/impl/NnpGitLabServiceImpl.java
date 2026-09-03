
package com.numbons.gitlabintegration.service.impl;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.numbons.gitlabintegration.service.NnpGitLabService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import com.numbons.gitlabintegration.api.client.NnpGitlabClient;
import com.numbons.gitlabintegration.api.model.GitAccessToken;
import com.numbons.gitlabintegration.api.model.User;
import com.numbons.gitlabintegration.service.GitLabService;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class NnpGitLabServiceImpl implements NnpGitLabService {

	@Autowired
	private NnpGitlabClient nnpGitlabClient;
	
	@Override
    public ResponseEntity<List<User>> getUser(String username) {
        ResponseEntity<List<User>> users = nnpGitlabClient.getUser(username);
       
        // log.debug("Users {}", users.getStatusCode().value());
        return users;
    }

	@Override
	public ResponseEntity<GitAccessToken> createToken(User user,String env) {
		String scopes = GitLabService.scope;
		Map<String, String> queryMap = new HashMap<>();
		queryMap.put("name", env + "_token");
		queryMap.put("scopes", scopes);
		return nnpGitlabClient.generatePersonalAccessToken(user.getId(), queryMap);
	}

	@Override
	public ResponseEntity<List<GitAccessToken>> getToken(User user, String env) {

		return nnpGitlabClient.getPersonalAccessToken(user.getId(), env + "_token", "active");
	}

	@Override
	public void revokeToken(int id) {
		nnpGitlabClient.revokePersonalAccessToken(id);
	}



}
