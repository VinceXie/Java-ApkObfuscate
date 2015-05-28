package com.tencent.obfuscate.jar;


import java.util.ArrayList;

import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.FieldVisitor;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.FieldInsnNode;
import org.objectweb.asm.tree.FieldNode;
import org.objectweb.asm.tree.FrameNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.JumpInsnNode;
import org.objectweb.asm.tree.LabelNode;
import org.objectweb.asm.tree.MethodNode;

import com.tencent.obfuscate.Configuration;
import com.tencent.obfuscate.pool.FieldInfo;
import com.tencent.obfuscate.pool.MClassNode;
import com.tencent.obfuscate.pool.MethodInfo;
import com.tencent.obfuscate.pool.Pool;

/**
 * @author VinceXie vince.xie@qq.com
 * @version 2014-10-17 上午11:29:55
 * State
 * 插入goto 转换为dex会失效
 */
public class JarObfuscator2 implements Opcodes{

	public static void main(String args[]) throws Exception
	{

		if (args == null || args.length < 1) {
			System.out.println("useage: <beMerge.jar>");
			return;
		}
		Configuration.initArgs(args);
		Pool pool=Pool.loadJar();
		Parse(pool);
		DoUpset(pool);
		pool.saveJar("test\\upset.jar");
//		pool.saveJar(null);
	}
	
	public static void Parse(Pool pool) {
		ArrayList<MClassNode> classNodes=pool.classNodes;
		
		int i=0;
		for(MClassNode classNode:classNodes)
		{
			classNode.methodInfos.clear();
			classNode.fieldInfos.clear();
			for (MethodNode methodNode : classNode.methods) {
				i++;
				MethodInfo methodInfo = new MethodInfo(methodNode, classNode);
				methodInfo.setUpset();
				classNode.methodInfos.add(methodInfo);
			}
			for (FieldNode fieldNode : classNode.fields) {
				FieldInfo fieldInfo = new FieldInfo(classNode, fieldNode);
				classNode.fieldInfos.add(fieldInfo);

			}
		}
		System.out.println("总方法数："+i);
		
	}
	public static void DoUpset(Pool pool) throws Exception
	{
		int j=0;
		for(MClassNode classNode:pool.classNodes)
		{
			for(MethodInfo methodInfo:classNode.methodInfos)
			{
				if(methodInfo.canUpset)
				{
					j++;
					MethodNode methodNode=methodInfo.methodNode;
					InsnList insnList=methodNode.instructions;

					FrameNode fn=new FrameNode(Opcodes.F_SAME, 0, null, 0, null);
					FieldInsnNode fin=new FieldInsnNode(GETSTATIC, className, "MagicNum", "I");
					InsnNode in=new InsnNode(ICONST_0);
					InsnNode pop=new InsnNode(POP);
//					FieldInsnNode fin2=new FieldInsnNode(PUTSTATIC, className, "MagicNum", "I");
					
					LabelNode code1=new LabelNode();
					//goto可以使用 但是dex会回转回来
					JumpInsnNode goto1=new JumpInsnNode(GOTO, code1);
					
					LabelNode code2=new LabelNode();
					JumpInsnNode goto2=new JumpInsnNode(GOTO, code2);
					
//					LabelNode code3=new LabelNode();
//					JumpInsnNode goto3=new JumpInsnNode(IFNE, code3);
					
					LabelNode codeEnd=new LabelNode();
					JumpInsnNode gotoEnd=new JumpInsnNode(GOTO, codeEnd);
					
					int space=(insnList.size()-2)/2;
					
					//构造新的指令序列
					InsnList upserInsnList=new InsnList();

					
//					upserInsnList.add(in);
//					upserInsnList.add(pop);
					upserInsnList.add(goto1);
					
					upserInsnList.add(code2);
//					upserInsnList.add(fn);
					//插入第二块区域代码
					for(int i=space;i<insnList.size()-2;i++)
					{
						upserInsnList.add(insnList.get(i));
					}
					upserInsnList.add(gotoEnd);
					
					upserInsnList.add(code1);
					//upserInsnList.add(fn);
					//插入第一块区域代码
					for(int i=0;i<space;i++)
					{
						upserInsnList.add(insnList.get(i));
					}
//					upserInsnList.add(fin);
					upserInsnList.add(goto2);
					
					upserInsnList.add(codeEnd);	
					//插回最后的代码
					upserInsnList.add(insnList.get(insnList.size()-2));
					upserInsnList.add(insnList.get(insnList.size()-1));
					
					
					methodNode.instructions=upserInsnList;
					
					System.out.println(methodInfo);
					Tools.printMethod(methodInfo);
					
				}
				
			}
		}
		System.out.println("被打乱指令的方法数："+j);
	}
	
	public static String className="com/tencent/jarconfusion/Jarconfusion";
	public static byte[] dump () throws Exception {

		ClassWriter cw = new ClassWriter(0);
		FieldVisitor fv;
		MethodVisitor mv;
		AnnotationVisitor av0;

		cw.visit(V1_6, ACC_PUBLIC + ACC_SUPER, className, null, "java/lang/Object", null);

		cw.visitSource("Jarconfusion.java", null);

		{
		fv = cw.visitField(ACC_PUBLIC + ACC_STATIC, "MagicNum", "I", null, null);
		fv.visitEnd();
		}
		{
		mv = cw.visitMethod(ACC_STATIC, "<clinit>", "()V", null, null);
		mv.visitCode();
		Label l0 = new Label();
		mv.visitLabel(l0);
		mv.visitLineNumber(9, l0);
		mv.visitInsn(ICONST_1);
		mv.visitFieldInsn(PUTSTATIC, className, "MagicNum", "I");
		mv.visitInsn(RETURN);
		mv.visitMaxs(1, 0);
		mv.visitEnd();
		}
		{
		mv = cw.visitMethod(ACC_PUBLIC, "<init>", "()V", null, null);
		mv.visitCode();
		Label l0 = new Label();
		mv.visitLabel(l0);
		Label l11 = new Label();
		mv.visitLabel(l11);
		mv.visitLineNumber(7, l0);
		mv.visitVarInsn(ALOAD, 0);
		mv.visitMethodInsn(INVOKESPECIAL, "java/lang/Object", "<init>", "()V", false);
		mv.visitInsn(RETURN);
		Label l1 = new Label();
		mv.visitLabel(l1);
		mv.visitLocalVariable("this", "Lcom/tencent/jarconfusion/Jarconfusion;", null, l0, l1, 0);
		mv.visitMaxs(1, 1);
		mv.visitEnd();
		}
		cw.visitEnd();

		return cw.toByteArray();
	}
}
