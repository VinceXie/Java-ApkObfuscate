package com.tencent.jazz.util;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.objectweb.asm.LengthClassVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodNode;

import com.tencent.jazz.model.ClassMethodInfo;
import com.tencent.retchet.util.ASMUtils;

/**
 * ASM操作相关
 * @author noverguo
 */
public class ASMTools extends ASMUtils implements Opcodes {
	public static Set<String> allMethodNames = new HashSet<String>();
	public static int computeLength(List<ClassMethodInfo> cmis) throws Exception {
		int count = 0;
		if(cmis.size() > 0) {
			for(ClassMethodInfo cmi : cmis) {
				count += getLength(cmi);
			}
		}
		return count;
	}

	public static int getLength(ClassMethodInfo cmi) throws Exception {
		return getLength(cmi.mClassNode, cmi.mMethodNode);
	}
	public static int getLength(ClassNode cn, MethodNode mn) throws Exception {
		return new LengthClassVisitor(cn, mn).getLength();
	}
	public static int computeAfterMergeLengthOld(List<ClassMethodInfo> cmis) throws Exception {
		// 旧写法
		int res = computeLength(cmis) + getMaxByteCount(ILOAD) + getMaxByteCountByTableSwitch(cmis.size()) + 
				getMaxByteCount(NEW) + getMaxByteCount(DUP) + getMaxByteCount(INVOKESPECIAL) + getMaxByteCount(ATHROW);
		for(int i=0;i<cmis.size();++i) {
			Type[] types = Type.getArgumentTypes(cmis.get(i).mMethodNode.desc);
			if(!CommonUtils.isEmpty(types)) {
				int len = types.length;
				res += len * getMaxByteCount(ILOAD);
				res += len * getMaxByteCount(ISTORE);
				for(Type type : types) {
					if(castType(type).getSort() == Type.OBJECT) {
						res += getMaxByteCount(CHECKCAST);
					}
				}
			}
		}
		return res;
	}
	public static int computeAfterMergeLengthNew(List<ClassMethodInfo> cmis) throws Exception {
		return computeLength(cmis) + getMaxByteCount(ILOAD) + getMaxByteCountByTableSwitch(cmis.size()) + 
				getMaxByteCount(NEW) + getMaxByteCount(DUP) + getMaxByteCount(INVOKESPECIAL) + getMaxByteCount(ATHROW);
	}
	public static int getMaxByteCountByTableSwitch(int labelCount) {
		// TableSwitch: 1 + 4 + 4(dflt) + 8(min,max) + 4*labels.length
		return 1 + 4 + 8 + 4 * (labelCount + 1);
	}
	public static int getMaxByteCount(int op) {
		// XCONST: 1 LDC: 3    XLOAD XSTORE: 4  CHECKCAST: 3
		// INVOKEINTERFACE: 5  other invoke: 3
		int res = 4;
		if(op < DCONST_1 || op == ATHROW || op == DUP) {
			res = 1;
		} else if(op == LDC || op == CHECKCAST || op == NEW || op == INVOKESPECIAL) {
			res = 3;
		} else if(between(op, ILOAD, ALOAD) || between(op, ISTORE, ASTORE)) {
			res = 4;
		}
		return res;
	}

	private static boolean between(int op, int min, int max) {
		return op >= min && op <= max;
	}
	
	public static <T> void  change(T[] arr, int first, int second) {
		T tmp = arr[first];
		arr[first] = arr[second];
		arr[second] = tmp;
	}
	
	/**
	 * 比较两个MethodInfo之间的大小
	 * @param first
	 * @param second
	 * @return
	 */
	public static int compare(MethodNode first, MethodNode second) {
		Type[] firstArgsType = Type.getArgumentTypes(first.desc);
		Type[] secondArgsType = Type.getArgumentTypes(second.desc);
		if(CommonUtils.isEmpty(firstArgsType)) {
			if(CommonUtils.isEmpty(secondArgsType)) {
				return 0;
			} else {
				return -1;
			}
		} else {
			if(CommonUtils.isEmpty(secondArgsType)) {
				return 1;
			} else {
				int len = Math.min(firstArgsType.length, secondArgsType.length);
				for(int i=0;i<len;++i) {
					int val = compare(ASMUtils.castType(firstArgsType[i]), ASMUtils.castType(secondArgsType[i]));
					if(val != 0) {
						return val;
					}
				}
				if(firstArgsType.length == secondArgsType.length) {
					return 0;
				} else if(firstArgsType.length > secondArgsType.length) {
					return 1;
				} else {
					return -1;
				}
			}
		}
	}
	
	public static int compare(Type firstType, Type secondType) {
		return firstType.getSort() - secondType.getSort(); 
	}
	
	public static boolean startWiths(MethodNode first, MethodNode second) {
		Type[] firstArgsType = Type.getArgumentTypes(first.desc);
		Type[] secondArgsType = Type.getArgumentTypes(second.desc);
		if(CommonUtils.isEmpty(secondArgsType)) {
			return true;
		}
		if(CommonUtils.isEmpty(firstArgsType)) {
			return false;
		}
		if(firstArgsType.length < secondArgsType.length) {
			return false;
		}
		int len = secondArgsType.length;
		for(int i=0;i<len;++i) {
			if(compare(ASMUtils.castType(firstArgsType[i]), ASMUtils.castType(secondArgsType[i])) != 0) {
				return false;
			}
		}
		return true;
	}
	public static String toMethodNameString(String className, String methodName, String methodDesc) {
		String value = ASMUtils.toString(className, methodName, methodDesc);
		return value.substring(0, value.lastIndexOf(')') + 1);
	}
	public static String makeSureMethodNameUnExist(String className, String methodName, String methodDesc) {
		while(allMethodNames.contains(toMethodNameString(className, methodName, methodDesc))) {
			char c = methodName.charAt(methodName.length()-1);
			if(c < 'a' || c >= 'z') {
				methodName += 'a';
			} else {
				methodName = methodName.substring(0, methodName.length()-1) + (char)(c + 1);
			}
		}
		
		allMethodNames.add(toMethodNameString(className, methodName, methodDesc));
		return methodName;
	}

	public static String toMethodNameString(ClassNode cn, MethodNode mn) {
		return toMethodNameString(cn.name, mn.name, mn.desc);
	}
}
