package com.rthink.data.monitor.util;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class RegexUtils {
	
	public static String interceptRegexStr(String regex, String str) {
		Pattern pattern = Pattern.compile(regex);
		Matcher matcher = pattern.matcher(str);
		while (matcher.find()) {
			return matcher.group(1);
		}
		return null;
	}
	
}
