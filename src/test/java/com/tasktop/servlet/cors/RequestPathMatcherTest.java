package com.tasktop.servlet.cors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;

import java.util.Arrays;

import javax.servlet.http.HttpServletRequest;

import org.junit.Test;

public class RequestPathMatcherTest {
	private RequestPathMatcher requestMatcher;
	
	@Test
	public void matchesRequestWithinPathList() {
		requestMatcher = createRequestPathMatcher("/somewhere");
		assertMatchesRequest(mockRequest("endpoint", "somewhere"));
	}
	
	@Test
	public void doesNotMatchRequestWithRootPathIfPatternDefinedForSpecificPaths() {
		requestMatcher = createRequestPathMatcher("/somewhere/");
		assertDoesNotMatchRequest(mockRequest("endpoint", "somewhere"));
	}
	
	@Test
	public void doesNotMatchRequestNotWithinPathList() {
		requestMatcher = createRequestPathMatcher("/other");
		assertDoesNotMatchRequest(mockRequest("endpoint", "somewhere"));
	}
	
	@Test
	public void matchesRequestWhenPathPartiallyInPathList() {
		requestMatcher = createRequestPathMatcher("/some");
		assertMatchesRequest(mockRequest("endpoint", "somewhere"));
	}
	
	@Test
	public void doesNotMatchRequestWhenPathOnStartOfPathList() {
		requestMatcher = createRequestPathMatcher("/somewhereelse");
		assertDoesNotMatchRequest(mockRequest("endpoint", "somewhere"));
	}
	
	@Test
	public void doesNotMatchRequestWhenPathOnEndOfPathList() {
		requestMatcher = createRequestPathMatcher("/elsesomewhere");
		assertDoesNotMatchRequest(mockRequest("endpoint", "somewhere"));
	}
	
	@Test
	public void doesNotMatchRequestWhenPathDoesNotStartWithExcludedPaths() {
		requestMatcher = createRequestPathMatcher("/else/somewhere");
		assertDoesNotMatchRequest(mockRequest("endpoint", "somewhere"));
	}
	
	@Test
	public void doesNotMatchRequestWhenPatternMoreSpecificThanRequest() {
		requestMatcher = createRequestPathMatcher("/somewhere/else");
		assertDoesNotMatchRequest(mockRequest("endpoint", "somewhere"));
	}
	
	@Test
	public void matchesRequestWhenRequestMoreSpecificThanPattern() {
		requestMatcher = createRequestPathMatcher("/somewhere/else");
		assertMatchesRequest(mockRequest("endpoint", "somewhere/else/specifically"));
	}
	
	@Test
	public void matchesRequestWhenPercentEncodedRequestMatchesPattern() {
		requestMatcher = createRequestPathMatcher("/somewhere/é");
		assertMatchesRequest(mockRequest("endpoint", "somewhere/%C3%A9"));
	}
	
	@Test
	public void doesNotMatchRequestWhenPercentEncodedRequestNotInPattern() {
		requestMatcher = createRequestPathMatcher("/somewhere/élse");
		assertDoesNotMatchRequest(mockRequest("endpoint", "somewhere/%C3%A9"));
	}
	
	@Test
	public void matchesRequestWithSemanticallyEquivalentPathInPatternButDifferentEncodings() {
		requestMatcher = createRequestPathMatcher("/a+space/");
		assertMatchesRequest(mockRequest("endpoint", "a%20space/"));
	}
	
	@Test
	public void matchesRequestWhenPathWithinMatcherPathList() {
		requestMatcher = createRequestPathMatcher("/some/other/place", "/somewhere/else");
		assertMatchesRequest(mockRequest("endpoint", "somewhere/else/specifically"));
	}
	
	@Test
	public void doesNotMatchRequestWhenPathNotWithinMatcherPathList() {
		requestMatcher = createRequestPathMatcher("/some/other/place", "/somewhere/else");
		assertDoesNotMatchRequest(mockRequest("endpoint", "somewhere/third/place"));
	}
	
	private RequestPathMatcher createRequestPathMatcher(String... paths) {
		return new RequestPathMatcher(Arrays.asList(paths));
	}
	
	private void assertMatchesRequest(HttpServletRequest request) {
		assertThat(requestMatcher.matchesRequest(request)).isTrue();
	}
	
	private void assertDoesNotMatchRequest(HttpServletRequest request) {
		assertThat(requestMatcher.matchesRequest(request)).isFalse();
	}

	private HttpServletRequest mockRequest(String contextPath, String requestURI) {
		HttpServletRequest request = mock(HttpServletRequest.class);
		
		doReturn(contextPath).when(request).getContextPath();
		doReturn(contextPath + "/" + requestURI).when(request).getRequestURI();
		
		return request;
	}
}
