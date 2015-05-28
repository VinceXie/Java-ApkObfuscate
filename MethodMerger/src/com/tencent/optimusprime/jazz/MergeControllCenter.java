package com.tencent.optimusprime.jazz;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.charset.Charset;
import java.util.List;

import org.objectweb.asm.Opcodes;

import com.tencent.jazz.util.CommonUtils;
import com.tencent.jazz.util.Log;
import com.tencent.optimusprime.ClassTree;
import com.tencent.optimusprime.jazz.control.AbstractMergeControllCenter;
import com.tencent.retchet.MergerMap;
import com.tencent.retchet.Record;

/**
 * 总控制中心
 * @author noverguo
 */
public class MergeControllCenter implements Opcodes {
	
	/**
	 * 只合并类里私有函数
	 */
	public final static int Version1_1=1;
	/**
	 * 合并类里和类外的私有函数
	 */
	public final static int Version1_2=2;
	/**
	 * 合并类里和类外的私有函数
	 */
	public final static int Version2_1=3;
	/**
	 * 方法合并控制中心总入口
	 * @param version 版本<br/>
	 * 				Version1_1: 只作同一类里合并私有方法<br/>
	 * 				Version1_2: 在同一类里合并受限制的私有方法，合并所有孤立的私有方法
	 * 				Version2_1: 在同一类里合并受限制的方法，合并所有孤立的方法
	 * @throws Exception
	 */
	public static void merger(int version) throws Exception {
		merger(version, null);
	}
	public static void merger(int version, List<Record> records) throws Exception {
		Class<?> clazz = Class.forName("com.tencent.optimusprime.jazz.control.MergeControllCenterV" + version);
		AbstractMergeControllCenter inst = (AbstractMergeControllCenter) clazz.newInstance();
		inst.init(records);
		
		Log.i("run version: " + getVersion(version));
		ClassTree classTree = Env.rebuild();
		int allCount = classTree.getMethodInfos(ClassTree.FLAG_ALL, false).size();
		int allProguardCount = classTree.getMethodInfos(ClassTree.FLAG_ALL_PROGUARD, false).size();
		int staticCount = classTree.getMethodInfos(ClassTree.FLAG_STATIC).size();
		int privateStaticCount = classTree.getMethodInfos(ClassTree.FLAG_PRIVATE_STATIC).size();
		int instanceCount = classTree.getMethodInfos(ClassTree.FLAG_NOTSTATIC).size();
		int privateInstanceCount = classTree.getMethodInfos(ClassTree.FLAG_PRIVATE_NOTSTATIC).size();
		
		saveCountInfo("", allCount, allProguardCount, staticCount, privateStaticCount, instanceCount, privateInstanceCount);
		
		Log.i("合并前所有方法: " + allCount);
		Log.i("合并前所有混淆过的方法: " + allProguardCount);
		Log.i("合并前混淆过的所有静态方法: " + staticCount);
		Log.i("合并前混淆过的私有静态方法: " + privateStaticCount);
		Log.i("合并前混淆过的所有实例方法: " + instanceCount);
		Log.i("合并前混淆过的私有实例方法: " + privateInstanceCount);
		Log.i("合并前所有静态方法: " + classTree.getMethodInfos(ClassTree.FLAG_STATIC, false).size());
		Log.i("合并前私有静态方法: " + classTree.getMethodInfos(ClassTree.FLAG_PRIVATE_STATIC, false).size());
		Log.i("合并前所有实例方法: " + classTree.getMethodInfos(ClassTree.FLAG_NOTSTATIC, false).size());
		Log.i("合并前私有实例方法: " + classTree.getMethodInfos(ClassTree.FLAG_PRIVATE_NOTSTATIC, false).size());
		
		inst.run();
		
		classTree = Env.rebuild();
		allCount = classTree.getMethodInfos(ClassTree.FLAG_ALL, false).size();
		allProguardCount = classTree.getMethodInfos(ClassTree.FLAG_ALL_PROGUARD, false).size();
		staticCount = classTree.getMethodInfos(ClassTree.FLAG_STATIC).size();
		privateStaticCount = classTree.getMethodInfos(ClassTree.FLAG_PRIVATE_STATIC).size();
		instanceCount = classTree.getMethodInfos(ClassTree.FLAG_NOTSTATIC).size();
		privateInstanceCount = classTree.getMethodInfos(ClassTree.FLAG_PRIVATE_NOTSTATIC).size();
		
		saveCountInfo("Last", allCount, allProguardCount, staticCount, privateStaticCount, instanceCount, privateInstanceCount);
		writeCountApp();
		
		Log.i("合并后所有方法: " + allCount);
		Log.i("合并后混淆过的所有方法: " + allProguardCount);
		Log.i("合并后混淆过的所有静态方法: " + staticCount);
		Log.i("合并后混淆过的私有静态方法: " + privateStaticCount);
		Log.i("合并后混淆过的所有实例方法: " + instanceCount);
		Log.i("合并后混淆过的私有实例方法: " + privateInstanceCount);
		Log.i("合并后所有静态方法: " + classTree.getMethodInfos(ClassTree.FLAG_STATIC, false).size());
		Log.i("合并后私有静态方法: " + classTree.getMethodInfos(ClassTree.FLAG_PRIVATE_STATIC, false).size());
		Log.i("合并后所有实例方法: " + classTree.getMethodInfos(ClassTree.FLAG_NOTSTATIC, false).size());
		Log.i("合并后私有实例方法: " + classTree.getMethodInfos(ClassTree.FLAG_PRIVATE_NOTSTATIC, false).size());
		
		if(Env.savePath != null) {
			MergerMap.getInstant().saveMap(new File(Env.savePath.getAbsolutePath())+"/mapping-op.txt");
		}
	}

	public static String getVersion(int version) {
		switch(version) {
		case Version1_1:
			return "1.1";
		case Version1_2:
			return "1.2";
		case Version2_1:
			return "2.1";
		}
		return "error version";
	}
	
	private static void saveCountInfo(String fileLast, int allCount, int allProguardCount, int staticCount, int privateStaticCount, int instanceCount, int privateInstanceCount) throws IOException {
		if(Env.outDir == null) {
			return;
		}
		File lockFile = new File("file_lock");
		if (!lockFile.exists()) {
			lockFile.createNewFile();
		}
		FileOutputStream out = new FileOutputStream(lockFile);
		FileChannel channel = out.getChannel();
		FileLock lock = channel.lock();
		
		appendToFile(new File(Env.outDir, "allCount" + fileLast), allCount);
		appendToFile(new File(Env.outDir, "allProguardCount" + fileLast), allProguardCount);
		appendToFile(new File(Env.outDir, "staticCount" + fileLast), staticCount);
		appendToFile(new File(Env.outDir, "privateStaticCount" + fileLast), privateStaticCount);
		appendToFile(new File(Env.outDir, "instanceCount" + fileLast), instanceCount);
		appendToFile(new File(Env.outDir, "privateInstanceCount" + fileLast), privateInstanceCount);
		
		lock.release();
		channel.close();
		out.close();
		lockFile.delete();
	}
	
	private static void appendToFile(File file, int count) throws IOException {
		String preCount = readFromFile(file);
		CommonUtils.saveFileFromString(preCount + count, file.getAbsolutePath());
	}
	
	private static String readFromFile(File file) throws IOException {
		if (file.exists()) {
			return CommonUtils.fileToString(file) + " ";
		}
		return "";
	}


	private static void writeCountApp() throws IOException {
		if(Env.outDir == null) {
			return;
		}
		String dirPath = Env.outDir.getAbsolutePath();
		if(!dirPath.endsWith("/") && !dirPath.endsWith("\\")) {
			dirPath += File.separator;
		}
		File countBat = new File(Env.outDir, "统计结果.bat");
		String content = "@echo off\r\njava -jar " + dirPath + "count_result.jar " + dirPath 
				+ "\r\necho ------------------------------------------------\r\necho    计算结果保存在 " 
				+ dirPath + "op_result.txt\r\necho ------------------------------------------------\r\npause";
		CommonUtils.saveFileFromString(content, countBat.getAbsolutePath(), Charset.forName("gbk"));
		InputStream is = Main.class.getResourceAsStream("/res/count_result.jar");
		CommonUtils.saveFileFromInputStream(is, dirPath + "count_result.jar");
	}
}
