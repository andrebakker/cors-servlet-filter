/*******************************************************************************
 * Copyright (c) 2017 Tasktop Technologies.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *     http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *******************************************************************************/
package com.tasktop.servlet.cors;

import static java.util.stream.Collectors.joining;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URLDecoder;
import java.util.Arrays;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

//based on https://www.owasp.org/index.php/CORS_OriginHeaderScrutiny
public class CorsHeaderScrutinyServletFilter implements Filter {

	private static final String HEADER_HOST = "Host";
	private static final String HEADER_X_FORWARDED_HOST = "X-Forwarded-Host";
	private static final String HEADER_ORIGIN = "Origin";
	private static final String HEADER_REFERER = "Referer";
	
	static final String EXCLUDE_HEADER_CHECK_PARAM = "exclude-header-check";
	
	Optional<RequestExclusionMatcher> requestExclusionMatcher = Optional.empty();

	@Override
	public void init(FilterConfig filterConfig) throws ServletException {
		requestExclusionMatcher = Optional.ofNullable(filterConfig.getInitParameter(EXCLUDE_HEADER_CHECK_PARAM))
				.map(RequestExclusionMatcher::new);
	}
	
	@Override
	public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
			throws IOException, ServletException {
		HttpServletRequest httpRequest = (HttpServletRequest)request;
		if (requestExclusionMatcher.map(matcher -> !matcher.allowHeaderCheckExclusion(httpRequest)).orElse(true)) {
			try {
				checkRequestHeaders(httpRequest);
			} catch (ForbiddenException e) {
				((HttpServletResponse) response).sendError(HttpServletResponse.SC_FORBIDDEN, e.getMessage());
				return;
			}
		}
		chain.doFilter(request, response);
	}

	private void checkRequestHeaders(HttpServletRequest request) {
		getSingleHeader(request, HEADER_ORIGIN).ifPresent(headerValue -> validateUriHeader(request, headerValue));
		getSingleHeader(request, HEADER_REFERER).ifPresent(headerValue -> validateUriHeader(request, headerValue));
	}

	private void validateUriHeader(HttpServletRequest request, String headerValue) {
		checkNotEmpty(headerValue);
		String hostHeader = getEffectiveHostHeader(request);
		checkNotEmpty(hostHeader);
		URI uri = URI.create(headerValue);
		checkHost(hostHeader, uri.getHost());
	}

	private String getEffectiveHostHeader(HttpServletRequest request) {
		return getSingleHeader(request, HEADER_X_FORWARDED_HOST).orElse(getSingleHeaderChecked(request, HEADER_HOST));
	}

	private void checkHost(String hostHeader, String host) {
		String hostHeaderHost = hostHeader.split(":")[0];
		if (!hostHeaderHost.equals(host)) {
			throw createForbiddenException();
		}
	}

	private String getSingleHeaderChecked(HttpServletRequest request, String headerName) {
		return getSingleHeader(request, headerName).orElseThrow(() -> createForbiddenException());
	}

	private Optional<String> getSingleHeader(HttpServletRequest request, String headerName) {
		List<String> list = listHeaders(request, headerName);
		if (list.size() > 1) {
			throw createForbiddenException();
		}
		return list.stream().findFirst();
	}

	private void checkNotEmpty(String headerValue) {
		if (headerValue.trim().isEmpty()) {
			throw createForbiddenException();
		}
	}

	private ForbiddenException createForbiddenException() {
		return new ForbiddenException("Forbidden");// don't disclose the reason
	}

	@SuppressWarnings("unchecked")
	private List<String> listHeaders(HttpServletRequest request, String headerName) {
		return Collections.list((Enumeration<String>) request.getHeaders(headerName));
	}

	@Override
	public void destroy() {
		// nothing to do
	}
	
	Optional<RequestExclusionMatcher> getRequestExclusionMatcher() {
		return this.requestExclusionMatcher;
	}
	
	class RequestExclusionMatcher {
		private static final String PATTERN_OR = "|";
		
		private static final String PATTERN_PREFIX = "^";
		
		private static final String PATTERN_SUFFIX = "(/|$)";
		
		private static final String PATH_DELIMITER_PATTERN = "[,\\s]";
		
		private final Pattern acceptPattern;
		
		private List<String> excludePaths = Collections.emptyList();
		
		RequestExclusionMatcher(String excludeHeaderCheckPaths) {
			this.excludePaths = toPathList(excludeHeaderCheckPaths);
			
			String urlExclusionRegex = excludePaths.stream().map(Pattern::quote).collect(joining(PATTERN_OR));
			this.acceptPattern = Pattern.compile(PATTERN_PREFIX + urlExclusionRegex + PATTERN_SUFFIX);
		}

		private boolean allowHeaderCheckExclusion(HttpServletRequest request) {
			try {
				String requestPath = URLDecoder.decode(getRequestUriWithoutContextPath(request), "UTF-8");
				return acceptPattern.matcher(requestPath).find();
			} catch (UnsupportedEncodingException e) {
				return false;
			}
		}
		
		private String getRequestUriWithoutContextPath(HttpServletRequest request) {
			String requestURI = request.getRequestURI(); 
			return requestURI.substring(requestURI.indexOf(request.getContextPath()) + request.getContextPath().length());
		}
		
		private List<String> toPathList(String value) {
			return Arrays.asList(value.split(PATH_DELIMITER_PATTERN))
					.stream()
					.map(String::trim)
					.filter(((Predicate<String>) String::isEmpty).negate())
					.collect(Collectors.toList());
		}
		
		List<String> getExcludePaths() {
			return this.excludePaths;
		}
	}
}
