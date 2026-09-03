package com.numbons.gitlabintegration.service;

import java.util.List;

import com.numbons.gitlabintegration.api.model.User;

public interface CacheService {
	
	void initCache();
	
	List<User> getAllGroupMembers(Integer id);

}
