package org.graalvm.argo.proxies;

import java.lang.reflect.Method;
import java.util.Arrays;

public class JavaProxy {

	/**
	 * Expected args: <timestamp> <target class name> <remaining args>
	 * @param args
	 * @throws Exception
	 */
	public static void main(String[] args) throws Exception {
		if (args.length < 2) {
			System.err.println("Error invoking JavaProxy, expected at least two arguments (timestamp and target classname).");
			System.exit(1);
		}

		System.out.println("VMM boot time: " + (System.currentTimeMillis() - Long.parseLong(args[0])));
		Class<?> cls = Class.forName(args[1]);
		Method meth = cls.getMethod("main", String[].class);
		meth.invoke(null, (Object) Arrays.copyOfRange(args, 2, args.length));
	}
}
