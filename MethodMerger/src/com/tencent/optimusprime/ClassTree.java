package com.tencent.optimusprime;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.FieldInsnNode;
import org.objectweb.asm.tree.FieldNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;

import com.tencent.retchet.util.ASMUtils;

/**
 * @author VinceXie vince.xie@qq.com
 * @version 2014-5-15 下午4:25:02 State 树结构
 */
public class ClassTree implements Opcodes {

	/**
	 * 所有类
	 */
	private ArrayList<MClassNode> classes = new ArrayList<MClassNode>();
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

	private List<String> conStrings=new ArrayList<String>();

	public void setConStrings(List<String> conString) {
		this.conStrings = conString;
	}


	/**
	 * proguard的mapping
	 */
	private MappingReader mReader;

	public static final int FLAG_ALL = 0;
	public static final int FLAG_PRIVATE = 1;
	public static final int FLAG_STATIC = 2;
	public static final int FLAG_NOTSTATIC=3;
	public static final int FLAG_PRIVATE_STATIC=4;
	public static final int FLAG_PRIVATE_NOTSTATIC=5;
	public static final int FLAG_ALL_PROGUARD=6;

	public void setmReader(MappingReader mReader) {
		this.mReader = mReader;
	}

	public void addNode(MClassNode mClassNode) {

		getClasses().add(mClassNode);
		hashStrToNode.put(mClassNode.name, mClassNode);
	}

	/**
	 * 根据classes的值生成树
	 */
	public void build() {

		hashStrToInfo.clear();
		methodInfos.clear();
		
		buildClassPool();
		buildMethodInfo();
		buildInfoCaller();
		buildCanMer();
		findUnOverride();
		
		

	}

	/**
	 * 打乱包路径和类名
	 */
	public void messClass()
	{
		HashMap<String, String> changMap=new HashMap<String, String>();
		for(MClassNode classNode:classes)
		{
			if(!classNode.canMess)
			{
				continue;
			}
			classNode.name=Tools.getRandomString(8);
			changMap.put(classNode.orgName, classNode.name);
			
			//替换子类的继承名
			for(String tmp:classNode.sonNodeNames)
			{
				hashStrToNode.get(tmp).dadNodeNames.remove(classNode.orgName);
				hashStrToNode.get(tmp).dadNodeNames.add(classNode.name);
				hashStrToNode.get(tmp).superName=classNode.name;
			}			
			//里面的default全改为public
			for(FieldNode node:classNode.fields)
			{
				if(Tools.isDefault(node.access))
				{
					int access=Opcodes.ACC_PUBLIC;
					if(Tools.isStatic(access))
					{
						access=access+Opcodes.ACC_STATIC;
					}
					if(Tools.isFinal(access))
					{
						access=access+Opcodes.ACC_FINAL;
					}
					node.access=access;
				}
			}
			for(MethodNode node:classNode.methods)
			{
				if(Tools.isDefault(node.access))
				{
					int access=Opcodes.ACC_PUBLIC;
					if(Tools.isStatic(access))
					{
						access=access+Opcodes.ACC_STATIC;
					}
					if(Tools.isFinal(access))
					{
						access=access+Opcodes.ACC_FINAL;
					}
					node.access=access;
				}
			}
		}
		
		for(MClassNode classNode:classes)
		{
			for(MethodNode methodNode:classNode.methods)
			{
				for(int i=0;i<methodNode.instructions.size();i++)
				{
					AbstractInsnNode abstractInsnNode=methodNode.instructions.get(i);
					if(abstractInsnNode.getType()==AbstractInsnNode.FIELD_INSN)
					{
						FieldInsnNode node=(FieldInsnNode)abstractInsnNode;
						for(String tmp:changMap.keySet())
						{
							if(tmp.equals(node.owner))
							{
								node.owner=changMap.get(tmp);
							}
							break;
						}			
					}
					else if(abstractInsnNode.getType()==AbstractInsnNode.METHOD_INSN)
					{
						MethodInsnNode node=(MethodInsnNode)abstractInsnNode;
						for(String tmp:changMap.keySet())
						{
							if(tmp.equals(node.owner))
							{
								node.owner=changMap.get(tmp);
							}
							break;
						}			
						
					}
					
				}
			}
		}
	}
	
	/**
	 * 找出没有覆盖关系的函数
	 */
	private void findUnOverride() {
		for (MethodInfo methodInfo : methodInfos) {
			if (!methodInfo.canMer) {
				continue;
			}

			if (MethodInfo.isPrivate(methodInfo.methodNode)) {
				methodInfo.unOverride = true;
				continue;
			}
			
			boolean unOverride=true;
			for(String dadName:methodInfo.classNode.dadNodeNames)
			{
				if(hashStrToNode.containsKey(dadName))
				{
					unOverride=false;
					break;
				}
			}
			for(String sonName:methodInfo.classNode.sonNodeNames)
			{
				if(hashStrToNode.containsKey(sonName))
				{
					unOverride=false;
					break;
				}
			}
			if(unOverride)
			{
				methodInfo.unOverride=true;
			}
		}
	}

	/**
	 * 为零散的类建立关系
	 */
	private void buildClassPool() {
		for (MClassNode classNode : classes) {
			classNode.orgName=classNode.name;
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

		if (mReader == null) {
			for (MethodInfo methodInfo : methodInfos) {
				if (methodInfo.methodNode.name.charAt(0) != '<'
						&&!methodInfo.methodNode.name.equals("main")
						&&!isNativeOrAbstractOrInConfig(methodInfo)) {
					methodInfo.canMer = true;
					methodInfo.canMerOutside=MethodInfo.isMerOutside(methodInfo.methodNode);
				}
			}
			return;
		}

		for (MethodInfo methodInfo : methodInfos) {
			if (mReader.getObfuseds().contains(methodInfo.getMapString())) {
				methodInfo.canMer = true;
				methodInfo.canMerOutside=MethodInfo.isMerOutside(methodInfo.methodNode);
				for(String tmp:conStrings) {
					if(ASMUtils.toString(methodInfo.classNode, methodInfo.methodNode).contains(tmp)) {
						System.out.println("[Optimus Prime] grep method" + ASMUtils.toString(methodInfo.classNode, methodInfo.methodNode));
						methodInfo.canMer = false;
					}
				}
			}

		}
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
		String methodString=methodInfo.classNode.name+"."+methodInfo.methodNode.name+methodInfo.methodNode.desc;
		for(String tmp:conStrings)
		{
			if(tmp.equals(methodString))
			{
				return true;
			}
		}
		return false;
	}
	/**
	 * 生成外部调用
	 * 
	 * @param classNode
	 */
	private void buildInfoCaller() {
		for (MClassNode classNode : getClasses()) {
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
	/**
	 * 生成自身methondinfo
	 * 
	 * @param classNode
	 */
	private void buildMethodInfo() {
		for (MClassNode classNode : getClasses()) {
			classNode.methodInfos.clear();
			for (MethodNode methodNode : classNode.methods) {
				MethodInfo methodInfo = new MethodInfo(methodNode, classNode);
				hashStrToInfo.put(classNode.name + methodNode.name
						+ methodNode.desc, methodInfo);

				classNode.methodInfos.add(methodInfo);
				methodInfos.add(methodInfo);
			}
		}
	}

	/**
	 * 根据参数返回不同信息
	 * 
	 * @param flag:FLAG_ALL\FLAG_PRIVATE(私有非静态)\FLAG_STATIC
	 * @return 所有可混淆的方法信息
	 */
	public ArrayList<MethodInfo> getMethodInfos(int flag) {
		return getMethodInfos(flag, true);
	}
	public ArrayList<MethodInfo> getMethodInfos(int flag, boolean grep) {
		ArrayList<MethodInfo> methodInfos = new ArrayList<MethodInfo>();
		switch (flag) {
		case FLAG_ALL:
			return this.methodInfos;
		case FLAG_PRIVATE:
			for (MethodInfo methodInfo : this.methodInfos) {
				MethodNode methodNode = methodInfo.methodNode;
				if (!MethodInfo.isPrivate(methodNode)) {
					continue;
				}
				if(!grep || (grep && methodInfo.canMer)) {
					methodInfos.add(methodInfo);
				}
			}
			break;
		case FLAG_PRIVATE_STATIC:
			for (MethodInfo methodInfo : this.methodInfos) {
				MethodNode methodNode = methodInfo.methodNode;
				if (!MethodInfo.isPrivate(methodNode)) {
					continue;
				}
				if (!MethodInfo.isStatic(methodNode)) {
					continue;
				}
				if(!grep || (grep && methodInfo.canMer)) {
					methodInfos.add(methodInfo);
				}
			}
			break;
		case FLAG_PRIVATE_NOTSTATIC:
			for (MethodInfo methodInfo : this.methodInfos) {
				MethodNode methodNode = methodInfo.methodNode;
				if (!MethodInfo.isPrivate(methodNode)) {
					continue;
				}
				if (MethodInfo.isStatic(methodNode)) {
					continue;
				}
				if(!grep || (grep && methodInfo.canMer)) {
					methodInfos.add(methodInfo);
				}
			}
			break;
		case FLAG_STATIC:
			for (MethodInfo methodInfo : this.methodInfos) {
				MethodNode methodNode = methodInfo.methodNode;
				if (!MethodInfo.isStatic(methodNode)) {
					continue;
				}
				if(!grep || (grep && methodInfo.canMer)) {
					methodInfos.add(methodInfo);
				}
			}
			break;
		case FLAG_NOTSTATIC:
			for (MethodInfo methodInfo : this.methodInfos) {
				MethodNode methodNode = methodInfo.methodNode;
				if (MethodInfo.isStatic(methodNode)) {
					continue;
				}
				if(!grep || (grep && methodInfo.canMer)) {
					methodInfos.add(methodInfo);
				}
			}
			break;
		case FLAG_ALL_PROGUARD:
			for (MethodInfo methodInfo : this.methodInfos) {
				if(methodInfo.canMer) {
					methodInfos.add(methodInfo);
				}
			}
		}
		
		return methodInfos;
	}


	public MClassNode getNode(String string) {
		return hashStrToNode.get(string);
	}

	public MethodInfo getMethodInfo(String className,String methodName,String methodDesc) {
		return hashStrToInfo.get(className+methodName+methodDesc);
	}


	public ArrayList<MClassNode> getClasses() {
		return classes;
	}

}
