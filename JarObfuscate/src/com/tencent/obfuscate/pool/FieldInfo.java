package com.tencent.obfuscate.pool;

import java.util.ArrayList;

import org.objectweb.asm.tree.FieldNode;
import org.objectweb.asm.tree.MethodNode;

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
	 * 调用的字段
	 */
	public FieldNode fieldNode;
	
	
	/**
	 * 
	 * @param mClassNode
	 * @param fieldNode
	 */
	public FieldInfo(MClassNode mClassNode,FieldNode fieldNode)
	{
		this.classNode=mClassNode;
		this.fieldNode=fieldNode;
	}
}
