package com.numbons.gitlabintegration.api.client;

import com.numbons.gitlabintegration.api.configuration.GitLabClientConfiguration;
import com.numbons.gitlabintegration.api.model.GitAccessToken;
import com.numbons.gitlabintegration.api.model.Group;
import com.numbons.gitlabintegration.api.model.Project;
import com.numbons.gitlabintegration.api.model.User;

import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;
import java.util.Map;


@FeignClient(name = "${feign.name}", url = "${feign.url}"+"${feign.url.api}", configuration = GitLabClientConfiguration.class)
public interface GitLabClient {
    @PostMapping(value = "/users", consumes = MediaType.APPLICATION_JSON_VALUE)
    ResponseEntity<User> createUser(Map<String, ?> queryMap);

    @DeleteMapping(value = "/users/{id}", consumes = MediaType.APPLICATION_JSON_VALUE)
    ResponseEntity<String> deleteUser(@PathVariable Integer id, @PathVariable boolean hard_delete);

    @PostMapping(value = "/groups", consumes = MediaType.APPLICATION_JSON_VALUE)
    ResponseEntity<Group> createGroup(Map<String, ?> queryMap);
    
    @PostMapping(value = "/projects", consumes = MediaType.APPLICATION_JSON_VALUE)
    ResponseEntity<Project> createProject(Map<String, ?> queryMap);

    @DeleteMapping(value = "/groups/{id}", consumes = MediaType.APPLICATION_JSON_VALUE)
    ResponseEntity<String> deleteGroup(@PathVariable Integer id);

    @PostMapping(value = "/groups/{id}/members", consumes = MediaType.APPLICATION_JSON_VALUE)
    ResponseEntity<String> associateUserWithGroup(Map<String, ?> queryMap, @PathVariable Integer id);

    @GetMapping(value = "/groups/{id}/members", consumes = MediaType.APPLICATION_JSON_VALUE)
    ResponseEntity<String> getUserGroupRelations(@PathVariable Integer id);
    
    @GetMapping(value = "/groups/{id}/members/all", consumes = MediaType.APPLICATION_JSON_VALUE)
    ResponseEntity<List<User>> getGroupMembers(@PathVariable Integer id,@RequestParam(required = false,defaultValue = "100",name = "per_page") Integer per_page);
    

    @GetMapping(value = "/groups", consumes = MediaType.APPLICATION_JSON_VALUE)
    ResponseEntity<List<Group>> getAllGroups(@RequestParam(required = false,defaultValue = "true",name = "top_level_only") Boolean top_level_only,@RequestParam(required = false,defaultValue = "100",name = "per_page") Integer per_page);
    
   
    @GetMapping(value = "/groups/{id}/subgroups", consumes = MediaType.APPLICATION_JSON_VALUE)
    ResponseEntity<List<Group>> getAllSubGroupOfGroup(@PathVariable String id,@RequestParam(required = false,defaultValue = "100",name = "per_page") Integer per_page);
    
    @GetMapping(value = "/users", consumes = MediaType.APPLICATION_JSON_VALUE)
    ResponseEntity<List<User>> getUser(@RequestParam String username);
    
    @PostMapping(value = "/users/{id}/personal_access_tokens", consumes = MediaType.APPLICATION_JSON_VALUE)
    ResponseEntity<GitAccessToken> generatePersonalAccessToken(@PathVariable Integer id,Map<String, ?> queryMap);
    
    @GetMapping(value = "/personal_access_tokens", consumes = MediaType.APPLICATION_JSON_VALUE)
    ResponseEntity<List<GitAccessToken>> getPersonalAccessToken(@RequestParam("user_id")int id,@RequestParam("search") String name,@RequestParam("state") String active);

    @DeleteMapping(value = "/personal_access_tokens/{id}", consumes = MediaType.APPLICATION_JSON_VALUE)
    ResponseEntity<Void> revokePersonalAccessToken(@PathVariable int id);
}
