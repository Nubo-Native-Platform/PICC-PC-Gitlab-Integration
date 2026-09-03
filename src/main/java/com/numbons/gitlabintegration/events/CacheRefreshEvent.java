package com.numbons.gitlabintegration.events;

import org.springframework.context.ApplicationEvent;

public class CacheRefreshEvent extends ApplicationEvent {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	public CacheRefreshEvent(Object source) {
		super(source);
	}

}
