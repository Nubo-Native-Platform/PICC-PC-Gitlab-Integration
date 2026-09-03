package com.numbons.gitlabintegration.api.configuration;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.Scheduled;

import com.numbons.gitlabintegration.service.CacheService;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;

@Configuration
@Slf4j
public class CacheConfiguration {


	@Autowired
	private CacheService cacheService;

	@PostConstruct
	@Scheduled(cron = "${cache.refresh.cron.expression:${cache.refresh.corn.expression:0 0 * * * *}}")
	private void initAllGroupsCache() {
		cacheService.initCache();
	}

	// NOTE: the Hazelcast Config bean previously lived here (commented out).
	// It has been moved to its own standalone HazelcastConfiguration class.
	// This class autowires CacheService, and CacheService (via CacheServiceImpl)
	// depends on HazelcastInstance, which in turn depends on the Config bean -
	// defining Config here creates a circular dependency:
	// cacheConfiguration -> hazelcastInstance -> cacheServiceImpl -> cacheConfiguration.
	// See HazelcastConfiguration for the actual network/port setup.

}