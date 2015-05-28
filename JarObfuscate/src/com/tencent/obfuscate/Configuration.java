package com.tencent.obfuscate;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;



/**
 * @author VinceXie vince.xie@qq.com
 * @version 2014-6-11 上午11:28:50 State 读取proguard生成的mapping
 */
public class Configuration {

	/**
	 * 需读取的配置路径
	 */
	public static String outPath;

	public static String jarPath;
	
	public static String keepOpPath;


	/**
	 * 混淆过方法：类名+方法名+参数
	 */
	public static Set<String> obfuseds = new HashSet<String>();
	public static Set<String> applyMappingObfuseds = new HashSet<String>();

	/**
	 * proguard混淆过的的类名字典:a/b/c->a/b/b a/b/b=a/b/c
	 */
	public static HashMap<String, String> backClassMap = new HashMap<String, String>();


	
	public static HashMap<String, String> messRecords =new HashMap<String, String>();

	/**
	 * keepop指定不被合并的方法
	 */
	public static Set<String> keepClassList = new HashSet<String>();

	

	
	public static void readKeepOp()
	{
		if (keepOpPath == null) {
			return;
		}
		File file = new File(keepOpPath);
		BufferedReader br;
		try {
			br = new BufferedReader(new InputStreamReader(new FileInputStream(file)));

			for (String line = br.readLine(); line != null; line = br.readLine()) {
				if(line.startsWith("-keepop "))
				{
					keepClassList.add(line.split(" ")[1].replaceAll("\\.", "/").trim());
				}
			}
			br.close();
		} catch (Exception e) {

			e.printStackTrace();
			return;
		}
	}

	public static void initArgs(String[] args) throws Exception {
		jarPath = args[0];

		String[] arr = new String[2];
		for (int i = 0, j = 0; i < args.length && j < arr.length; ++i) {
			if (args[i].startsWith("-")) {
				++i;
			} else {
				arr[j++] = args[i];
			}
		}

		for (int i = 0; i < args.length; ++i) {
			if ("-k".equals(args[i].trim())) {
				keepOpPath = args[i + 1];
				++i;
			}
		}
		
		
	}


	public static void read() {
		
		readKeepOp();
	}
}
