// Copyright © 2013-2014 Esko Luontola <www.orfjackal.net>
// This software is released under the Apache License 2.0.
// The license text is at http://www.apache.org/licenses/LICENSE-2.0

package net.orfjackal.retrolambda.defaultmethods;

import org.objectweb.asm.Type;

import java.lang.reflect.Method;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import static org.objectweb.asm.Type.*;
/**
 * Created by arneball on 2014-08-12.
 */
public class Helpers {
	private static final Pattern pattern = Pattern.compile("\\((.*)\\)(.*)");

	public static String addParam(String desc, String className) {
		Matcher m = pattern.matcher(desc);
		m.find();
		String rest = m.group(1);
		String returntype = m.group(2);
		return String.format("(L%s;%s)%s", className, rest, returntype);
	}

	public static String changeReturnType(String desc, String returnType) {
		Matcher m = pattern.matcher(desc);
		m.find();
		String rest = m.group(1);
		return String.format("(%s)L%s;", rest, returnType);
	}

	public static Class<?> loadClass(String className) {
		try {
			ClassLoader cl = Thread.currentThread().getContextClassLoader();
			return cl.loadClass(className.replace('/', '.'));
		} catch (ClassNotFoundException e) {
			throw new RuntimeException(e);
		}
	}

	public static boolean isPrimitive(Type containerReturnType) {
		return Stream.of(BYTE_TYPE, SHORT_TYPE, INT_TYPE, LONG_TYPE, FLOAT_TYPE, DOUBLE_TYPE, VOID_TYPE, BOOLEAN_TYPE)
				.filter(containerReturnType::equals)
				.findAny()
				.isPresent();

	}
}
