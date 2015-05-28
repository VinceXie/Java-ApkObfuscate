package com.tencent.optimusprime;

import java.util.ArrayList;

import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.ClassNode;

/**
 * @author VinceXie vince.xie@qq.com
 * @version 2014-5-19 下午2:31:42 State
 */
public class MClassNode extends ClassNode implements Opcodes {
	
	public MClassNode() {
		super(ASM5);
	}
	
	/**
	 * 
	 */
	public String orgName;
	/**
	 * 是否可打乱
	 */
	public boolean canMess=false;
	/**
	 * 类里的方法信息
	 */
	public ArrayList<MethodInfo> methodInfos=new ArrayList<MethodInfo>();
	
	/**
	 * 直系父亲
	 */
	public ArrayList<String> dadNodeNames = new ArrayList<String>();
	
	/**
	 * 直系儿子
	 */
	public ArrayList<String> sonNodeNames = new ArrayList<String>();
	
	/**
	 * 覆盖输出字符串
	 */
	@Override
	public String toString() {
		return name;
	}


}
