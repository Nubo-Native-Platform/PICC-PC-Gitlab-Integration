
package com.numbons.gitlabintegration.api;

import java.util.List;

import com.numbons.gitlabintegration.service.NnpGitLabService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.numbons.gitlabintegration.api.model.GitAccessToken;
import com.numbons.gitlabintegration.api.model.User;
import com.numbons.gitlabintegration.exception.IntegrationException;
import com.numbons.gitlabintegration.exception.IntegrationExceptionMessage;

import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping(path = "/nnp/api")
@Slf4j
public class NnpGitServiceAPI {
	
	 @Autowired
	 private NnpGitLabService nnpGitLabService;
	
	@GetMapping(path = {"/getNNPGitLabPAT/{userId}/{env}"})
    public ResponseEntity<String> getTokenForIngPullsecret(@PathVariable String userId, @PathVariable String env){
    	GitAccessToken token;
    	List<User> users = nnpGitLabService.getUser(userId).getBody();
    	if(users == null) {
    		IntegrationExceptionMessage iem = new IntegrationExceptionMessage();
			iem.setCode(500);
			iem.setMessage("Should present a user against username - "+userId);
			throw new IntegrationException(iem);
    	}
    	if(users.size()>1) {
    		IntegrationExceptionMessage iem = new IntegrationExceptionMessage();
			iem.setCode(500);
			iem.setMessage("Ony one user should be available against a userId. But received more !! "+userId);
			throw new IntegrationException(iem);
    	}
    	List<GitAccessToken> gitAccTokens = nnpGitLabService.getToken(users.get(0),env).getBody();
    	if(gitAccTokens == null || gitAccTokens.isEmpty()){
    		token = nnpGitLabService.createToken(users.get(0),env).getBody();
    	}else {
    		token = gitAccTokens.get(0);//considering only one token will present for a user with a specific name and status active 
    		/*
    		 * If token exist, we can not get the value of the token.
    		 * So only option to revoke and create a new token with same name.
    		 */
    		nnpGitLabService.revokeToken(token.getId());
    		token = nnpGitLabService.createToken(users.get(0),env).getBody();
    	}
    	return ResponseEntity.ok(token.getToken());
    }


}
