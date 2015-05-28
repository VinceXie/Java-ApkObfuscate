package com.tencent.jazz.tools;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.List;

import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.Attribute;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.FieldVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.util.TraceClassVisitor;

import com.tencent.jazz.model.ClassMethodInfo;
import com.tencent.jazz.util.CommonUtils;

/**
 * 方法合并前后的记录，用于找Bug
 * @author noverguo
 */
public class MethodRecordPrinter implements Opcodes {
	
	private static File dir = null;
	
	public static void enable(File outDir) {
		if(!outDir.exists()) {
			outDir.mkdirs();
		}
		dir = outDir;
	}
	
	public static void disable() {
		dir = null;
	}
	
	private static String getMethodRecord(final MethodNode methodNode, final ClassNode classNode) throws IOException {
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		PrintWriter writer = new PrintWriter(out);
		classNode.accept(new ClassVisitor(ASM5, new TraceClassVisitor(writer)) {
			@Override
			public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
			}
			@Override
			public AnnotationVisitor visitAnnotation(String desc, boolean visible) {
				return null;
			}
			@Override
			public void visitAttribute(Attribute attr) {
			}
			@Override
			public FieldVisitor visitField(int access, String name, String desc, String signature, Object value) {
				return null;
			}
			@Override
			public void visitInnerClass(String name, String outerName, String innerName, int access) {
			}
			public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
				if(methodNode.name.equals(name) && methodNode.desc.equals(desc)) {
					return super.visitMethod(access, name, desc, signature, exceptions);
				}
				return null;
			}
			@Override
			public void visitOuterClass(String owner, String name, String desc) {
			}
			@Override
			public void visitSource(String source, String debug) {
			}
		});
		writer.flush();
		writer.close();
		String res = out.toString();
		out.close();
		return res;
	}
	
	public static void printNewMethodRecord(ClassMethodInfo cmi) throws IOException {
		if(dir == null) {
			return;
		}
		ClassNode newClassNode = cmi.mClassNode;
		final MethodNode newMethodNode = cmi.mMethodNode;
		File outDir = new File(dir, newClassNode.name + "/");
		if(!outDir.exists()) {
			outDir.mkdirs();
		}
		String outName = newMethodNode.name;
		File newMethodFile = new File(outDir, outName + "_new");
		String newMethodOut = cmi.toString() + "\r\n";
		
		newMethodOut += getMethodRecord(newMethodNode, newClassNode);
		CommonUtils.saveFileFromString(newMethodOut, newMethodFile.getAbsolutePath());

	}
	public static void printOldMethodRecord(ClassMethodInfo newMethodInfo,List<ClassMethodInfo> cmis) throws IOException {
		if(dir == null) {
			return;
		}
		ClassNode newClassNode = newMethodInfo.mClassNode;
		final MethodNode newMethodNode = newMethodInfo.mMethodNode;
		File outDir = new File(dir, newClassNode.name + "/");
		if(!outDir.exists()) {
			outDir.mkdirs();
		}
		String oldMethodOut = "";
		String outName = newMethodNode.name;
		File oldMethodFile = new File(outDir, outName + "_old");
		for(ClassMethodInfo cmi : cmis) {
			oldMethodOut += cmi.toString() + "\r\n";
			oldMethodOut += getMethodRecord(cmi.mMethodNode, cmi.mClassNode);
		}
		CommonUtils.saveFileFromString(oldMethodOut, oldMethodFile.getAbsolutePath());
	}
}
