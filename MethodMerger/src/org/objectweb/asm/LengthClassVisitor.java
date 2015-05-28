package org.objectweb.asm;

import java.lang.reflect.Field;

import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodNode;

/**
 * 方法指令长度计算
 * @author noverguo
 */
public class LengthClassVisitor extends ClassVisitor implements Opcodes {
	private MethodNode mn;
	private int len = -1;
	public LengthClassVisitor(ClassNode cn, MethodNode mn) {
		super(ASM5);
		this.mn = mn;
		cn.accept(this);
	}

	@Override
	public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
		if(name.equals(mn.name) && desc.equals(mn.desc)) {
			MethodWriter mw = new MethodWriter(new ClassWriter(ASM5), access, name, desc, signature, exceptions, true, false);
			Field field = null;
			try {
				field = MethodWriter.class.getDeclaredField("code");
				field.setAccessible(true);
				ByteVector bv = (ByteVector) field.get(mw);
				len = bv.length;
			} catch (Exception e) {
				throw new RuntimeException(e);
			}
		}
		return null;
	}
	
	
	public int getLength() throws Exception {
		return len;
	}
	
}
