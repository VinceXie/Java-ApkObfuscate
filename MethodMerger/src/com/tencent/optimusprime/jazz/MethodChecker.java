package com.tencent.optimusprime.jazz;

import java.util.List;
import java.util.Map;

import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodNode;

import com.tencent.jazz.model.ClassMethodInfo;
import com.tencent.jazz.util.CommonUtils;
import com.tencent.optimusprime.MethodInfo;

/**
 * 方法检查
 * @author noverguo
 */
public class MethodChecker implements Opcodes {

	public static boolean check(MethodInfo mi) {
		if(hasAnnotation(mi.methodNode)) {
			return false;
		}
		if(isInit(mi)) {
			return false;
		}
		if(isAbstract(mi)) {
			return false;
		}
		if(!checkClassVersion(TypeConvertor.get(mi))) {
			return false;
		}
		return true;
	}
	public static boolean isInit(MethodInfo mi) {
		return checkIsInit(mi.methodNode);
	}
	public static boolean checkIsInit(MethodNode mi) {
		if(mi.name.contains("<")) {
			return true;
		}
		return false;
	}
	public static boolean isAbstract(MethodInfo mi) {
		return isAbstract(mi.methodNode);
	}
	public static boolean isAbstract(MethodNode mn) {
		return (mn.access & ACC_ABSTRACT) == ACC_ABSTRACT;
	}
	
	/**
	 * 检查自身、Caller以及Invoker的编译版本是否合格
	 * @param cmi
	 * @return
	 */
	public static boolean checkClassVersion(ClassMethodInfo cmi) {
		if(!atLeast1_5ClassVersion(cmi)) {
			return false;
		}
		if(!atLeast1_5ClassVersion(cmi.invokerMethodMap)) {
			return false;
		}
		return true;
	}
	/**
	 * 编译版本至少JDK1.5以上
	 * @param cn
	 * @return
	 */
	public static boolean atLeast1_5ClassVersion(ClassNode cn) {
		int version = cn.version;
		// 45.3(196653) -> 1.1
		// 48->1.4 49->1.5 50->1.6 51->1.7 52->1.8
		// 兼容至java8
		return version >= 49 && version < 53;
	}
	private static boolean atLeast1_5ClassVersion(ClassMethodInfo cmi) {
		return atLeast1_5ClassVersion(cmi.mClassNode);
	}
	private static boolean atLeast1_5ClassVersion(Map<String, ClassMethodInfo> map) {
		if(!CommonUtils.isEmpty(map)) {
			for(ClassMethodInfo cmi : map.values()) {
				if(!atLeast1_5ClassVersion(cmi)) {
					return false;
				}
			}
		}
		return true;
	}
	
	private static final String[] grepNames = {"activity", "service", "receiver", "provider"};
	/**
	 * 这里限定四大组件的类皆包含各自的英文名
	 * @param cn
	 * @return
	 */
	public static boolean isComponent(ClassNode cn) {
		String name = cn.name.toLowerCase();
		for(String grepName : grepNames) {
			if(name.contains(grepName)) {
				return true;
			}
		}
		return false;
	}
	
	/**
	 * 包含<clinit>静态初始化代码
	 * @param classNode
	 * @return
	 */
	public static boolean hasCinit(ClassNode classNode) {
		List<MethodNode> mnList = classNode.methods;
		for(MethodNode mn : mnList) {
			if(mn.name.equals("<clinit>")) {
				return true;
			}
		}
		return false;
	}
	
	public static boolean hasAnnotation(MethodNode mn) {
		return CommonUtils.getSize(mn.visibleAnnotations) > 0 || CommonUtils.getSize(mn.visibleParameterAnnotations) > 0
				|| CommonUtils.getSize(mn.invisibleAnnotations) > 0 || CommonUtils.getSize(mn.invisibleParameterAnnotations) > 0;
	}
}
