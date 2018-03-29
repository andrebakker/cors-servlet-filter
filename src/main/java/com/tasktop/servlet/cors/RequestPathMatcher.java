package com.tasktop.servlet.cors;

import static java.util.stream.Collectors.joining;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import javax.servlet.http.HttpServletRequest;

class RequestPathMatcher {
	private static final String PATTERN_OR = "|";
	private static final String PATTERN_PREFIX = "^";
	private static final String PATTERN_SUFFIX = "(/|$)";
	private static final String PATH_DELIMITER_PATTERN = "[,\\s]+";
	
	private final Pattern pathPattern;
	
	private List<String> paths = Collections.emptyList();
	
	RequestPathMatcher(String headerCheckPaths) {
		this.paths = splitParameterToPathPatterns(headerCheckPaths);
		
		String urlExclusionRegex = paths.stream().map(this::decodePath).map(Pattern::quote).collect(joining(PATTERN_OR));
		this.pathPattern = Pattern.compile(PATTERN_PREFIX + urlExclusionRegex + PATTERN_SUFFIX);
	}

	boolean matchesRequest(HttpServletRequest request) {
		String requestPath = decodePath(getRequestUriWithoutContextPath(request));
		return pathPattern.matcher(requestPath).find();
	}
	
	private String decodePath(String path) {
		try {
			return URLDecoder.decode(path, StandardCharsets.UTF_8.name());
		} catch (UnsupportedEncodingException e) {
			throw new RuntimeException(e);
		}
	}
	
	private String getRequestUriWithoutContextPath(HttpServletRequest request) {
		String requestURI = request.getRequestURI(); 
		return requestURI.substring(request.getContextPath().length());
	}
	
	private List<String> splitParameterToPathPatterns(String value) {
		return Arrays.asList(value.split(PATH_DELIMITER_PATTERN))
				.stream()
				.map(String::trim)
				.filter(s -> !s.isEmpty())
				.collect(Collectors.toList());
	}
	
	List<String> getPaths() {
		return this.paths;
	}
}