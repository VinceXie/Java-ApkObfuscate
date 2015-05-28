package com.tencent.obfuscate;

import com.tencent.obfuscate.pool.Pool;


/**
 * @author VinceXie vince.xie@qq.com
 * @version 2014-10-16 下午5:19:09
 * State
 */
public class Main {

	public static void main(String args[]) throws Exception
	{
		Configuration.initArgs(args);
		Pool pool=Pool.loadJar();
	}
}
