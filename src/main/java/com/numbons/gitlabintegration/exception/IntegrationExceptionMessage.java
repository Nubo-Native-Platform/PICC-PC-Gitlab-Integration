package com.numbons.gitlabintegration.exception;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class IntegrationExceptionMessage {
	
	private int code ;
	private String message ;

}
