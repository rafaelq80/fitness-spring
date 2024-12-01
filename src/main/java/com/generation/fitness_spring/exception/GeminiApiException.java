package com.generation.fitness_spring.exception;

import org.springframework.http.HttpStatusCode;

public class GeminiApiException extends RuntimeException {
    
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	
	private final HttpStatusCode httpStatusCode;

    public GeminiApiException(String message, HttpStatusCode httpStatusCode) {
        super(message);
        this.httpStatusCode = httpStatusCode;
    }

    public HttpStatusCode getStatusCode() {
        return httpStatusCode;
    }
}
