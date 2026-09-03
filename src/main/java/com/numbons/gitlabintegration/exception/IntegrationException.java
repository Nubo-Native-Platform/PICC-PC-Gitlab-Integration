package com.numbons.gitlabintegration.exception;

import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = false)
public class IntegrationException extends RuntimeException {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	
	private IntegrationExceptionMessage integrationExceptionMessage ;

	public IntegrationException(IntegrationExceptionMessage message, Exception ex) {
		super(message.getMessage(), ex);
		integrationExceptionMessage = message; 
	}

	public IntegrationException(IntegrationExceptionMessage message) {
		super(message.getMessage());
		integrationExceptionMessage = message;
	}
	
	

}
