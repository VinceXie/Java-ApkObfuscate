package com.tencent.retchet;

import java.io.File;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.LineNumberNode;
import org.objectweb.asm.tree.MethodNode;

import com.tencent.retchet.util.ASMUtils;

/**
 * @author VinceXie vince.xie@qq.com
 * @version 2014-7-22 下午3:59:02 State
 */
public class MergerMap {

	public static HashMap<String, String> messClassMap=new HashMap<String, String>();
	
	public static String recordMark="-----";

	public static String messRecordMark="#MessRecordMark";
	
	private static MergerMap instant = null;

	public static final String MagicNum = "_81";

	public static ArrayList<Record> records = new ArrayList<Record>();

	public static MergerMap getInstant() {
		if (instant == null) {
			instant = new MergerMap();
		}
		return instant;
	}

	/**
	 * 在合并前操作偏移量和新范围，并记录。
	 * 
	 * @param newClassName
	 * @param methodNodes
	 * @param argsMap
	 */
	public void recordBeforeMerge(ClassNode newClassNode, MethodNode newMethodNode) {
		String methodName = newMethodNode.name;
		String methodDesc = newMethodNode.desc;
		if (methodName.contains(MagicNum)) {
			int start = -1;
			int end = -1;
			for (int i = 0; i < newMethodNode.instructions.size(); i++) {

				AbstractInsnNode abstractInsnNode = newMethodNode.instructions.get(i);
				if (abstractInsnNode.getType() == AbstractInsnNode.LINE) {
					LineNumberNode lineNumberNode = (LineNumberNode) abstractInsnNode;
					if (start == -1) {
						start = lineNumberNode.line;
					}
					end = lineNumberNode.line;
				}
			}

			methodName = methodName.replace(MagicNum, "");
			Type[] argTypes = Type.getArgumentTypes(methodDesc);
			Type retType = Type.getReturnType(methodDesc);
			String argDesc = "(";
			for (int i = 1; i < argTypes.length; ++i) {
				argDesc += argTypes[i].getDescriptor();
			}
			argDesc += ")" + retType.getDescriptor();
			methodDesc = argDesc;
			records.add(new Record(newClassNode.name, newMethodNode.name, newMethodNode.desc, newClassNode.name, methodName, methodDesc, 0, start, end, Record.INSTANCE_TO_STATIC, null));
		}
	}
	
	public void recordBeforeMerge(ClassNode newClassNode, MethodNode newMethodNode, ArrayList<ClassNode> classNodes, ArrayList<MethodNode> methodNodes, Map<String, NewOldMethodArgMapInfo> argsMap) {
		int offset = 0;
		int start = 0;
		int end = 0;
		int j = 0;
		for (MethodNode methodNode : methodNodes) {
			int pos = 0;
			ClassNode cn = classNodes.get(j);
			String key = ASMUtils.toString(cn, methodNode);
			for (int i = 0; i < methodNode.instructions.size(); i++) {

				AbstractInsnNode abstractInsnNode = methodNode.instructions.get(i);
				if (abstractInsnNode.getType() == AbstractInsnNode.LINE) {
					LineNumberNode lineNumberNode = (LineNumberNode) abstractInsnNode;
					if (pos == 0) {
						offset = end + 1 - lineNumberNode.line;
						start = lineNumberNode.line + offset;
					}
					lineNumberNode.line = lineNumberNode.line + offset;
					pos = lineNumberNode.line;
				}
			}
			end = pos;
			String methodName = methodNode.name;
			String methodDesc = methodNode.desc;
			if(argsMap != null) {
				records.add(new Record(newClassNode.name, newMethodNode.name, newMethodNode.desc, cn.name, methodName, methodDesc, offset, start, end, Record.STATIC_TO_MERGED_V1, argsMap.get(key)));
			} else {
				records.add(new Record(newClassNode.name, newMethodNode.name, newMethodNode.desc, cn.name, methodName, methodDesc, offset, start, end, Record.STATIC_TO_MERGED_V2, j));
			}
			j++;
		}
	}

	/**
	 * 保存到文件 用于merger
	 * 
	 * @param path
	 * @throws Exception
	 */
	public void saveMap(String path) throws Exception {
		PrintWriter pw = new PrintWriter(path);
		pw.println(recordMark);
		for (Record record : records) {
			pw.println(record.toString());
		}
		pw.println(messRecordMark);
		for(String orgName:messClassMap.keySet())
		{
			MessRecord messRecord=new MessRecord(orgName,messClassMap.get(orgName));
			pw.println(messRecord.toString());
		}
		pw.close();
	}

	/**
	 * 追加到文件 用于opguard
	 * 
	 * @param path
	 * @throws Exception
	 */
	public static void appendMap(String path) throws Exception {
		File file = new File(path);
		FileWriter fileWriter = new FileWriter(file, true);
		PrintWriter pw = new PrintWriter(fileWriter);
		pw.println(recordMark);
		for (Record record : records) {
			pw.println(record.toString());
		}
		pw.println(messRecordMark);
		for(String orgName:messClassMap.keySet())
		{
			MessRecord messRecord=new MessRecord(orgName,messClassMap.get(orgName));
			pw.println(messRecord.toString());
		}
		pw.close();
		fileWriter.close();
	}
}
