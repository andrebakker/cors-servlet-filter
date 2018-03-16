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

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;
import java.net.URLDecoder;
import java.util.*;

import java.util.function.Function;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
	static final String PATH_EXCLUSION_PATTERN_PARAM = "path-exclusion-validURIPattern";

	private static final String REGEX_PATTERN = "[\n, ]";
	private List<String> pathExclusionPatterns = Collections.emptyList();
	private static final Pattern validURIPattern = Pattern.compile("^/[0-9a-zA-Z]+");
	private static final Logger logger = LoggerFactory.getLogger(CorsHeaderScrutinyServletFilter.class);


	@Override
	public void init(FilterConfig filterConfig) throws ServletException {
		Optional<String> initParameter = Optional.ofNullable(filterConfig.getInitParameter(PATH_EXCLUSION_PATTERN_PARAM));
		pathExclusionPatterns = initParameter
				.map(parseInitParamToExclusionPatterns())
				.orElse(Collections.emptyList());
	}

	List<String> getPathExclusionPatterns() {
		return pathExclusionPatterns;
	}

	private Function<String, List<String>> parseInitParamToExclusionPatterns() {
		return p -> Arrays.stream(p.trim().split(REGEX_PATTERN))
				.map(String::trim)
				.filter(s -> validURIPattern.matcher(s).find()) // throw an error for bad patterns?
				.collect(Collectors.toList());
	}
	
	@Override
	public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
			throws IOException, ServletException {
		HttpServletRequest httpRequest = (HttpServletRequest)request;
		if (!requestPathIsExcluded(httpRequest)) {
			try {
				checkRequestHeaders(httpRequest);
			} catch (ForbiddenException e) {
				((HttpServletResponse) response).sendError(HttpServletResponse.SC_FORBIDDEN, e.getMessage());
				return;
			}
		}
		chain.doFilter(request, response);
	}

	private boolean requestPathIsExcluded(HttpServletRequest request) {
		String requestPath = request.getRequestURI().substring(request.getContextPath().length());

		try {
			URLDecoder.decode(requestPath, "UTF-8");
		} catch (UnsupportedEncodingException e) {
			logger.warn("Unable to decode request endpoint {}", requestPath);
			return true;
		}

		return pathExclusionPatterns.stream()
				.anyMatch(requestPath::startsWith);
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
}
