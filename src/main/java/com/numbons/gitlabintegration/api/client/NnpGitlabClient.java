
package com.numbons.gitlabintegration.api.client;

import java.util.List;
import java.util.Map;

import com.numbons.gitlabintegration.api.configuration.NnpGitLabClientConfiguration;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.numbons.gitlabintegration.api.model.GitAccessToken;
import com.numbons.gitlabintegration.api.model.User;

@FeignClient(name = "${nnp.feign.name}", url = "${nnp.feign.url}"+"${feign.url.api}", configuration = NnpGitLabClientConfiguration.class)
public interface NnpGitlabClient {
	
	@GetMapping(value = "/users", consumes = MediaType.APPLICATION_JSON_VALUE)
    ResponseEntity<List<User>> getUser(@RequestParam String username);
	
	@PostMapping(value = "/users/{id}/personal_access_tokens", consumes = MediaType.APPLICATION_JSON_VALUE)
    ResponseEntity<GitAccessToken> generatePersonalAccessToken(@PathVariable Integer id,Map<String, ?> queryMap);
    
    @GetMapping(value = "/personal_access_tokens", consumes = MediaType.APPLICATION_JSON_VALUE)
    ResponseEntity<List<GitAccessToken>> getPersonalAccessToken(@RequestParam("user_id")int id,@RequestParam("search") String name,@RequestParam("state") String active);

    @DeleteMapping(value = "/personal_access_tokens/{id}", consumes = MediaType.APPLICATION_JSON_VALUE)
    ResponseEntity<Void> revokePersonalAccessToken(@PathVariable int id);

}
