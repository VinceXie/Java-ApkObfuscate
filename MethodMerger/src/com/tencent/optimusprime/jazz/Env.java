package com.tencent.optimusprime.jazz;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.util.List;

import com.tencent.jazz.util.ASMTools;
import com.tencent.jazz.util.CommonUtils;
import com.tencent.jazz.util.Log;
import com.tencent.optimusprime.ClassTree;
import com.tencent.optimusprime.ClassTreeHelper;
import com.tencent.optimusprime.MethodInfo;

/**
 * 全局环境
 * @author noverguo
 */
public class Env {
	public static File savePath = null;
	public static File outDir = null;
	static {
		if(savePath != null) {
			CommonUtils.makesureDirExist(savePath);
		}
		if(outDir != null) {
			CommonUtils.makesureDirExist(outDir);
		}
	}
	public static ClassTree classTree;
	public static ClassTreeHelper classTreeHelper;
	public static ClassTree rebuild() {
		classTree = classTreeHelper.buildTree();
		ASMTools.allMethodNames.clear();
		List<MethodInfo> methodInfos = classTree.getMethodInfos(ClassTree.FLAG_ALL);
		for(MethodInfo mi : methodInfos) {
			ASMTools.allMethodNames.add(ASMTools.toMethodNameString(mi.classNode, mi.methodNode));
		}
		return classTree;
	}
	private static boolean init = false;
	public static void setSavePath(File path) throws IOException {
		savePath = path;
		if(savePath != null) {
			CommonUtils.makesureDirExist(savePath);
		}
		if(init) {
			Log.close();
		}
		init = true;
		// 输出日志文件
		Log.sOut = new PrintStream(new File(Env.savePath, "op_result.out"));
	}
	public static void setOutDir(File path) {
		outDir = path;
		if(outDir != null) {
			CommonUtils.makesureDirExist(outDir);
		}
	}
}
