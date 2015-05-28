package com.tencent.jazz.util;

import java.io.PrintStream;

/**
 * 日志输出
 * @author noverguo
 */
public class Log {
	public static PrintStream sOut = System.out;
	public static void i(String msg) {
		sOut.println(msg);
		sOut.flush();
	}
	public static void close() {
		if(sOut != System.out) {
			sOut.close();
		}
	}
	public static void o(String msg) {
		System.out.println("[Optimus Prime] " + msg);
	}
}
