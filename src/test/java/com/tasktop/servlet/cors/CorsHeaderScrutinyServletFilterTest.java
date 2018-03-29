package com.tasktop.servlet.cors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.Optional;

import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

public class CorsHeaderScrutinyServletFilterTest {

	private static final String HTTP_HEADER_HOST = "Host";

	private static final String HTTP_HEADER_ORIGIN = "Origin";

	private static final String HTTP_HEADER_REFERER = "Referer";

	private static final String HTTP_HEADER_X_FORWARDED_HOST = "X-Forwarded-Host";

	@Rule
	public final ExpectedException thrown = ExpectedException.none();

	private final HttpServletResponse response = mock(HttpServletResponse.class);

	private final HttpServletRequest request = mock(HttpServletRequest.class);

	private final CorsHeaderScrutinyServletFilter filter = new CorsHeaderScrutinyServletFilter();

	private final FilterChain chain = mock(FilterChain.class);

	private final FilterConfig config = mock(FilterConfig.class);


	@Before
	public void before() {
		doReturn(Collections.emptyEnumeration()).when(request).getHeaders(any());
	}

	@Test
	public void initWithNoHeaderCheckExclusionParam() throws ServletException {
		filter.init(config);
		assertThat(filter.getRequestExclusionMatcher()).isEmpty();
	}
	
	@Test
	public void initWithHeaderCheckExclusionPath() throws ServletException {
		filter.init(configWithExlusionPathAs("some/path"));
		assertThat(filter.getRequestExclusionMatcher()).isNotEmpty();
	}

	@Test
	public void destroy() {
		filter.destroy();
	}

	@Test
	public void doFilterAcceptsRequestWithoutOriginOrReferer() throws IOException, ServletException {
		verifyDoFilterAcceptsRequest();
	}

	@Test
	public void doFilterRejectsRequestWithEmptyOrigin() throws IOException, ServletException {
		mockHeader(HTTP_HEADER_ORIGIN, "");
		verifyDoFilterRejectsRequest();
	}

	@Test
	public void doFilterRejectsRequestWithOriginWithoutHost() throws IOException, ServletException {
		mockHeader(HTTP_HEADER_ORIGIN, "http://a-host");
		verifyDoFilterRejectsRequest();
	}

	@Test
	public void doFilterRejectsRequestWithOriginWithDifferentHost() throws IOException, ServletException {
		mockHeader(HTTP_HEADER_ORIGIN, "http://a-host");
		mockHeader(HTTP_HEADER_HOST, "a-different-host");
		verifyDoFilterRejectsRequest();
	}

	@Test
	public void doFilterAcceptsRequestWithOriginWithDifferentHostAndMatchingXForwardedHost()
			throws IOException, ServletException {
		mockHeader(HTTP_HEADER_ORIGIN, "http://a-host");
		mockHeader(HTTP_HEADER_HOST, "a-different-host");
		mockHeader(HTTP_HEADER_X_FORWARDED_HOST, "a-host");
		verifyDoFilterAcceptsRequest();
	}

	@Test
	public void doFilterRejectsRequestWithOriginWithMatchingHostAndDifferentXForwardedHost()
			throws IOException, ServletException {
		mockHeader(HTTP_HEADER_ORIGIN, "http://a-host");
		mockHeader(HTTP_HEADER_HOST, "a-host");
		mockHeader(HTTP_HEADER_X_FORWARDED_HOST, "a-different-host");
		verifyDoFilterRejectsRequest();
	}

	@Test
	public void doFilterRejectsRequestWithOriginWithEmptyHost() throws IOException, ServletException {
		mockHeader(HTTP_HEADER_ORIGIN, "http://");
		mockHeader(HTTP_HEADER_HOST, "");
		verifyDoFilterRejectsRequest();
	}

	@Test
	public void doFilterRejectsRequestWithMultipleOriginHeadersWithMatchingHost() throws IOException, ServletException {
		mockHeader(HTTP_HEADER_ORIGIN, "http://a-host", "http://a-different-host");
		mockHeader(HTTP_HEADER_HOST, "a-host");
		verifyDoFilterRejectsRequest();
	}

	@Test
	public void doFilterAcceptsRequestWithMatchingOrigin() throws IOException, ServletException {
		mockHeader(HTTP_HEADER_ORIGIN, "http://a-host");
		mockHeader(HTTP_HEADER_HOST, "a-host");
		verifyDoFilterAcceptsRequest();
	}

	@Test
	public void doFilterAcceptsRequestWithMatchingOriginAndPort() throws IOException, ServletException {
		mockHeader(HTTP_HEADER_ORIGIN, "http://a-host:8080");
		mockHeader(HTTP_HEADER_HOST, "a-host:8080");
		verifyDoFilterAcceptsRequest();
	}

	@Test
	public void doFilterAcceptsRequestWithMatchingOriginAndDifferentPort() throws IOException, ServletException {
		mockHeader(HTTP_HEADER_ORIGIN, "http://a-host:9999");
		mockHeader(HTTP_HEADER_HOST, "a-host:8080");
		verifyDoFilterAcceptsRequest();
	}

	@Test
	public void doFilterRejectsRequestWithEmptyReferer() throws IOException, ServletException {
		mockHeader(HTTP_HEADER_REFERER, "");
		verifyDoFilterRejectsRequest();
	}

	@Test
	public void doFilterRejectsRequestWithRefererWithoutHost() throws IOException, ServletException {
		mockHeader(HTTP_HEADER_REFERER, "http://a-host/some/path");
		verifyDoFilterRejectsRequest();
	}

	@Test
	public void doFilterRejectsRequestWithRefererWithDifferentHost() throws IOException, ServletException {
		mockHeader(HTTP_HEADER_REFERER, "http://a-host/some/path");
		mockHeader(HTTP_HEADER_HOST, "a-different-host");
		verifyDoFilterRejectsRequest();
	}

	@Test
	public void doFilterRejectsRequestWithRefererWithEmptyHost() throws IOException, ServletException {
		mockHeader(HTTP_HEADER_REFERER, "/a-path");
		mockHeader(HTTP_HEADER_HOST, "");
		verifyDoFilterRejectsRequest();
	}

	@Test
	public void doFilterRejectsRequestWithMultipleRefererHeadersWithMatchingHost()
			throws IOException, ServletException {
		mockHeader(HTTP_HEADER_REFERER, "http://a-host", "http://a-host");
		mockHeader(HTTP_HEADER_HOST, "a-host");
		verifyDoFilterRejectsRequest();
	}

	@Test
	public void doFilterAcceptsRequestWithMatchingReferer() throws IOException, ServletException {
		mockHeader(HTTP_HEADER_REFERER, "http://a-host/some/path");
		mockHeader(HTTP_HEADER_HOST, "a-host");
		verifyDoFilterAcceptsRequest();
	}

	@Test
	public void doFilterAcceptsRequestWithMatchingRefererAndOrigin() throws IOException, ServletException {
		mockHeader(HTTP_HEADER_REFERER, "http://a-host/some/path");
		mockHeader(HTTP_HEADER_ORIGIN, "http://a-host");
		mockHeader(HTTP_HEADER_HOST, "a-host");
		verifyDoFilterAcceptsRequest();
	}
	
	@Test
	public void doFilterSkipsHeaderCheckWhenExclusionIsAllowed() throws IOException, ServletException {
		RequestPathMatcher matchAllRequests = mock(RequestPathMatcher.class);
		doReturn(true).when(matchAllRequests).matchesRequest(any());
		
		CorsHeaderScrutinyServletFilter filter = createFilterWithPathMatcher(matchAllRequests);
		
		filter.doFilter(request, response, chain);
		verify(chain).doFilter(request, response);
		verifyNoMoreInteractions(chain);
		verifyHeaderCheckSkipped();
	}
	
	@Test
	public void doFilterPerformsHeaderCheckWhenExclusionIsNotAllowed() throws IOException, ServletException {
		RequestPathMatcher matchNoRequests = mock(RequestPathMatcher.class);
		doReturn(false).when(matchNoRequests).matchesRequest(any());
		
		CorsHeaderScrutinyServletFilter filter = createFilterWithPathMatcher(matchNoRequests);
		
		filter.doFilter(request, response, chain);
		verify(chain).doFilter(request, response);
		verifyNoMoreInteractions(chain);
		
		verify(request, atLeastOnce()).getHeaders(any());
	}
	
	private FilterConfig configWithExlusionPathAs(String path) {
		doReturn(path).when(config).getInitParameter(CorsHeaderScrutinyServletFilter.PATH_EXCLUSION_PATTERN);
		return config;
	}

	private void verifyDoFilterAcceptsRequest() throws IOException, ServletException {
		filter.doFilter(request, response, chain);
		verify(chain).doFilter(request, response);
		verifyNoMoreInteractions(chain);
	}

	private void verifyDoFilterRejectsRequest() throws IOException, ServletException {
		filter.doFilter(request, response, chain);
		expectForbidden();
	}
	
	private void verifyHeaderCheckSkipped() {
		verify(request, never()).getHeader(any());
		verify(request, never()).getHeaderNames();
		verify(request, never()).getHeaders(any());
	}

	private void expectForbidden() throws IOException {
		verify(response).sendError(HttpServletResponse.SC_FORBIDDEN, "Forbidden");
		verifyNoMoreInteractions(chain);
	}

	private void mockHeader(String headerName, String... values) {
		doAnswer(i -> Collections.enumeration(Arrays.asList(values))).when(request).getHeaders(headerName);
	}
	
	private CorsHeaderScrutinyServletFilter createFilterWithPathMatcher(RequestPathMatcher matcher) {
		return  new CorsHeaderScrutinyServletFilter() {
			@Override
			Optional<RequestPathMatcher> getRequestExclusionMatcher() {
				return Optional.of(matcher);
			}
		};
	}
}