package com.tencent.optimusprime.jazz;

import java.io.File;
import java.io.IOException;
import java.util.List;

import com.tencent.jazz.tools.MethodRecordPrinter;
import com.tencent.jazz.util.CommonUtils;
import com.tencent.jazz.util.Log;
import com.tencent.optimusprime.ClassTreeHelper;
import com.tencent.retchet.Record;

/**
 * 主入口
 * @author noverguo
 */
public class Main {

	public static final int APK_MODE = 0;

	public static final int JAR_MODE = 1;

	public static int MODE = JAR_MODE;
	public static int VERSION = MergeControllCenter.Version2_1;

	public static String jarPath = null;
	public static String mappingPath = null;
	public static List<String> grepMethods = null;
	public static String mer_mappingPath = null;

	public static void main(String[] args) throws Exception {
		if (args == null || args.length < 1) {
			System.out
					.println("useage: <beMerge.jar> [-p mapping-pro.txt][-v version] [-o outputDir] [-g grepMethod.txt] [-m mapping-mer.txt]");
			return;
		}
		initArgs(args);
		long start = System.currentTimeMillis();

		Env.setSavePath(new File("bin/optimus_prime/"));
		MethodRecordPrinter.enable(new File("bin/optimus_prime/records/"));

		Log.i("jar: " + jarPath);
		Log.i("mapping: " + mappingPath);

		// 获得一颗树
		ClassTreeHelper classTreeHelper = new ClassTreeHelper();
		Env.classTreeHelper = classTreeHelper;
		classTreeHelper.loadJar(jarPath, mappingPath, grepMethods);

		if (mer_mappingPath == null) {
			// 类里所有私有方法进行合并
			MergeControllCenter.merger(VERSION);
		} else {
			 MergeControllCenter.merger(VERSION, Record.fromFile(new File(mer_mappingPath)));
		}

		// 写回到原文件
		classTreeHelper.saveJar(null);

		long runTime = (System.currentTimeMillis() - start) / 1000;
		long min = runTime / 60;
		long sec = runTime % 60;
		Log.i("run time: " + min + "min" + sec + "s");
		System.out.println("run time: " + min + "min" + sec + "s");
		Log.close();
	}

	public static void initArgs(String[] args) throws IOException {
		jarPath = args[0];

		String[] arr = new String[2];
		for (int i = 0, j = 0; i < args.length && j < arr.length; ++i) {
			if (args[i].startsWith("-")) {
				++i;
			} else {
				arr[j++] = args[i];
			}
		}

		for (int i = 0; i < args.length; ++i) {
			if ("-v".equals(args[i].trim())) {
				try {
					VERSION = Integer.parseInt(args[i + 1]);
					++i;
				} catch (NumberFormatException e) {
					throw new RuntimeException("version must be a number!", e);
				}
			} else if ("-o".equals(args[i].trim())) {
				Env.setOutDir(new File(args[i + 1]));
				++i;
			} else if ("-g".equals(args[i].trim())) {
				String grepMethodPath = args[i + 1];
				++i;
				grepMethods = CommonUtils.fileToList(grepMethodPath);
			} else if ("-m".equals(args[i].trim())) {
				mer_mappingPath = args[i + 1];
				++i;
			} else if ("-p".equals(args[i].trim())) {
				mappingPath = args[i + 1];
				++i;
				MODE = APK_MODE;
			}
		}
	}

}
