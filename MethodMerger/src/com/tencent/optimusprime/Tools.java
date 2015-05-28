package com.tencent.optimusprime;

import java.util.Random;

import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.ClassNode;

/**
 * @author VinceXie vince.xie@qq.com
 * @version 2014-8-22 下午5:53:03
 * State
 */
public class Tools implements Opcodes {
	
	public static String getRandomString(int length) {    
	    StringBuffer buffer = new StringBuffer("0123456789abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ");    
	    StringBuffer sb = new StringBuffer();    
	    Random r = new Random();    
	    int range = buffer.length();    
	    for (int i = 0; i < length; i ++) {    
	        sb.append(buffer.charAt(r.nextInt(range)));    
	    }    
	    return sb.toString();    
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

}
