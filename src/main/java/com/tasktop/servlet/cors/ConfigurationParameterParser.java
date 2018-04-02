package com.tasktop.servlet.cors;

import java.util.Arrays;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

public class ConfigurationParameterParser {
	private static final String PATH_DELIMITER_PATTERN = "[,\\s]+";
	
	static List<String> parseExclusionPaths(String paths) {
		return Arrays.asList(paths.split(PATH_DELIMITER_PATTERN))
				.stream()
				.map(String::trim)
				.filter(s -> !s.isEmpty())
				.map(prependSlashIfNotPresent())
				.collect(Collectors.toList());
	}

	private static Function<String, String> prependSlashIfNotPresent() {
		return path -> {
			if(path.startsWith("/")) {
				return path;
			}
			return "/" + path;
		};
	}
}
