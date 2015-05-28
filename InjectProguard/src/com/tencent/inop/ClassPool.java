package com.tencent.inop;

import java.util.ArrayList;
import java.util.HashMap;

import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.FieldInsnNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.JumpInsnNode;
import org.objectweb.asm.tree.LabelNode;
import org.objectweb.asm.tree.LdcInsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.VarInsnNode;

/**
 * @author VinceXie vince.xie@qq.com
 * @version 2014-8-18 上午11:03:51
 * State
 */
public class ClassPool implements Opcodes{

	//以下为注入的方法点
	private final String mergerMethod="proguard/ProGuard.execute()V";
	
	private final String mapMethod="proguard/ProGuard.obfuscate()V";
	
	private final String outMethod="proguard/ClassPathEntry.<init>(Ljava/io/File;Z)V";
	
	private final String applyMethod="proguard/ProGuard.obfuscate()V";
	
	private final String keepOpMethodg="proguard/ConfigurationParser.parse(Lproguard/Configuration;)V";
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
		buildMethodInfo();
		injectMerger();
		injectMap();
		injectOut();
		injectApply();
		injectKeepOp();
	}
	private void injectMerger()
	{
		MethodNode methodNode=hashStrToInfo.get(mergerMethod).methodNode;
		InsnList insnList=methodNode.instructions;
		for(int i=insnList.size()-1;i>=0;i--)
		{
			AbstractInsnNode abstractInsnNode=insnList.get(i);
			if(abstractInsnNode.getOpcode()==RETURN)
			{
				insnList.insertBefore(abstractInsnNode, 
						new MethodInsnNode(INVOKESTATIC, "com/tencent/optimusprime/OpGuard", "Merger", "()V", false));
			}
		}
	}
	
	private void injectMap()
	{
		MethodNode methodNode=hashStrToInfo.get(mapMethod).methodNode;
		InsnList insnList=methodNode.instructions;
		for(int i=insnList.size()-1;i>=0;i--)
		{
			AbstractInsnNode abstractInsnNode=insnList.get(i);
			if(abstractInsnNode.getOpcode()==RETURN)
			{
				insnList.insertBefore(abstractInsnNode, new VarInsnNode(ALOAD, 0));
				insnList.insertBefore(abstractInsnNode, new FieldInsnNode(GETFIELD, "proguard/ProGuard", "configuration", "Lproguard/Configuration;"));
				insnList.insertBefore(abstractInsnNode, new FieldInsnNode(GETFIELD, "proguard/Configuration", "printMapping", "Ljava/io/File;"));
				LabelNode l8 = new LabelNode();
				insnList.insertBefore(abstractInsnNode, new JumpInsnNode(IFNULL, l8));
				insnList.insertBefore(abstractInsnNode, new VarInsnNode(ALOAD, 0));
				insnList.insertBefore(abstractInsnNode, new FieldInsnNode(GETFIELD, "proguard/ProGuard", "configuration", "Lproguard/Configuration;"));
				insnList.insertBefore(abstractInsnNode, new FieldInsnNode(GETFIELD, "proguard/Configuration", "printMapping", "Ljava/io/File;"));
				insnList.insertBefore(abstractInsnNode, new MethodInsnNode(INVOKEVIRTUAL, "java/io/File", "getAbsolutePath", "()Ljava/lang/String;", false));
				insnList.insertBefore(abstractInsnNode, new FieldInsnNode(PUTSTATIC, "com/tencent/optimusprime/OpGuard", "mapPath", "Ljava/lang/String;"));
				insnList.insertBefore(abstractInsnNode, l8);
			}
		}
	}
	
	private void injectOut()
	{
		MethodNode methodNode=hashStrToInfo.get(outMethod).methodNode;
		InsnList insnList=methodNode.instructions;
		for(int i=insnList.size()-1;i>=0;i--)
		{
			AbstractInsnNode abstractInsnNode=insnList.get(i);
			if(abstractInsnNode.getOpcode()==RETURN)
			{
				insnList.insertBefore(abstractInsnNode, new VarInsnNode(ILOAD, 2));
				LabelNode l8 = new LabelNode();
				insnList.insertBefore(abstractInsnNode, new JumpInsnNode(IFEQ, l8));
				insnList.insertBefore(abstractInsnNode, new VarInsnNode(ALOAD, 1));
				insnList.insertBefore(abstractInsnNode, new MethodInsnNode(INVOKEVIRTUAL, "java/io/File", "getAbsolutePath", "()Ljava/lang/String;", false));
				insnList.insertBefore(abstractInsnNode, new FieldInsnNode(PUTSTATIC, "com/tencent/optimusprime/OpGuard", "outPath", "Ljava/lang/String;"));
				insnList.insertBefore(abstractInsnNode, l8);
			}
		}
	}
	
	private void injectApply()
	{
		MethodNode methodNode=hashStrToInfo.get(applyMethod).methodNode;
		InsnList insnList=methodNode.instructions;
		for(int i=insnList.size()-1;i>=0;i--)
		{
			AbstractInsnNode abstractInsnNode=insnList.get(i);
			if(abstractInsnNode.getOpcode()==RETURN)
			{
				insnList.insertBefore(abstractInsnNode, new VarInsnNode(ALOAD, 0));
				insnList.insertBefore(abstractInsnNode, new FieldInsnNode(GETFIELD, "proguard/ProGuard", "configuration", "Lproguard/Configuration;"));
				insnList.insertBefore(abstractInsnNode, new FieldInsnNode(GETFIELD, "proguard/Configuration", "applyMapping", "Ljava/io/File;"));
				LabelNode l8 = new LabelNode();
				insnList.insertBefore(abstractInsnNode, new JumpInsnNode(IFNULL, l8));
				insnList.insertBefore(abstractInsnNode, new VarInsnNode(ALOAD, 0));
				insnList.insertBefore(abstractInsnNode, new FieldInsnNode(GETFIELD, "proguard/ProGuard", "configuration", "Lproguard/Configuration;"));
				insnList.insertBefore(abstractInsnNode, new FieldInsnNode(GETFIELD, "proguard/Configuration", "applyMapping", "Ljava/io/File;"));
				insnList.insertBefore(abstractInsnNode, new MethodInsnNode(INVOKEVIRTUAL, "java/io/File", "getAbsolutePath", "()Ljava/lang/String;", false));
				insnList.insertBefore(abstractInsnNode, new FieldInsnNode(PUTSTATIC, "com/tencent/optimusprime/OpGuard", "applyMapPath", "Ljava/lang/String;"));
				insnList.insertBefore(abstractInsnNode, l8);
			}
		}
	}
	
	private void injectKeepOp()
	{
		MethodNode methodNode=hashStrToInfo.get(keepOpMethodg).methodNode;
		InsnList insnList=methodNode.instructions;
		boolean hasGOTO=false;
		AbstractInsnNode insertNode;
		for(int i=0;i<insnList.size();i++)
		{
			AbstractInsnNode abstractInsnNode=insnList.get(i);
			if(((abstractInsnNode.getOpcode()==IFNULL)||(abstractInsnNode.getOpcode()==GOTO))&&!hasGOTO)
			{
				hasGOTO=true;
				continue;
			}
			if(abstractInsnNode.getType()==AbstractInsnNode.LABEL&&hasGOTO)
			{
				LabelNode whileBeginLabel=(LabelNode)abstractInsnNode;
				insertNode=insnList.get(i+1);
				System.out.println("注入keepop!");
				//if ("-keepop".equals(nextWord)){
				insnList.insertBefore(insertNode, new LdcInsnNode("-keepop"));
				insnList.insertBefore(insertNode, new VarInsnNode(ALOAD, 0));
				insnList.insertBefore(insertNode, new FieldInsnNode(GETFIELD, "proguard/ConfigurationParser", "nextWord", "Ljava/lang/String;"));
				insnList.insertBefore(insertNode, new MethodInsnNode(INVOKEVIRTUAL, "java/lang/String", "equals", "(Ljava/lang/Object;)Z", false));
				LabelNode l3 = new LabelNode();			
				insnList.insertBefore(insertNode, new JumpInsnNode(IFEQ, l3));
				//readNextWord();
				insnList.insertBefore(insertNode, new VarInsnNode(ALOAD, 0));
				insnList.insertBefore(insertNode, new MethodInsnNode(INVOKESPECIAL, "proguard/ConfigurationParser", "readNextWord", "()V", false));
				//OpGuard.addKeepOp(nextWord);
				insnList.insertBefore(insertNode, new VarInsnNode(ALOAD, 0));
				insnList.insertBefore(insertNode, new FieldInsnNode(GETFIELD, "proguard/ConfigurationParser", "nextWord", "Ljava/lang/String;"));
				insnList.insertBefore(insertNode, new MethodInsnNode(INVOKESTATIC, "com/tencent/optimusprime/OpGuard", "addKeepOp", "(Ljava/lang/String;)V", false));
				//readNextWord();
				insnList.insertBefore(insertNode, new VarInsnNode(ALOAD, 0));
				insnList.insertBefore(insertNode, new MethodInsnNode(INVOKESPECIAL, "proguard/ConfigurationParser", "readNextWord", "()V", false));
				//if(nextWord!=null) 作了特殊处理，自己处理了while判断
				insnList.insertBefore(insertNode, new VarInsnNode(ALOAD, 0));
				insnList.insertBefore(insertNode, new FieldInsnNode(GETFIELD, "proguard/ConfigurationParser", "nextWord", "Ljava/lang/String;"));
				insnList.insertBefore(insertNode, new JumpInsnNode(IFNONNULL, whileBeginLabel));
				//else：nextWord!=null 跳出
				insnList.insertBefore(insertNode, new InsnNode(RETURN));
				//最初的if判断失败则跳过以上代码
				insnList.insertBefore(insertNode, l3);
				return;
			}
			
		}
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
