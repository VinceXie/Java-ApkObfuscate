package com.tencent.retchet.util;

import java.lang.reflect.Field;

import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.TypeInsnNode;

/**
 * ASM操作相关
 * @author noverguo
 */
public class ASMUtils implements Opcodes {
	public static int getCastOpByTypes(Type src, Type dist) {
		int op = -1;
		if(!src.equals(dist)) {
			String opName = src.getDescriptor() + "2" + dist.getDescriptor();
			try {
				Field field = ASMUtils.class.getField(opName);
				op = (Integer) field.get(null);
			} catch (Exception e) {
			}
		}
		return op;
	}
	public static AbstractInsnNode getCastNodeByTypes(Type src, Type dist) {
		AbstractInsnNode ain = null;
		if(!src.equals(dist)) {
			if(castType(src).getSort() == Type.OBJECT) {
				return new TypeInsnNode(CHECKCAST, dist.getInternalName());
			} else {
				String opName = src.getDescriptor() + "2" + dist.getDescriptor();
				try {
					Field field = ASMUtils.class.getField(opName);
					int op = (Integer) field.get(null);
					ain = new InsnNode(op);
				} catch (Exception e) {
				}
			}
		}
		return ain;
	}
	public static Type castType(Type type) {
		switch(type.getSort()) {
		case Type.OBJECT:
		case Type.ARRAY:
			type = Type.getType(Object.class);
			break;
		}
		return type;
	}
	public static InsnNode newConstInsnNodeByType(Type type) {
		int op = -1;
		switch (type.getSort()) {
		case Type.BOOLEAN:
		case Type.BYTE:
		case Type.CHAR:
		case Type.SHORT:
		case Type.INT:
			op = ICONST_0;
			break;
		case Type.FLOAT:
			op = FCONST_0;
			break;
		case Type.DOUBLE:
			op = DCONST_0;
			break;
		case Type.LONG:
			op = LCONST_0;
			break;
		case Type.OBJECT:
		case Type.ARRAY:
			op = ACONST_NULL;
			break;
		}
		if(op != -1) {
			return new InsnNode(op);
		}
		return null;
	}
	public static String toString(ClassNode cn, MethodNode mn) {
		return toString(cn.name, mn.name, mn.desc);
	}

	public static String toString(MethodInsnNode min) {
		return toString(min.owner, min.name, min.desc);
	}
	public static String toString(String clsName, String methodName, String methodDesc) {
		return clsName + "." + methodName + methodDesc;
	}
}
