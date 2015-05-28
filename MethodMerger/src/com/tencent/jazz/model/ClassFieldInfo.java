package com.tencent.jazz.model;

import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldNode;

/**
 * 类-属性的封装
 * @author noverguo
 */
public class ClassFieldInfo {
	public ClassNode mClassNode;
	public FieldNode mFieldNode;
	public ClassFieldInfo(ClassNode mClassNode, FieldNode mFieldNode) {
		this.mClassNode = mClassNode;
		this.mFieldNode = mFieldNode;
	}
	
	@Override
	public String toString() {
		return mClassNode.name + "." + mFieldNode.name + "->" + mFieldNode.desc + " " + mFieldNode.access;
	}
	
}
