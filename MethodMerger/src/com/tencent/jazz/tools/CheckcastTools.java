package com.tencent.jazz.tools;

import java.util.ArrayList;
import java.util.List;

import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.TypeInsnNode;

/**
 * 与Checkcast相关
 * @author noverguo
 */
public class CheckcastTools implements Opcodes {

	public static List<String> getCheckcastClasses(MethodNode methodNode) {
		List<String> res = new ArrayList<String>();
		InsnList insnList = methodNode.instructions;
		for(int i=0;i<insnList.size();++i) {
			AbstractInsnNode insnNode = insnList.get(i);
			if(insnNode.getOpcode() == CHECKCAST && insnNode.getType() == AbstractInsnNode.TYPE_INSN) {
				TypeInsnNode typeNode = (TypeInsnNode)insnNode;
				res.add(typeNode.desc);
			}
		}
		return res;
	}
}
