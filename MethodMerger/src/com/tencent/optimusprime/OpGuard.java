package com.tencent.optimusprime;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;

import com.tencent.optimusprime.jazz.Env;
import com.tencent.optimusprime.jazz.MergeControllCenter;
import com.tencent.retchet.MergerMap;
import com.tencent.retchet.Record;

/**
 * @author VinceXie vince.xie@qq.com
 * @version 2014-8-14 下午5:11:11 State
 */
public class OpGuard {

	// 以下为注入点
	public static String mapPath;

	public static String outPath;

	public static String applyMapPath;

	//
	public static int Version = MergeControllCenter.Version2_1;

	public static void Merger() throws Exception {

		System.out.println("Merging...");
		// 运行merger
		ClassTreeHelper classTreeHelper = new ClassTreeHelper();
		Env.classTreeHelper = classTreeHelper;
		classTreeHelper.loadJar(outPath, mapPath, null);
		ClassTree classTree = classTreeHelper.buildTree();
		Env.classTree = classTree;
		Env.setSavePath(new File(outPath + "/.."));
		Env.setOutDir(new File("D:\\Opguard"));

		if (applyMapPath != null) {		
			MergeControllCenter.merger(Version, readApplyMap(new File(applyMapPath)));
		} else {
			MergeControllCenter.merger(Version);
		}
		// 写回到原文件
		classTreeHelper.saveJar(null);

		MergerMap.appendMap(mapPath);
	}

	public static ArrayList<Record> readApplyMap(File file) {
		ArrayList<Record> records = new ArrayList<Record>();
		try {
			System.out.println("readApplyMap!");
			boolean fen = false;
			BufferedReader br = new BufferedReader(new InputStreamReader(
					new FileInputStream(file)));
			for (String line = br.readLine(); line != null; line = br
					.readLine()) {
				if (line.contains("-----")) {
					fen = true;
					continue;
				}
				if (fen) {
					records.add(new Record(line));
				} 
			}
			br.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
		System.out.println(records);
		return records;
	}
}
