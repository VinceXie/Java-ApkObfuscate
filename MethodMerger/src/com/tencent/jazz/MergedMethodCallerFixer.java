package com.tencent.jazz;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.LdcInsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.VarInsnNode;

import com.tencent.jazz.model.ClassMethodInfo;
import com.tencent.jazz.util.CommonUtils;
import com.tencent.jazz.util.JarLoader;
import com.tencent.retchet.MergerMap;
import com.tencent.retchet.NewOldMethodArgMapInfo;
import com.tencent.retchet.NewOldMethodArgMapInfo.IndexTypeMap;
import com.tencent.retchet.Record;
import com.tencent.retchet.util.ASMUtils;

/**
 * 1、修复实例转静态后的调用
 * 2、修复合并后的调用
 * @author noverguo
 */
public class MergedMethodCallerFixer implements Opcodes {

	public static void fix(List<String> mappingInfo, String jarPath) throws IOException {
		List<ClassNode> nodeList = JarLoader.loadJar(jarPath);
		fix(mappingInfo, nodeList);
		JarLoader.saveToJar(jarPath, jarPath, nodeList);
	}
	public static void fix(List<String> mappingInfo, List<ClassNode> nodeList) {
		List<Record> records = new ArrayList<Record>();
		for(String recordStr : mappingInfo) {
			records.add(new Record(recordStr));
		}
		fixByRecord(records, nodeList);
	}
	public static void fixByRecord(List<Record> records, List<ClassNode> nodeList) {
		
		for(ClassNode cn : nodeList) {
			// 先fix实例转静态
			for(Record record : records) {
				if(record.argInfo == null) {
					fix(cn, record);
				}
			}
			// fix合并的方法
			for(Record record : records) {
				if(record.argInfo != null) {
					fix(cn, record);
				}
			}
		}
	}
	private static void fix(ClassNode cn, Record record) {
		if(record.argInfo == null) {
			for(MethodNode callerMethodNode : cn.methods) {
				InsnList oldInsn = callerMethodNode.instructions;
				// 对所有指令进行遍历
				for(AbstractInsnNode p=oldInsn.getFirst();p!=null;p=p.getNext()){
					// 找到调用方法的指令
					if(p.getType() == AbstractInsnNode.METHOD_INSN) {
						MethodInsnNode min = (MethodInsnNode) p;
						String key = ClassMethodInfo.toString(min);
						if(record.getOldKey().equals(key)) {
							// 把旧的调用关系改为新的即可
							min.setOpcode(INVOKESTATIC);
							min.name = record.newMethodName;
							min.desc = record.newMethodDesc;
						}
					}
				}
			}
		} else {
			List<MethodNode> methods = cn.methods;
			List<Type> newMethodArgTypes = new ArrayList<Type>();
			Type[] argumentTypes = Type.getArgumentTypes(record.newMethodDesc);
			// 第一个为which
			for(int i=1;i<argumentTypes.length;++i) {
				newMethodArgTypes.add(argumentTypes[i]);
			}
			int newMethodArgSize = newMethodArgTypes.size();
			if(!CommonUtils.isEmpty(methods)) {
				String oldKey = ASMUtils.toString(record.oldClassName, record.oldMethodName, record.oldMethodDesc);
				// 
				String oldKey2 = ASMUtils.toString(record.oldClassName, record.oldMethodName + MergerMap.MagicNum, record.oldMethodDesc);
				Type newMethodRetType = Type.getReturnType(record.newMethodDesc);
				for(MethodNode callerMethodNode : methods) {
					InsnList oldInsn = callerMethodNode.instructions;
					int callerMaxLocal = callerMethodNode.maxLocals + 1;
					// 对所有指令进行遍历
					for(AbstractInsnNode p=oldInsn.getFirst();p!=null;p=p.getNext()){
						// 找到调用方法的指令
						if(p.getType() == AbstractInsnNode.METHOD_INSN) {
							MethodInsnNode min = (MethodInsnNode) p;
							String curKey = ASMUtils.toString(min);
							if(oldKey.equals(curKey) || oldKey2.equals(curKey)) {
								InsnList newInsn = new InsnList();
								// 取得caller方法的本地变量大小，在修复中会用到一些本地变量，这里直接增加新本地变量，这样就不会影响到原方法
								int nextLocal = callerMethodNode.maxLocals + 1;
								// 取得新旧方法的参数映射表
								NewOldMethodArgMapInfo newOldMethodArgMapInfo = record.argInfo;
								
								// 用于存放新方法调用参数的指令
								AbstractInsnNode[] newLoadArgNodes = new AbstractInsnNode[newMethodArgSize];
								int oldMethodArgSize = CommonUtils.getSize(newOldMethodArgMapInfo.mMap);
								if(oldMethodArgSize > 0) {
									// 把栈中的数据按旧方法的参数反序保存到新的local变量中
									for(int j=oldMethodArgSize-1;j>=0;j--) {
										IndexTypeMap indexTypeMap = newOldMethodArgMapInfo.mMap[j];
										int op = indexTypeMap.mOldType.getOpcode(ISTORE);
										newInsn.add(new VarInsnNode(op, nextLocal));
										nextLocal += indexTypeMap.mOldType.getSize();
									}
									// 取修复调用时最大的Local值
									if(callerMaxLocal < nextLocal) {
										callerMaxLocal = nextLocal;
									}
									// 把刚刚保存到新的local变量中的数据根据旧方法的的参数顺序依次设置到新方法对应的压入参数指令中
									for(int j=0;j<oldMethodArgSize;++j) {
										IndexTypeMap indexTypeMap = newOldMethodArgMapInfo.mMap[j];
										int op = indexTypeMap.mOldType.getOpcode(ILOAD);
										nextLocal -= indexTypeMap.mOldType.getSize();
										newLoadArgNodes[indexTypeMap.mNewIndex] = new VarInsnNode(op, nextLocal);
									}
								}
								newInsn.add(new LdcInsnNode(newOldMethodArgMapInfo.mWhich));
								// 填充新方法相对旧方法多出来的参数
								for(int j=0;j<newMethodArgSize;++j) {
									if(newLoadArgNodes[j] == null) {
										newLoadArgNodes[j] = ASMUtils.newConstInsnNodeByType(newMethodArgTypes.get(j));
									}
									newInsn.add(newLoadArgNodes[j]);
								}
								
								// 插入修复压入参数的指令
								oldInsn.insertBefore(p, newInsn);
								// 插入新方法的调用
								oldInsn.insertBefore(p, new MethodInsnNode(INVOKESTATIC, record.newClassName, record.newMethodName, record.newMethodDesc, false));
								Type oldRetType = Type.getReturnType(min.desc);
								// 返回类型不一致，则作对应的强制类型转换
								if(!newMethodRetType.equals(oldRetType)) {
									AbstractInsnNode castNode = ASMUtils.getCastNodeByTypes(newMethodRetType, oldRetType);
									if(castNode != null) {
										oldInsn.insertBefore(p, castNode);
									}
								}
								// 删除旧方法的调用
								AbstractInsnNode q = p.getPrevious();
								oldInsn.remove(p);
								p=q;
							}
						}
					}
					// 修复最大本地变量数
					callerMethodNode.maxLocals = callerMaxLocal;
				}
			}
		}
	}

}
