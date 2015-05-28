package com.tencent.cc;

import java.io.*;

/**
 * @author VinceXie vince.xie@qq.com
 * @version 2014-8-18 上午10:51:12
 * State
 */
public class CC {

	/**
	 * @param args
	 * @throws Exception 
	 */
	public static void main(String[] args) throws Exception {
		
		ClassPoolHelper cph=new ClassPoolHelper();
		cph.loadJar("merger.jar", true);
		cph.loadJar(args[0], false);
		
		cph.inject();
		
		cph.saveJar("merger_change.jar");
		
	}

	
}
