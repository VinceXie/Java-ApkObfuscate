package com.tencent.cc;

import java.util.ArrayList;
import java.util.HashMap;

import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.FieldInsnNode;
import org.objectweb.asm.tree.FieldNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.JumpInsnNode;
import org.objectweb.asm.tree.LabelNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.VarInsnNode;



/**
 * @author VinceXie vince.xie@qq.com
 * @version 2014-8-18 上午11:03:51
 * State
 */
public class ClassPool implements Opcodes{

	/**
	 * 所有类
	 */
	public ArrayList<MClassNode> classes = new ArrayList<MClassNode>();
	/**
	 * 类名与类的映射
	 */
	public static HashMap<String, MClassNode> hashStrToNode = new HashMap<String, MClassNode>();

	/**
	 * classNode.name + methodNode.name + methodNode.desc与方法信息的映射表
	 */
	private HashMap<String, MethodInfo> hashStrToInfo = new HashMap<String, MethodInfo>();

	/**
	 * 所有方法信息的集合
	 */
	private ArrayList<MethodInfo> methodInfos = new ArrayList<MethodInfo>();


	public void addNode(MClassNode mClassNode) {
		classes.add(mClassNode);
		hashStrToNode.put(mClassNode.name, mClassNode);
	}

	public void build()
	{
		buildClassPool();
		buildMethodInfo();
		buildInfoCaller();
		buildCanMer();
		

	}

	private void messClass()
	{
		for (MClassNode classNode : classes) {
			
		}
	}
	/**
	 * 为零散的类建立关系
	 */
	private void buildClassPool() {
		for (MClassNode classNode : classes) {
			// 父节点建立关系
			classNode.dadNodeNames.add(classNode.superName);
			if (hashStrToNode.get(classNode.superName) != null) {
				MClassNode dadNode = hashStrToNode.get(classNode.superName);
				dadNode.sonNodeNames.add(classNode.name);
			}
			// 接口建立关系
			for (String tmp : classNode.interfaces) {
				classNode.dadNodeNames.add(tmp);
				if (hashStrToNode.get(tmp) != null) {
					MClassNode dadNode = hashStrToNode.get(tmp);
					dadNode.sonNodeNames.add(classNode.name);
				}
			}
		}
	}
	
	/**
	 * 通过proguard的mapping设置哪些是可合并的
	 */
	private void buildCanMer() {
		
		for (MethodInfo methodInfo : methodInfos) {
			if (methodInfo.methodNode.name.charAt(0) != '<'
					&&!methodInfo.methodNode.name.equals("main")
					&&!isNativeOrAbstractOrInConfig(methodInfo)) {
				methodInfo.canMer = true;
				
			}
		}
		return;
	}
	
	private boolean isNativeOrAbstractOrInConfig(MethodInfo methodInfo)
	{
		if((methodInfo.methodNode.access&ACC_NATIVE)==ACC_NATIVE)
		{
			return true;
		}
		if((methodInfo.methodNode.access&ACC_ABSTRACT)==ACC_ABSTRACT)
		{
			return true;
		}
//		String methodString=methodInfo.classNode.name+"."+methodInfo.methodNode.name+methodInfo.methodNode.desc;
//		for(String tmp:conStrings)
//		{
//			if(tmp.equals(methodString))
//			{
//				return true;
//			}
//		}
		return false;
	}
	/**
	 * 生成外部调用
	 * 
	 * @param classNode
	 */
	private void buildInfoCaller() {
		for (MClassNode classNode : classes) {
			for (MethodNode methodNode : classNode.methods) {
				InsnList insnList = methodNode.instructions;
				MethodInfo outMethodInfo=hashStrToInfo.get(classNode.name+methodNode.name+methodNode.desc);
				for (int i = 0; i < insnList.size(); i++) {
					AbstractInsnNode insnNode = insnList.get(i);
					if (insnNode.getOpcode() == INVOKESTATIC||
							insnNode.getOpcode() == INVOKEVIRTUAL||
							insnNode.getOpcode() == INVOKESPECIAL||
							insnNode.getOpcode() == INVOKEINTERFACE||
							insnNode.getOpcode() == INVOKEDYNAMIC) {
						MethodInsnNode methodInsnNode = (MethodInsnNode) insnNode;
								
						MethodInfo methodInfo = findRealMethodInfo(methodInsnNode.owner,methodInsnNode.name+ methodInsnNode.desc);
						
						// 找不到，则必然是系统类的方法。系统类不会正面调用我们写的类，所以不需要作Caller的处理
						if(methodInfo == null) {
							MClassNode cn = hashStrToNode.get(methodInsnNode.owner);
							// 如果是系统的方法，但调用时用的是我们的类，则我们写的类可能需要进行修复，因此此时需要对invoker进行处理
							// 如：ourClass.superSystemMethod
							if(cn != null) {
								// 由于invoker的方法是父类的，所以必然不是private，因而一率当public处理
								outMethodInfo.invokers.add(new Invoker(cn, new MethodNode(ACC_PUBLIC, methodInsnNode.name, methodInsnNode.desc, "", null)));
							}
						} else {
							//构建caller
							methodInfo.callers.add(new Caller(classNode, methodNode));
							//构建invoker
							outMethodInfo.invokers.add(new Invoker(methodInfo.classNode, methodInfo.methodNode));
						}
					}
					else if(insnNode.getClass().equals(FieldInsnNode.class))
					{
						FieldInsnNode fieldInsnNode=(FieldInsnNode)insnNode;
						MClassNode fieldClassNode=hashStrToNode.get(fieldInsnNode.owner);
						//调用的是系统包的字段则跳过
						if(fieldClassNode==null)
						{
							continue;
						}
						FieldInfo fieldInfo=findRealFieldInfo(fieldClassNode, fieldInsnNode.name);
						if(fieldInfo==null)
						{
							continue;
						}
						outMethodInfo.fieldInfos.add(fieldInfo);
					}
					
				}

			}
		}
	}
	
	private MethodInfo findRealMethodInfo(String className,String methodNameDesc)
	{
		MClassNode classNode=hashStrToNode.get(className);
		if(classNode==null)
		{
			return null;
		}
		for(MethodNode methodNode:classNode.methods)
		{
			if(methodNameDesc.equals(methodNode.name+methodNode.desc))
			{
				return hashStrToInfo.get(className+methodNameDesc);
			}
		}
		
		return findRealMethodInfo(classNode.superName,methodNameDesc);
		
	}
	private FieldInfo findRealFieldInfo(MClassNode classNode,String fieldName)
	{
		for(FieldNode fieldNode:classNode.fields)
		{
			if(fieldName.equals(fieldNode.name))
			{
				return new FieldInfo(classNode, fieldNode);
			}
		}
		MClassNode dadClassNode=hashStrToNode.get(classNode.superName);
		if(dadClassNode==null)
		{
			return null;
		}
		return findRealFieldInfo(dadClassNode,fieldName);
		
	}
	/**
	 * 生成自身methondinfo
	 * 
	 * @param classNode
	 */
	private void buildMethodInfo() {
		for (MClassNode classNode : classes) {
			classNode.methodInfos.clear();
			for (MethodNode methodNode : classNode.methods) {
				MethodInfo methodInfo = new MethodInfo(methodNode, classNode);
				hashStrToInfo.put(classNode.name + "."+methodNode.name
						+ methodNode.desc, methodInfo);
				System.out.println(classNode.name + "."+methodNode.name
						+ methodNode.desc);
				classNode.methodInfos.add(methodInfo);
				methodInfos.add(methodInfo);
			}
		}
	}
}
