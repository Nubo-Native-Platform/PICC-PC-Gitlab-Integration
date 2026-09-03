package com.numbons.gitlabintegration.service.impl;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentMap;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.numbons.gitlabintegration.api.client.GitLabClient;
import com.numbons.gitlabintegration.api.model.Group;
import com.numbons.gitlabintegration.api.model.User;
import com.numbons.gitlabintegration.service.CacheService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.HazelcastJsonValue;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class CacheServiceImpl implements CacheService {

	@Autowired
	private GitLabClient gitLabClient;

	@Autowired
	private HazelcastInstance hazelcastInstance;

	private ConcurrentMap<String, HazelcastJsonValue> allGroupMembersMap() {
		return hazelcastInstance.getMap("all-group-members");
	}

	@Override
	public void initCache() {
		// log.info("Initializing all-group-members cache .....");
		ObjectMapper mapper = new ObjectMapper();

		List<Group> allGroups = gitLabClient.getAllGroups(true, 100).getBody();

		for (Group g : allGroups) {

			List<Group> allSubGroups = gitLabClient.getAllSubGroupOfGroup(g.getId().toString(), 100).getBody();

			for (Group sg : allSubGroups) {
				List<User> sgUsers = gitLabClient.getGroupMembers(sg.getId(), 100).getBody();
				try {
					String usersJsonString = mapper.writeValueAsString(sgUsers);
					allGroupMembersMap().put(sg.getId().toString(), new HazelcastJsonValue(usersJsonString));
				} catch (JsonProcessingException e) {
					log.error("Error occurred while initializing all-group-members cache for subgroup [{}]: ",
							sg.getId(), e);
				}

			}

			List<User> users = gitLabClient.getGroupMembers(g.getId(), 100).getBody();
			try {
				String usersJsonString = mapper.writeValueAsString(users);
				allGroupMembersMap().put(g.getId().toString(), new HazelcastJsonValue(usersJsonString));
			} catch (JsonProcessingException e) {
				log.error("Error occurred while initializing all-group-members cache for group [{}]: ", g.getId(), e);
			}

		}

		log.info("all-group-members cache Initialized Successfully.....");

	}

	@Override
	public List<User> getAllGroupMembers(Integer id) {

		ObjectMapper objectMapper = new ObjectMapper();

		ConcurrentMap<String, HazelcastJsonValue> allGroupMembersCache = allGroupMembersMap();

		HazelcastJsonValue allGroupMembersHJson = allGroupMembersCache.get(id.toString());

		// log.info(allGroupMembersHJson.getValue());

		List<User> allGroups = new ArrayList<User>();
		try {
			allGroups = objectMapper.readValue(allGroupMembersHJson.getValue(), new TypeReference<List<User>>() {
			});
		} catch (Exception e) {
			log.error("Error reading group members from cache for group ID [{}]: ", id, e);
		}

		return allGroups;
	}

}
