package com.tasktop.servlet.cors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;

import org.junit.Test;

public class RequestPathMatcherTest {
	private RequestPathMatcher requestMatcher;
	
	@Test
	public void createWithEmptyString() throws ServletException {
		requestMatcher = new RequestPathMatcher("");
		assertThat(requestMatcher.getPaths()).isEmpty();
	}
	
	@Test 
	public void createWithSinglePath() throws ServletException {
		requestMatcher = new RequestPathMatcher("/some/path");
		assertThat(requestMatcher.getPaths()).containsExactly("/some/path");
	}

	@Test
	public void createWithCommaSeparatedPaths() throws ServletException {
		requestMatcher = new RequestPathMatcher("/some/path,/other/different/path");
		assertThat(requestMatcher.getPaths()).containsExactlyInAnyOrder("/some/path", "/other/different/path");
	}
	
	@Test
	public void createWithWhitespaceSeparatedPaths() throws ServletException {
		requestMatcher = new RequestPathMatcher("/some/path /other/different/path");
		assertThat(requestMatcher.getPaths()).containsExactlyInAnyOrder("/some/path", "/other/different/path");
	}
	
	@Test
	public void createWithMultipleMixedDelimitersSeparatedPaths() throws ServletException {
		requestMatcher = new RequestPathMatcher("/first/path, /second/path\t/third/path\n\t\n/fourth/path   \t ");
		assertThat(requestMatcher.getPaths())
			.containsExactlyInAnyOrder("/first/path", "/second/path", "/third/path", "/fourth/path");
	}
	
	@Test
	public void matchesRequestWithinPathList() {
		requestMatcher = new RequestPathMatcher("/somewhere");
		assertMatchesRequest(mockRequest("endpoint", "somewhere"));
	}
	
	@Test
	public void doesNotMatchRequestNotWithinPathList() {
		requestMatcher = new RequestPathMatcher("/other");
		assertDoesNotMatchRequest(mockRequest("endpoint", "somewhere"));
	}
	
	@Test
	public void doesNotMatchRequestWhenPathPartiallyInPathList() {
		requestMatcher = new RequestPathMatcher("/some");
		assertDoesNotMatchRequest(mockRequest("endpoint", "somewhere"));
	}
	
	@Test
	public void doesNotMatchRequestWhenPathOnStartOfPathList() {
		requestMatcher = new RequestPathMatcher("/somewhereelse");
		assertDoesNotMatchRequest(mockRequest("endpoint", "somewhere"));
	}
	
	@Test
	public void doesNotMatchRequestWhenPathOnEndOfPathList() {
		requestMatcher = new RequestPathMatcher("/elsesomewhere");
		assertDoesNotMatchRequest(mockRequest("endpoint", "somewhere"));
	}
	
	@Test
	public void doesNotMatchRequestWhenPathDoesNotStartWithExcludedPaths() {
		requestMatcher = new RequestPathMatcher("/else/somewhere");
		assertDoesNotMatchRequest(mockRequest("endpoint", "somewhere"));
	}
	
	@Test
	public void doesNotMatchRequestWhenPatternMoreSpecificThanRequest() {
		requestMatcher = new RequestPathMatcher("/somewhere/else");
		assertDoesNotMatchRequest(mockRequest("endpoint", "somewhere"));
	}
	
	@Test
	public void matchesRequestWhenRequestMoreSpecificThanPattern() {
		requestMatcher = new RequestPathMatcher("/somewhere/else");
		assertMatchesRequest(mockRequest("endpoint", "somewhere/else/specifically"));
	}
	
	@Test
	public void matchesRequestWhenPercentEncodedRequestMatchesPattern() {
		requestMatcher = new RequestPathMatcher("/somewhere/é");
		assertMatchesRequest(mockRequest("endpoint", "somewhere/%C3%A9"));
	}
	
	@Test
	public void doesNotMatchRequestWhenPercentEncodedRequestNotInPattern() {
		requestMatcher = new RequestPathMatcher("/somewhere/élse");
		assertDoesNotMatchRequest(mockRequest("endpoint", "somewhere/%C3%A9"));
	}
	
	@Test
	public void matchesRequestWithSemanticallyEquivalentPathInPatternButDifferentEncodings() {
		requestMatcher = new RequestPathMatcher("/a+space/");
		assertMatchesRequest(mockRequest("endpoint", "a%20space/"));
	}
	
	@Test
	public void matchesRequestWhenPathWithinMatcherPathList() {
		requestMatcher = new RequestPathMatcher("\t\t/some/other/place\n\t\t\t/somewhere/else\n");
		assertMatchesRequest(mockRequest("endpoint", "somewhere/else/specifically"));
	}
	
	@Test
	public void doesNotMatchRequestWhenPathNotWithinMatcherPathList() {
		requestMatcher = new RequestPathMatcher("\t\t/some/other/place\n\t\t\t/somewhere/else\n");
		assertDoesNotMatchRequest(mockRequest("endpoint", "somewhere/third/place"));
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
