package com.tencent.obfuscate.jar;

import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Random;

import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.analysis.Analyzer;
import org.objectweb.asm.tree.analysis.AnalyzerException;
import org.objectweb.asm.tree.analysis.BasicVerifier;

import com.tencent.obfuscate.pool.AsmVerify;
import com.tencent.obfuscate.pool.MethodInfo;

/**
 * @author VinceXie vince.xie@qq.com
 * @version 2014-8-22 下午5:53:03
 * State
 */
public class Tools implements Opcodes {
	
	/**
	 * 防止随机生成重复
	 */
	private static ArrayList<String> randoms=new ArrayList<String>();
	
	public static String getRandomString(int length) {    
	    //StringBuffer buffer = new StringBuffer("0123456789abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ");    
	    StringBuffer buffer = new StringBuffer("abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ");  
	    StringBuffer sb = new StringBuffer();    
	    Random r = new Random();    
	    int range = buffer.length();    
	    for (int i = 0; i < length; i ++) {    
	        sb.append(buffer.charAt(r.nextInt(range)));    
	    }    
	    if(randoms.contains(sb.toString()))
	    {
	    	return getRandomString(length);
	    }
	    else {
	    	randoms.add(sb.toString());
	    	return sb.toString();    
		}   
	}   
	
	public static boolean isInterface(ClassNode classNode)
	{
		if((classNode.access&ACC_INTERFACE)==ACC_INTERFACE)
		{
			return true;
		}
		return false;
	}
	
	public static boolean isDefault(int access)
	{
		if(!((access&ACC_PUBLIC)==ACC_PUBLIC)&&!((access&ACC_PRIVATE)==ACC_PRIVATE)&&!((access&ACC_PROTECTED)==ACC_PROTECTED))
		{
			return true;
		}
		return false;
	}
	
	public static boolean isStatic(int access)
	{
		if((access&ACC_STATIC)==ACC_STATIC)
		{
			return true;
		}
		return false;
	}
	
	public static boolean isFinal(int access)
	{
		if((access&ACC_FINAL)==ACC_FINAL)
		{
			return true;
		}
		return false;
	}
	
	public static boolean isAbstract(int access)
	{
		if((access&ACC_ABSTRACT)==ACC_ABSTRACT)
		{
			return true;
		}
		return false;
	}
	
	public static boolean isSynthetic(int access)
	{
		if((access&ACC_SYNTHETIC)==ACC_SYNTHETIC)
		{
			return true;
		}
		return false;
	}
	
	public static boolean isPrivate(int access) {
		if ((access & ACC_PRIVATE) == ACC_PRIVATE) {
			return true;
		}
		return false;
	}
	
	public static boolean isPublic(int access) {
		if ((access & ACC_PUBLIC) == ACC_PUBLIC) {
			return true;
		}
		return false;
	}

	public static boolean isProtect(int access)
	{
		if ((access & ACC_PROTECTED) == ACC_PROTECTED) {
			return true;
		}
		return false;
	}
	
	public static boolean isReturn(int opcode)
	{
		if(opcode>=172&&opcode<=177)
		{
			return true;
		}
		return false;
	}
	
	public static void printMethod(MethodInfo methodInfo) throws Exception
	{
		BasicVerifier verifier = new BasicVerifier();
		Analyzer a = new Analyzer(verifier);
		try
		{
			a.analyze(methodInfo.methodNode.name, methodInfo.methodNode);
		}catch(Exception e)
		{			
			e.printStackTrace(System.err);			
		}
		AsmVerify.printAnalyzerResult(methodInfo.methodNode, a, new PrintWriter(System.out));
		
	}
}
