package com.tencent.jazz.tools;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.LdcInsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.VarInsnNode;

import com.tencent.jazz.model.ClassFieldInfo;
import com.tencent.jazz.model.ClassMethodInfo;
import com.tencent.jazz.util.ASMTools;
import com.tencent.jazz.util.CommonUtils;
import com.tencent.retchet.MergerMap;
import com.tencent.retchet.NewOldMethodArgMapInfo;
import com.tencent.retchet.NewOldMethodArgMapInfo.IndexTypeMap;
import com.tencent.retchet.Record;
import com.tencent.retchet.util.ASMUtils;

/**
 * 方法合并的核心实现
 * @author noverguo
 */
public class MergeMethodTools implements Opcodes {

	private static class IsolateMethodVisitor extends MethodVisitor {
		private Map<String, ClassMethodInfo> invokerMethodMap;

		private IsolateMethodVisitor(int api, MethodVisitor mv, Map<String, ClassMethodInfo> invokerMethodMap) {
			super(api, mv);
			this.invokerMethodMap = invokerMethodMap;
		}

		@Override
		public void visitMethodInsn(int opcode, String owner, String name, String desc, boolean itf) {
			// 调用private方法改为调用public方法，也就是说后面会把所以调用的方法的权限改为public
			if (opcode == INVOKESPECIAL && !name.startsWith("<")) {
				String key = owner + "." + name + desc;
				// 只处理私有方法
				if (invokerMethodMap.containsKey(key) && (invokerMethodMap.get(key).mMethodNode.access & ACC_PRIVATE) != 0) {
					opcode = INVOKEVIRTUAL;
				}
			}
			super.visitMethodInsn(opcode, owner, name, desc, itf);
		}
	}
	
	private static int getTypeSize(ClassMethodInfo classMethodInfo) {
		return CommonUtils.getSize(Type.getArgumentTypes(classMethodInfo.mMethodNode.desc));
	}

	public static void mergeMethod(ClassMethodInfo newClassMethodInfo, List<ClassMethodInfo> beMergeMethodList, List<Record> records) throws IOException {
		Record recordForNew = records.get(0);
		// 新方法保存的类
		ClassNode newMethodClass = newClassMethodInfo.mClassNode;
		// 旧方法的映射表，用于快速查找
		// 新方法需要为public，不然其它类访问不了~
		newMethodClass.access = newMethodClass.access & (~ACC_PRIVATE) & (~ACC_PROTECTED) | ACC_PUBLIC;
		// 新方法的异常列表，异常列表不应该出现重复
		Set<String> newMethodExceptions = new HashSet<String>();
		// 新方法的返回值类型
		final Type newMethodRetType = Type.getReturnType(recordForNew.newMethodDesc);
		// 被合并方法的大小
		int methodSize = beMergeMethodList.size();
		// 新方法的名称
		String newMethodName = recordForNew.newMethodName;
		
		initAndSetFieldToPublic(beMergeMethodList, newMethodExceptions);
		// 得到新方法的参数列表，这里需要把第一个参数I去掉
		List<Type> newMethodArgTypes = Arrays.asList(Type.getArgumentTypes(recordForNew.newMethodDesc.replaceFirst("I", "")));
		// 新方法的描述，其中限定第一个参数为which，代表调用的是哪个方法
		String newMethodDesc = recordForNew.newMethodDesc;
		
		// 建立新旧方法的参数关系映射表
		Map<String, NewOldMethodArgMapInfo> argsMap = new HashMap<String, NewOldMethodArgMapInfo>();
		for(Record record : records) {
			argsMap.put(record.getOldKey(), record.argInfo);
		}
		// 在存放新方法的类中创建该方法，并限定该方法为静态方法
		final MethodNode newMethodNode = (MethodNode) newMethodClass.visitMethod(ACC_STATIC & (~ACC_PRIVATE) & (~ACC_PROTECTED) | ACC_PUBLIC, newMethodName, newMethodDesc, null,
				newMethodExceptions.toArray(new String[newMethodExceptions.size()]));
		// 新的方法与其所在类的关系信息
		newClassMethodInfo.mMethodNode = (MethodNode) newMethodNode;
		
		Map<String, ClassMethodInfo> callerMap = newClassMethodInfo.callerMap;
		Map<String, ClassMethodInfo> invokerMethodMap = newClassMethodInfo.invokerMethodMap;
		Map<String, ClassFieldInfo> invokerFieldMap = newClassMethodInfo.invokerFieldMap;
		for (int i = 0; i < methodSize; ++i) {
			// 取得当前待合并的方法
			ClassMethodInfo oldClassMethodInfo = beMergeMethodList.get(i);
			// 把旧方法的caller、invokerMethod、invokerField汇总到新方法关系信息中
			callerMap.putAll(oldClassMethodInfo.callerMap);
			invokerMethodMap.putAll(oldClassMethodInfo.invokerMethodMap);
			invokerFieldMap.putAll(oldClassMethodInfo.invokerFieldMap);
		}
		
		// record作用位置
		recordBeforeMerge(newClassMethodInfo, beMergeMethodList, argsMap, newMethodNode);
		// 输出所有旧方法记录
		MethodRecordPrinter.printOldMethodRecord(newClassMethodInfo, beMergeMethodList);
		
		mergeCodesToNewMethod(beMergeMethodList, newMethodClass, argsMap, newMethodNode, newClassMethodInfo);

		// TODO 对调用者进行修复
		fixCaller(newMethodClass, beMergeMethodList, newMethodRetType, newMethodArgTypes, argsMap, newMethodNode, newClassMethodInfo.callerMap);

		// 把旧方法所执行的方法的类及其方法 的访问权限设置为public,而其caller中调用该方法的地方，如果为private，则进行修复
		fixInvokerCaller(newClassMethodInfo.invokerMethodMap);

		fixInvokeField(newClassMethodInfo.invokerFieldMap);
		// 记录新方法
		MethodRecordPrinter.printNewMethodRecord(newClassMethodInfo);
	}
	
	/**
	 * 把多个方法合并成一个方法，并对调用该方法的方法进行修复
	 * 
	 * @param access
	 *            待合并及新方法的访问类型
	 * @param beMergeMethodList
	 *            待合并的方法列表
	 * @return 修改后的ClassNode集合
	 * @throws IOException
	 */
	public static ClassMethodInfo mergeMethod(List<ClassMethodInfo> beMergeMethodList) throws IOException {
		if (beMergeMethodList == null || beMergeMethodList.size() == 0) {
			return null;
		}
		// 只有一个函数就没必要合并了
		if (beMergeMethodList.size() == 1) {
			return beMergeMethodList.get(0);
		}
		// 新方法保存的类
		ClassNode newMethodClass = beMergeMethodList.get(0).mClassNode;
		// 新方法需要为public，不然其它类访问不了~
		newMethodClass.access = newMethodClass.access & (~ACC_PRIVATE) & (~ACC_PROTECTED) | ACC_PUBLIC;
		// 新方法的异常列表，异常列表不应该出现重复
		Set<String> newMethodExceptions = new HashSet<String>();
		// 新方法的返回值类型
		final Type newMethodRetType = getNewRetType(beMergeMethodList);
		// 被合并方法的大小
		int methodSize = beMergeMethodList.size();
		// 设置被合并的方法的所有成员变量为public
		initAndSetFieldToPublic(beMergeMethodList, newMethodExceptions);
		// 得到新方法的参数列表
		List<Type> newMethodArgTypes = getArgTypes(beMergeMethodList);
		
		// 新方法的描述，其中限定第一个参数为which，代表调用的是哪个方法
		String newMethodDesc = "(I" + getArgTypesStr(newMethodArgTypes) + ")" + newMethodRetType.getDescriptor();
		// 新方法的名称
		String newMethodName = "_" + beMergeMethodList.get(0).mMethodNode.name.replaceFirst(MergerMap.MagicNum, "");
		
		// 保证新方法名称唯一
		newMethodName = ASMTools.makeSureMethodNameUnExist(newMethodClass.name, newMethodName, newMethodDesc);
		// 建立新旧方法的参数关系映射表
		Map<String, NewOldMethodArgMapInfo> argsMap = buildNewOldMethodArgMap(beMergeMethodList, newMethodArgTypes);
		// 在存放新方法的类中创建该方法，并限定该方法为静态方法
		final MethodNode newMethodNode = (MethodNode) newMethodClass.visitMethod(ACC_STATIC | ACC_PUBLIC, newMethodName, newMethodDesc, null,
				newMethodExceptions.toArray(new String[newMethodExceptions.size()]));
		
		// 新的方法与其所在类的关系信息
		ClassMethodInfo newClassMethodInfo = new ClassMethodInfo(newMethodClass, (MethodNode) newMethodNode);
		Map<String, ClassMethodInfo> callerMap = newClassMethodInfo.callerMap;
		Map<String, ClassMethodInfo> invokerMethodMap = newClassMethodInfo.invokerMethodMap;
		Map<String, ClassFieldInfo> invokerFieldMap = newClassMethodInfo.invokerFieldMap;
		for (int i = 0; i < methodSize; ++i) {
			// 取得当前待合并的方法
			ClassMethodInfo oldClassMethodInfo = beMergeMethodList.get(i);
			// 把旧方法的caller、invokerMethod、invokerField汇总到新方法关系信息中
			callerMap.putAll(oldClassMethodInfo.callerMap);
			invokerMethodMap.putAll(oldClassMethodInfo.invokerMethodMap);
			invokerFieldMap.putAll(oldClassMethodInfo.invokerFieldMap);
		}
		
		recordBeforeMerge(newClassMethodInfo, beMergeMethodList, argsMap, newMethodNode);
		// ------------------record end------------------------
		// 输出所有旧方法记录
		MethodRecordPrinter.printOldMethodRecord(newClassMethodInfo, beMergeMethodList);
		
		mergeCodesToNewMethod(beMergeMethodList, newMethodClass, argsMap, newMethodNode, newClassMethodInfo);

		// TODO 对调用者进行修复
		fixCaller(newMethodClass, beMergeMethodList, newMethodRetType, newMethodArgTypes, argsMap, newMethodNode, newClassMethodInfo.callerMap);

		// 把旧方法所执行的方法的类及其方法 的访问权限设置为public,而其caller中调用该方法的地方，如果为private，则进行修复
		fixInvokerCaller(newClassMethodInfo.invokerMethodMap);

		fixInvokeField(newClassMethodInfo.invokerFieldMap);
		// 记录新方法
		MethodRecordPrinter.printNewMethodRecord(newClassMethodInfo);
		return newClassMethodInfo;
	}
	
	private static void mergeCodesToNewMethod(List<ClassMethodInfo> beMergeMethodList, ClassNode newMethodClass, Map<String, NewOldMethodArgMapInfo> argsMap, final MethodNode newMethodNode,
			ClassMethodInfo newClassMethodInfo) {
		Map<String, ClassMethodInfo> callerMap = newClassMethodInfo.callerMap;
		Map<String, ClassMethodInfo> invokerMethodMap = newClassMethodInfo.invokerMethodMap;
		int methodSize = beMergeMethodList.size();
		Label[] labels = new Label[methodSize];
		for (int i = 0; i < methodSize; ++i) {
			labels[i] = new Label();
		}
		// 先加载第一个参数，用于接下来的switch语句
		newMethodNode.visitVarInsn(ILOAD, 0);
		Label defaultLabel = new Label();
		newMethodNode.visitTableSwitchInsn(0, methodSize - 1, defaultLabel, labels);
		for (int i = 0; i < methodSize; ++i) {
			// 取得当前待合并的方法
			ClassMethodInfo oldClassMethodInfo = beMergeMethodList.get(i);

			// case i:
			newMethodNode.visitLabel(labels[i]);
			MethodNode curNode = oldClassMethodInfo.mMethodNode;
			// 移除对应类中的方法
			oldClassMethodInfo.mClassNode.methods.remove(curNode);

			// key直接使用toString得到,以保证统一
			String key = oldClassMethodInfo.toString();
			final NewOldMethodArgMapInfo newOldMethodArgMapInfo = argsMap.get(key);
			final int oldMethodArgSize = CommonUtils.getSize(newOldMethodArgMapInfo.mMap);
			if (oldMethodArgSize > 0) {
				// ---------------fix arg start-----------------
				// method_old(IJ) -> method_new(Ljava/lang/Object;JI)
				// -> ILOAD 3; LLOAD 1
				// -> LSTORE 1; ISTORE 0
				// 先把旧方法用到的参数依次压入栈顶
				for (int j = 0; j < oldMethodArgSize; j++) {
					IndexTypeMap indexTypeMap = newOldMethodArgMapInfo.mMap[j];
					if (indexTypeMap == null) {
						// 应该永远不会进到这里
						throw new RuntimeException("build new and old method argument map failed !!");
					}
					int op = indexTypeMap.mNewType.getOpcode(ILOAD);
					newMethodNode.visitVarInsn(op, indexTypeMap.mNewPos);
					// 当为对象类型时，如果类型不一致，则需要进行强制类型转制
					if (!indexTypeMap.mNewType.equals(indexTypeMap.mOldType)) {
						if (indexTypeMap.mNewType.getSort() == Type.OBJECT) {
							// TODO 对象类型
							newMethodNode.visitTypeInsn(CHECKCAST, indexTypeMap.mOldType.getInternalName());
						} else {
							// 基本数据类型
							op = ASMUtils.getCastOpByTypes(indexTypeMap.mNewType, indexTypeMap.mOldType);
							if (op != -1) {
								newMethodNode.visitInsn(op);
							}
						}
					}
				}
				// 把刚才压入栈中的数据按旧方法的参数反序还原
				for (int j = oldMethodArgSize - 1; j >= 0; j--) {
					IndexTypeMap indexTypeMap = newOldMethodArgMapInfo.mMap[j];
					int op = indexTypeMap.mOldType.getOpcode(ISTORE);
					newMethodNode.visitVarInsn(op, indexTypeMap.mOldPos);
				}
				// ---------------fix arg end-----------------
			}
			// 处理原始代码，直接拷贝一份就行
			curNode.accept(new IsolateMethodVisitor(ASM5, newMethodNode, invokerMethodMap));
			if (callerMap != null && callerMap.size() > 0) {
				String oldClassMethodKey = oldClassMethodInfo.toString();
				String newClassMethodKey = newClassMethodInfo.toString();
				// 修复调用者关系表，如果调用者中含当前合并的方法，则替换成新的方法
				if (callerMap.containsKey(oldClassMethodKey)) {
					callerMap.remove(oldClassMethodKey);
					if (!callerMap.containsKey(newClassMethodKey)) {
						callerMap.put(newClassMethodKey, newClassMethodInfo);
					}
				}
			}
			if (invokerMethodMap != null && invokerMethodMap.size() > 0) {
				String oldClassMethodKey = oldClassMethodInfo.toString();
				String newClassMethodKey = newClassMethodInfo.toString();
				// 修复执行方法关系表，如果执行方法中含当前合并的方法，则替换成新的方法
				if (invokerMethodMap.containsKey(oldClassMethodKey)) {
					invokerMethodMap.remove(oldClassMethodKey);
					if (!invokerMethodMap.containsKey(newClassMethodKey)) {
						invokerMethodMap.put(newClassMethodKey, newClassMethodInfo);
					}
				}
			}
		}
		newMethodNode.maxLocals = calculateNewMaxLocal(newMethodClass, newMethodNode);

		// 默认处理为抛出异常，理论上不会进入到默认处理
		defaultCode(newMethodNode, defaultLabel);
	}
	private static void initAndSetFieldToPublic(List<ClassMethodInfo> beMergeMethodList, Set<String> newMethodExceptions) {
		for (int i = 0; i < beMergeMethodList.size(); ++i) {
			ClassMethodInfo classMethodInfo = beMergeMethodList.get(i);
			MethodNode curMethodNode = classMethodInfo.mMethodNode;
			newMethodExceptions.addAll(curMethodNode.exceptions);
			// 把该类的所有成员的访问权限设置为public的，以便合并后的方法能够访问
			List<FieldNode> fields = classMethodInfo.mClassNode.fields;
			if (fields != null && fields.size() > 0) {
				for (FieldNode fieldNode : fields) {
					fieldNode.access = fieldNode.access & (~ACC_PRIVATE) & (~ACC_PROTECTED) | ACC_PUBLIC;
				}
			}
		}
	}
	private static void fixInvokeField(Map<String, ClassFieldInfo> invokerFieldMap) {
		// 把旧方法所使用属性的类及其方法 的访问权限设置为public
		if (invokerFieldMap != null && invokerFieldMap.size() > 0) {
			for (Entry<String, ClassFieldInfo> entry : invokerFieldMap.entrySet()) {
				ClassFieldInfo cfi = entry.getValue();
				cfi.mClassNode.access = cfi.mClassNode.access & (~ACC_PRIVATE) & (~ACC_PROTECTED) | ACC_PUBLIC;
				cfi.mFieldNode.access = cfi.mFieldNode.access & (~ACC_PRIVATE) & (~ACC_PROTECTED) | ACC_PUBLIC;
			}
		}
	}
	private static void recordBeforeMerge(ClassMethodInfo newClassMethodInfo, List<ClassMethodInfo> beMergeMethodList, Map<String, NewOldMethodArgMapInfo> argsMap, final MethodNode newMethodNode) {
		ArrayList<MethodNode> methodNodes = new ArrayList<MethodNode>();
		ArrayList<ClassNode> classNodes = new ArrayList<ClassNode>();
		for (ClassMethodInfo classMethodInfo : beMergeMethodList) {
			methodNodes.add(classMethodInfo.mMethodNode);
			classNodes.add(classMethodInfo.mClassNode);
		}
		// 合并前记录
		MergerMap.getInstant().recordBeforeMerge(newClassMethodInfo.mClassNode, newMethodNode, classNodes, methodNodes, argsMap);
	}
	private static void defaultCode(final MethodNode newMethodNode, Label defaultLabel) {
		newMethodNode.visitLabel(defaultLabel);
		newMethodNode.visitTypeInsn(NEW, "java/lang/RuntimeException");
		newMethodNode.visitInsn(DUP);
		newMethodNode.visitMethodInsn(INVOKESPECIAL, "java/lang/RuntimeException", "<init>", "()V", false);
		newMethodNode.visitInsn(ATHROW);
		newMethodNode.visitEnd();
	}
	public static void fixCallerV2(ClassNode newMethodClass, MethodNode newMethodNode, List<ClassMethodInfo> beMergeMethodList, 
			Type newMethodRetType, List<Type> newMethodArgTypes, Map<String, ClassMethodInfo> callerMap) {
		// 旧方法的映射表，用于快速查找
		Map<String, ClassMethodInfo> oldMethodMap = new HashMap<String, ClassMethodInfo>();
		for (int i = 0; i < beMergeMethodList.size(); ++i) {
			ClassMethodInfo cmi = beMergeMethodList.get(i);
			oldMethodMap.put(cmi.toString(), cmi);
		}
		// 旧方法位置的映射表，用于快速查找
		Map<String, Integer> oldMethodIndexMap = new HashMap<String, Integer>();
		for (int i = 0; i < beMergeMethodList.size(); ++i) {
			ClassMethodInfo cmi = beMergeMethodList.get(i);
			oldMethodIndexMap.put(cmi.toString(), i);
		}
		
		if (!CommonUtils.isEmpty(callerMap)) {
			int newMethodArgSize = newMethodArgTypes.size();
			// 修复调用旧方法的地方
			for (Entry<String, ClassMethodInfo> entry : callerMap.entrySet()) {
				ClassMethodInfo callerInfo = entry.getValue();
				MethodNode callerMethodNode = callerInfo.mMethodNode;
				InsnList oldInsn = callerMethodNode.instructions;
				int callerMaxLocal = callerMethodNode.maxLocals + 1;
				// 对所有指令进行遍历
				for (AbstractInsnNode p = oldInsn.getFirst(); p != null; p = p.getNext()) {
					// 找到调用方法的指令
					if (p.getType() == AbstractInsnNode.METHOD_INSN) {
						MethodInsnNode min = (MethodInsnNode) p;
						String key = ClassMethodInfo.toString(min);
						if (oldMethodMap.containsKey(key)) {
							ClassMethodInfo oldMethodInfo = oldMethodMap.get(key);
							int oldMethodArgSize = getTypeSize(oldMethodInfo);
							InsnList newInsn = new InsnList();
							// 取得caller方法的本地变量大小，在修复中会用到一些本地变量，这里直接增加新本地变量，这样就不会影响到原方法
							int nextLocal = callerMethodNode.maxLocals + 1;
							// 前面的参数一致，但可能会增加参数，因此需要把增加的一些无用参数加上
							for(int i=oldMethodArgSize;i<newMethodArgSize;++i) {
								Type type = newMethodArgTypes.get(i);
								newInsn.add(ASMUtils.newConstInsnNodeByType(type));
								nextLocal += type.getSize();
							}
							// 取修复调用时最大的Local值
							if (callerMaxLocal < nextLocal) {
								callerMaxLocal = nextLocal;
							}
							// 指定使用的是哪个旧方法
							newInsn.add(new LdcInsnNode(oldMethodIndexMap.get(key)));
							
							// 插入修复压入参数的指令
							oldInsn.insertBefore(p, newInsn);
							// 插入新方法的调用
							oldInsn.insertBefore(p, new MethodInsnNode(INVOKESTATIC, newMethodClass.name, newMethodNode.name, newMethodNode.desc, false));
							Type oldRetType = Type.getReturnType(min.desc);
							// 返回类型不一致，则作对应的强制类型转换
							if (!newMethodRetType.equals(oldRetType)) {
								AbstractInsnNode castNode = ASMUtils.getCastNodeByTypes(newMethodRetType, oldRetType);
								if (castNode != null) {
									oldInsn.insertBefore(p, castNode);
								}
							}
							// 删除旧方法的调用
							AbstractInsnNode q = p.getPrevious();
							oldInsn.remove(p);
							p = q;
						}
					}
				}
				// 修复最大本地变量数
				callerMethodNode.maxLocals = callerMaxLocal;
			}
		}
	}
	private static void fixCaller(ClassNode newMethodClass, List<ClassMethodInfo> beMergeMethodList, final Type newMethodRetType, List<Type> newMethodArgTypes, Map<String, NewOldMethodArgMapInfo> argsMap, final MethodNode newMethodNode,
			Map<String, ClassMethodInfo> callerMap) {
		// 旧方法的映射表，用于快速查找
		Map<String, ClassMethodInfo> oldMethodMap = new HashMap<String, ClassMethodInfo>();
		for (int i = 0; i < beMergeMethodList.size(); ++i) {
			ClassMethodInfo cmi = beMergeMethodList.get(i);
			oldMethodMap.put(cmi.toString(), cmi);
		}
		if (!CommonUtils.isEmpty(callerMap)) {
			int newMethodArgSize = newMethodArgTypes.size();
			// 修复调用旧方法的地方
			for (Entry<String, ClassMethodInfo> entry : callerMap.entrySet()) {
				ClassMethodInfo callerInfo = entry.getValue();
				MethodNode callerMethodNode = callerInfo.mMethodNode;
				InsnList oldInsn = callerMethodNode.instructions;
				int callerMaxLocal = callerMethodNode.maxLocals + 1;
				// 对所有指令进行遍历
				for (AbstractInsnNode p = oldInsn.getFirst(); p != null; p = p.getNext()) {
					// 找到调用方法的指令
					if (p.getType() == AbstractInsnNode.METHOD_INSN) {
						MethodInsnNode min = (MethodInsnNode) p;
						String key = ClassMethodInfo.toString(min);
						if (oldMethodMap.containsKey(key)) {
							InsnList newInsn = new InsnList();
							// 取得caller方法的本地变量大小，在修复中会用到一些本地变量，这里直接增加新本地变量，这样就不会影响到原方法
							int nextLocal = callerMethodNode.maxLocals + 1;
							// 取得新旧方法的参数映射表
							NewOldMethodArgMapInfo newOldMethodArgMapInfo = argsMap.get(key);

							// 用于存放新方法调用参数的指令
							AbstractInsnNode[] newLoadArgNodes = new AbstractInsnNode[newMethodArgSize];
							int oldMethodArgSize = CommonUtils.getSize(newOldMethodArgMapInfo.mMap);
							if (oldMethodArgSize > 0) {
								// 把栈中的数据按旧方法的参数反序保存到新的local变量中
								for (int j = oldMethodArgSize - 1; j >= 0; j--) {
									IndexTypeMap indexTypeMap = newOldMethodArgMapInfo.mMap[j];
									int op = indexTypeMap.mOldType.getOpcode(ISTORE);
									newInsn.add(new VarInsnNode(op, nextLocal));
									nextLocal += indexTypeMap.mOldType.getSize();
								}
								// 取修复调用时最大的Local值
								if (callerMaxLocal < nextLocal) {
									callerMaxLocal = nextLocal;
								}
								// 把刚刚保存到新的local变量中的数据根据旧方法的参数顺序依次设置到新方法对应的压入参数指令中
								for (int j = 0; j < oldMethodArgSize; ++j) {
									IndexTypeMap indexTypeMap = newOldMethodArgMapInfo.mMap[j];
									int op = indexTypeMap.mOldType.getOpcode(ILOAD);
									nextLocal -= indexTypeMap.mOldType.getSize();
									newLoadArgNodes[indexTypeMap.mNewIndex] = new VarInsnNode(op, nextLocal);
								}
							}
							newInsn.add(new LdcInsnNode(newOldMethodArgMapInfo.mWhich));
							// 填充新方法相对旧方法多出来的参数
							for (int j = 0; j < newMethodArgSize; ++j) {
								if (newLoadArgNodes[j] == null) {
									newLoadArgNodes[j] = ASMUtils.newConstInsnNodeByType(newMethodArgTypes.get(j));
								}
								newInsn.add(newLoadArgNodes[j]);
							}

							// 插入修复压入参数的指令
							oldInsn.insertBefore(p, newInsn);
							// 插入新方法的调用
							oldInsn.insertBefore(p, new MethodInsnNode(INVOKESTATIC, newMethodClass.name, newMethodNode.name, newMethodNode.desc, false));
							Type oldRetType = Type.getReturnType(min.desc);
							// 返回类型不一致，则作对应的强制类型转换
							if (!newMethodRetType.equals(oldRetType)) {
								AbstractInsnNode castNode = ASMUtils.getCastNodeByTypes(newMethodRetType, oldRetType);
								if (castNode != null) {
									oldInsn.insertBefore(p, castNode);
								}
							}
							// 删除旧方法的调用
							AbstractInsnNode q = p.getPrevious();
							oldInsn.remove(p);
							p = q;
						}
					}
				}
				// 修复最大本地变量数
				callerMethodNode.maxLocals = callerMaxLocal;
			}
		}
	}

	/**
	 * 计算出MaxLocal的值
	 * 
	 * @param newMethodClass
	 * @param newMethodNode
	 * @return
	 */
	private static int calculateNewMaxLocal(ClassNode newMethodClass, MethodNode newMethodNode) {
		ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_MAXS);
		newMethodClass.accept(cw);
		ClassReader cr = new ClassReader(cw.toByteArray());
		ClassNode cn = new ClassNode();
		cr.accept(cn, 0);
		String name = newMethodNode.name;
		String desc = newMethodNode.desc;
		for (MethodNode mn : cn.methods) {
			if (mn.name.equals(name) && mn.desc.equals(desc)) {
				return mn.maxLocals;
			}
		}
		return newMethodNode.maxLocals;
	}

	private static Map<String, NewOldMethodArgMapInfo> buildNewOldMethodArgMap(List<ClassMethodInfo> beMergeMethodList, List<Type> newMethodArgTypes) {
		Map<String, NewOldMethodArgMapInfo> argMap = new HashMap<String, NewOldMethodArgMapInfo>();
		int newMethodTypeSize = newMethodArgTypes.size();
		int methodSize = beMergeMethodList.size();
		// 分别给每个待合并的方法建立新旧参数映射表
		for (int i = 0; i < methodSize; ++i) {
			ClassMethodInfo cmi = beMergeMethodList.get(i);
			Type[] oldMethodTypes = Type.getArgumentTypes(cmi.mMethodNode.desc);
			Type[] tmpOldMethodTypes = Type.getArgumentTypes(cmi.mMethodNode.desc);
			int oldMethodTypeSize = CommonUtils.getSize(oldMethodTypes);
			NewOldMethodArgMapInfo mapInfo = new NewOldMethodArgMapInfo(i, oldMethodTypeSize);
			argMap.put(cmi.toString(), mapInfo);
			if (oldMethodTypeSize > 0) {
				// 此时的i可作为which使用
				// 参数存放的位置，int占4个字节，long点8个字节
				// 第一个参数为which
				int newArgCurPos = 1;
				for (int j = 0; j < newMethodTypeSize; ++j) {
					Type newType = newMethodArgTypes.get(j);
					int oldArgCurPos = 0;
					for (int k = 0; k < oldMethodTypeSize; ++k) {
						Type oldType = tmpOldMethodTypes[k];
						// 当新旧参数匹配上，则为其建立映射关系
						if (oldType != null && ASMUtils.castType(oldType).getSort() == newType.getSort()) {
							// 建立映射关系
							mapInfo.setIndexType(k, k, oldArgCurPos, j, newArgCurPos, oldType, newType);
							// 置已建立映射关系的参数为空，以免重复映射
							tmpOldMethodTypes[k] = null;
							break;
						}
						oldArgCurPos += oldMethodTypes[k].getSize();
					}
					newArgCurPos += newType.getSize();
				}
			}
		}
		return argMap;
	}

	private static Type[] castTypes(Type[] argumentTypes) {
		int size = CommonUtils.getSize(argumentTypes);
		if (size > 0) {
			for (int i = 0; i < size; ++i) {
				argumentTypes[i] = ASMUtils.castType(argumentTypes[i]);
			}
		}
		return argumentTypes;
	}

	/**
	 * 得到共用参数
	 */
	private static List<Type> getArgTypes(List<ClassMethodInfo> methodList) {
		int size = methodList.size();
		Type[] startTypes = null;
		int startIndex = 0;
		// 找到一个参数列表大于0的作为起始参数列表
		for (int i = 0; i < size; ++i) {
			startTypes = castTypes(Type.getArgumentTypes(methodList.get(i).mMethodNode.desc));
			if (CommonUtils.getSize(startTypes) > 0) {
				startIndex = i + 1;
				break;
			}
		}
		List<Type> allTypes = new ArrayList<Type>(Arrays.asList(startTypes));
		// 合并可共用参数
		for (int i = startIndex; i < size; ++i) {
			Type[] nextTypes = castTypes(Type.getArgumentTypes(methodList.get(i).mMethodNode.desc));
			if (CommonUtils.getSize(nextTypes) > 0) {
				int len = nextTypes.length;
				// 去掉可共用的参数
				for (Type fType : allTypes) {
					for (int j = 0; j < len; ++j) {
						if (nextTypes[j] != null && fType.equals(nextTypes[j])) {
							nextTypes[j] = null;
							break;
						}
					}
				}
				// 增加不可共用的参数到总参数列表
				for (int j = 0; j < len; ++j) {
					if (nextTypes[j] != null) {
						allTypes.add(nextTypes[j]);
					}
				}

			}
		}
		return allTypes;
	}

	private static String getArgTypesStr(List<Type> allTypes) {
		String res = "";
		if (allTypes != null && allTypes.size() != 0) {
			for (Type type : allTypes) {
				res += type.getDescriptor();
			}
		}
		return res;
	}

	/**
	 * 这里暂时限定了所有合并方法的返回类型一致
	 * 
	 * @param beMergeMethosList
	 * @return
	 */
	private static Type getNewRetType(List<ClassMethodInfo> beMergeMethosList) {
		return ASMUtils.castType(Type.getReturnType(beMergeMethosList.get(0).mMethodNode.desc));
	}

	/**
	 * 修复invoker在caller中的INVOKESPECIAL为INVOKEVIRTUAL
	 * 
	 * @param invokerCallerMap
	 *            key: invoker value:invoker对应的caller
	 */
	private static void fixInvokerCaller(Map<String, ClassMethodInfo> invokerCallerMap) {
		if (CommonUtils.isEmpty(invokerCallerMap)) {
			return;
		}
		for (ClassMethodInfo invoker : invokerCallerMap.values()) {
			int access = invoker.mMethodNode.access;
			invoker.mClassNode.access = invoker.mClassNode.access & (~ACC_PRIVATE) & (~ACC_PROTECTED) | ACC_PUBLIC;
			invoker.mMethodNode.access = invoker.mMethodNode.access & (~ACC_PRIVATE) & (~ACC_PROTECTED) | ACC_PUBLIC;
			// 不是私有的 或者是 静态的,不作处理.
			if ((access & ACC_PRIVATE) == 0 || (access & ACC_STATIC) != 0) {
				continue;
			}

			for (ClassMethodInfo invokerCaller : invoker.callerMap.values()) {
				InsnList insnList = invokerCaller.mMethodNode.instructions;
				for (int i = 0; i < insnList.size(); ++i) {
					AbstractInsnNode node = insnList.get(i);
					if (node.getType() == AbstractInsnNode.METHOD_INSN) {
						MethodInsnNode methodInsnNode = (MethodInsnNode) node;
						if (methodInsnNode.owner.equals(invoker.mClassNode.name) && methodInsnNode.name.equals(invoker.mMethodNode.name) && methodInsnNode.desc.equals(invoker.mMethodNode.desc)) {
							// 要跳过构造函数
							if (methodInsnNode.getOpcode() == INVOKESPECIAL && !methodInsnNode.name.contains("<")) {
								methodInsnNode.setOpcode(INVOKEVIRTUAL);
							}
						}
					}
				}
			}
		}
	}

}
