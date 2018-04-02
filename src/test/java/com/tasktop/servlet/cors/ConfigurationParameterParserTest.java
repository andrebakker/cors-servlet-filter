package com.tasktop.servlet.cors;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.Test;

public class ConfigurationParameterParserTest {
	@Test
	public void parseExclusionParameterEmptyString() {
		assertThat(ConfigurationParameterParser.parseExclusionPaths("")).isEmpty();
	}
	
	@Test 
	public void parseExclusionParameterWithSinglePath() {
		assertThat(ConfigurationParameterParser.parseExclusionPaths("/some/path")).containsExactly("/some/path");
	}

	@Test
	public void parseExclusionParameterWithCommaSeparatedPaths() {
		assertThat(ConfigurationParameterParser.parseExclusionPaths("/some/path,/other/different/path"))
			.containsExactlyInAnyOrder("/some/path", "/other/different/path");
	}
	
	@Test
	public void parseExclusionParameterWithWhitespaceSeparatedPaths() {
		assertThat(ConfigurationParameterParser.parseExclusionPaths("/some/path /other/different/path"))
			.containsExactlyInAnyOrder("/some/path", "/other/different/path");
	}
	
	@Test
	public void parseExclusionParameterWithMultipleMixedDelimitersSeparatedPaths() {
		assertThat(ConfigurationParameterParser.parseExclusionPaths("/first/path, /second/path\t/third/path\n\t\n/fourth/path   \t "))
			.containsExactlyInAnyOrder("/first/path", "/second/path", "/third/path", "/fourth/path");
	}
	
	@Test
	public void parseExclusionParameterPrependsSlashIfNotPresent() {
		assertThat(ConfigurationParameterParser.parseExclusionPaths("/some/path,other/different/path"))
		.containsExactlyInAnyOrder("/some/path", "/other/different/path");
	}
}
