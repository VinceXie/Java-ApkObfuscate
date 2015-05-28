package com.tencent.inop;

import java.io.*;

/**
 * @author VinceXie vince.xie@qq.com
 * @version 2014-8-18 上午10:51:12
 * State
 */
public class InjectProguard {

	/**
	 * @param args
	 * @throws Exception 
	 */
	public static void main(String[] args) throws Exception {
		
		System.out.print("此程序可把merger的函数合并功能注入到目标proguard中,维持目标proguard原有功能不变。\n注入后文件名为opguard,回车继续");
		System.in.read();
		if(args.length==0)
		{
			System.out.println("Usage:java -jar inject.jar proguard.jar");		
			return;
		}
		
		ClassPoolHelper cph=new ClassPoolHelper();
		cph.loadJar("merger.jar", true);
		cph.loadJar(args[0], false);
		
		cph.inject();
		
		cph.saveJar("opguard.jar");
		
		System.out.println("merger已注入至proguard,请直接使用opguard.jar。");
	}

	
}
