package com.tencent.jazz.tools;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;

import com.tencent.jazz.model.ClassMethodInfo;
import com.tencent.jazz.util.CommonUtils;
import com.tencent.retchet.MergerMap;

/**
 * 更改方法的Access
 * @author noverguo
 */
public class ChangeModifyTools implements Opcodes {

	/**
	 * 把实例方法转为私有的静态方法
	 * @param beChangedMethodList
	 * @param callerList
	 * @return
	 */
	public static void changeInstance2PrivateStatic(List<ClassMethodInfo> beChangedMethodList, Collection<ClassMethodInfo> callerList) {
		changeInstance2PrivateStatic(beChangedMethodList, callerList, null);
	}
	public static void changeInstance2PrivateStatic(List<ClassMethodInfo> beChangedMethodList, Collection<ClassMethodInfo> callerList, String newName) {
		if(beChangedMethodList == null || beChangedMethodList.size() == 0) {
			return;
		}
		int size = beChangedMethodList.size();
		// key:旧方法的的唯一标识cn.name + mn.name + mn.desc;  value:新方法的desc
		Map<String, MethodNode> oldNewDescMap = new HashMap<String, MethodNode>();
		for(int i=0;i<size;++i) {
			ClassMethodInfo cmi = beChangedMethodList.get(i);
			ClassNode cn = cmi.mClassNode;
			MethodNode mn = cmi.mMethodNode;
			String oldClassMethodKey = cmi.toString();
			// Lcom/tencent/panda/test/Panda;
			String clsName = "L" + cn.name + ";";
			String desc = mn.desc;
			// 加入this的参数
			// (Ljava/lang/Object;I)V  -->   (Lcom/tencent/panda/test/Panda;Ljava/lang/Object;I)V
			desc = "(" + clsName + desc.substring(1);
			mn.desc = desc;
			String methodName = newName;
			if(methodName == null) {
				// 更改一下方法名，以免出现重复
				methodName = mn.name + MergerMap.MagicNum;
			}
			mn.name = methodName;
			// instance -->  private static
			mn.access = (mn.access & (~ACC_PRIVATE) & (~ACC_PROTECTED) & (~ACC_PUBLIC)) | ACC_PRIVATE | ACC_STATIC;
			oldNewDescMap.put(oldClassMethodKey, mn);
			// 记录一下
			MergerMap.getInstant().recordBeforeMerge(cn, mn);
		}
		if(!CommonUtils.isEmpty(callerList)) {
			// 修复调用旧方法的地方
			for(ClassMethodInfo callerInfo : callerList) {
				MethodNode callerMethodNode = callerInfo.mMethodNode;
				InsnList oldInsn = callerMethodNode.instructions;
				// 对所有指令进行遍历
				for(AbstractInsnNode p=oldInsn.getFirst();p!=null;p=p.getNext()){
					// 找到调用方法的指令
					if(p.getType() == AbstractInsnNode.METHOD_INSN) {
						MethodInsnNode min = (MethodInsnNode) p;
						String key = ClassMethodInfo.toString(min);
						if(oldNewDescMap.containsKey(key)) {
							// 把旧的调用关系改为新的即可
							min.setOpcode(INVOKESTATIC);
							MethodNode mn = oldNewDescMap.get(key);
							min.name = mn.name;
							min.desc = mn.desc;
						}
					}
				}
			}
		}
	}
	/**
	 * 把方法访问权限修改为公有的方法
	 * @param beChangedMethodList
	 * @param callerList
	 * @return
	 */
	public static void changeAccess2Public(List<ClassMethodInfo> beChangedMethodList) {
		changeAccess(beChangedMethodList, ACC_PUBLIC);
	}
	/**
	 * 把方法访问权限修改为私有的方法
	 * @param beChangedMethodList
	 * @param callerList
	 * @return
	 */
	public static void changeAccess2Private(List<ClassMethodInfo> beChangedMethodList) {
		changeAccess(beChangedMethodList, ACC_PRIVATE);
	}
	/**
	 * 把方法访问权限修改为公有的方法
	 * @param beChangedMethodList
	 * @param callerList
	 * @return
	 */
	public static void changeAccess(List<ClassMethodInfo> beChangedMethodList, int access) {
		if(beChangedMethodList == null || beChangedMethodList.size() == 0) {
			return;
		}
		int size = beChangedMethodList.size();
		// key:旧方法的的唯一标识cn.name + mn.name + mn.desc;  value:新方法的desc
		for(int i=0;i<size;++i) {
			ClassMethodInfo cmi = beChangedMethodList.get(i);
			MethodNode mn = cmi.mMethodNode;
			mn.access = (mn.access & (~ACC_PRIVATE) & (~ACC_PROTECTED) & (~ACC_PUBLIC)) | access;
		}
	}
}
