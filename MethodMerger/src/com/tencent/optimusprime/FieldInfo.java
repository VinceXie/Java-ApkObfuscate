package com.tencent.optimusprime;

import org.objectweb.asm.tree.FieldNode;

/**
 * @author VinceXie vince.xie@qq.com
 * @version 2014-7-2 下午3:20:58
 * State
 */
public class FieldInfo {


	/**
	 * 调用的类
	 */
	public MClassNode classNode;
	
	/**
	 * 调用的方法
	 */
	public FieldNode fieldNode;
	
	
	public FieldInfo(MClassNode mClassNode,FieldNode fieldNode)
	{
		this.classNode=mClassNode;
		this.fieldNode=fieldNode;
	}
}
