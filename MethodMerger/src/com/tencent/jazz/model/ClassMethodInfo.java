package com.tencent.jazz.model;

import java.util.HashMap;
import java.util.Map;

import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;

import com.tencent.retchet.util.ASMUtils;

/**
 * 类-方法的封装
 * @author noverguo
 */
public class ClassMethodInfo {
	public ClassNode mClassNode;
	public MethodNode mMethodNode;
	public Map<String, ClassMethodInfo> callerMap = new HashMap<String, ClassMethodInfo>();
	public Map<String, ClassMethodInfo> invokerMethodMap = new HashMap<String, ClassMethodInfo>();
	public Map<String, ClassFieldInfo> invokerFieldMap = new HashMap<String, ClassFieldInfo>();
	
	public ClassMethodInfo() {
	}

	public ClassMethodInfo(ClassNode mClassNode, MethodNode mMethodNode) {
		this.mClassNode = mClassNode;
		this.mMethodNode = mMethodNode;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((mClassNode == null) ? 0 : mClassNode.name.hashCode());
		result = prime * result + ((mMethodNode == null) ? 0 : mMethodNode.name.hashCode());
		result = prime * result + ((mMethodNode == null) ? 0 : mMethodNode.desc.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		ClassMethodInfo other = (ClassMethodInfo) obj;
		if (mClassNode == null) {
			if (other.mClassNode != null)
				return false;
		} else if (!mClassNode.name.equals(other.mClassNode.name))
			return false;
		if (mMethodNode == null) {
			if (other.mMethodNode != null)
				return false;
		} else if (!mMethodNode.name.equals(other.mMethodNode.name)) {
			return false;
		} else if (!mMethodNode.desc.equals(other.mMethodNode.desc))
			return false;
		return true;
	}

	@Override
	public String toString() {
		return toString(mClassNode, mMethodNode);
	}
	public static String toString(ClassNode cn, MethodNode mn) {
		return ASMUtils.toString(cn, mn);
	}

	public static String toString(MethodInsnNode min) {
		return ASMUtils.toString(min);
	}
}
