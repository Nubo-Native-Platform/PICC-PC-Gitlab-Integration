package com.numbons.gitlabintegration.events;

import org.springframework.stereotype.Component;

import com.numbons.gitlabintegration.service.CacheService;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@AllArgsConstructor
public class EventListener {

	private CacheService cacheService;

	@org.springframework.context.event.EventListener(CacheRefreshEvent.class)
	public void onCacheRefresh(CacheRefreshEvent event) {
		cacheService.initCache();
	}
}