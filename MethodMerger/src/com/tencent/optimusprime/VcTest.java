package com.tencent.optimusprime;

import java.util.ArrayList;

/**
 * @author VinceXie vince.xie@qq.com
 * @version 2014-5-29 下午4:29:57
 * State
 */
public class VcTest {

	public static String[] argStrings;
	
	public static void main(String[] args) throws Exception {
		//测试用时
		long start=System.currentTimeMillis();
		
		if(args.length==0)
		{
			System.out.println("参数不对，第一个参数为proguard混淆后的jar包，第二个参数为proguard混淆后的mapping.txt");
			return;
		}
		argStrings=args;
		Example();
		
		//测试用时
		long end=System.currentTimeMillis();
		System.out.println("time used:"+(end-start)/1000);
	}
	

	/**
	 * 例子
	 * @throws Exception
	 */
	public static void Example() throws Exception {

		/**
		 * 获得一颗树
		 */
		ClassTreeHelper classTreeHelper=new ClassTreeHelper();
		if(argStrings.length==2)
		{
			classTreeHelper.loadJar(argStrings[0], argStrings[1],null);
		}
		else {
			classTreeHelper.loadJar(argStrings[0], null,null);
		}
		ClassTree classTree=classTreeHelper.buildTree();
		
		/**
		 * 以下为测试结果
		 */
		ArrayList<MethodInfo> methodInfos=classTree.getMethodInfos(ClassTree.FLAG_ALL);
		for(MethodInfo methodInfo:methodInfos)
		{
			System.out.println("_________________");
			System.out.println("所在类:"+methodInfo.classNode);
			System.out.println("函数:"+methodInfo.methodNode.name+" "+methodInfo.methodNode.desc);
			System.out.println("canMer:"+methodInfo.canMer+" canMerOut:"+methodInfo.canMerOutside);
			System.out.println("unOverride:"+methodInfo.unOverride);
			
			
			for(Caller caller:methodInfo.callers)
			{
				System.out.println("caller:");
				System.out.println("调用类:"+caller.classNode.name);
				System.out.println("调用方法:"+caller.methodNode.name);
			}
			
			for(Invoker invoker:methodInfo.invokers)
			{
				System.out.println("invoker:");
				System.out.println("调用类:"+invoker.classNode.name);
				System.out.println("调用方法:"+invoker.methodNode.name);
			}
			
			for(FieldInfo fieldInfo:methodInfo.fieldInfos)
			{
				System.out.println("invoke field:");
				System.out.println("调用类:"+fieldInfo.classNode.name);
				System.out.println("调用字段:"+fieldInfo.fieldNode.name);
			}
		}

		classTree.messClass();
		//各种操作
		/**
		 * 写回到原文件
		 */
		classTreeHelper.saveJar(argStrings[0]+"_mer");
	
	}


}
