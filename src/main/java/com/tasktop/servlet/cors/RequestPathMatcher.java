package com.tasktop.servlet.cors;

import static java.util.stream.Collectors.joining;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.function.Function;
import java.util.regex.Pattern;

import javax.servlet.http.HttpServletRequest;

class RequestPathMatcher {
	private static final String PATTERN_OR = "|";
	private static final String PATTERN_PREFIX = "^";
	
	private final Pattern pathPattern;
		
	RequestPathMatcher(List<String> paths) {
		String urlExclusionRegex = paths.stream()
			.map(this::decodePath)
			.map(Pattern::quote)
			.map(addPatternPrefix())
			.collect(joining(PATTERN_OR));
		
		this.pathPattern = Pattern.compile(urlExclusionRegex);
	}

	private Function<String, String> addPatternPrefix() {
		return pattern -> PATTERN_PREFIX + pattern;
	}

	boolean matchesRequest(HttpServletRequest request) {
		return pathPattern.matcher(getRequestUriWithoutContextPath(request)).find();
	}
	
	private String decodePath(String path) {
		try {
			return URLDecoder.decode(path, StandardCharsets.UTF_8.name());
		} catch (UnsupportedEncodingException e) {
			throw new RuntimeException(e);
		}
	}
	
	private String getRequestUriWithoutContextPath(HttpServletRequest request) {
		return decodePath(request.getRequestURI()).substring(decodePath(request.getContextPath()).length());
	}
}