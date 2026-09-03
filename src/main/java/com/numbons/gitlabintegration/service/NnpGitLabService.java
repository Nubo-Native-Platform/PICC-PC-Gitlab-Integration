
package com.numbons.gitlabintegration.service;

import java.util.List;

import org.springframework.http.ResponseEntity;

import com.numbons.gitlabintegration.api.model.GitAccessToken;
import com.numbons.gitlabintegration.api.model.User;

public interface NnpGitLabService {
	
	String scope = "api,read_user,read_api,read_repository,write_repository,read_registry,write_registry";
	
	ResponseEntity<List<User>> getUser(String username);

	ResponseEntity<GitAccessToken> createToken(User body, String env);
	
	ResponseEntity<List<GitAccessToken>> getToken(User body, String env);
	
	void revokeToken(int id);

}
