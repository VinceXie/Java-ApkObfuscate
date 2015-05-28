package com.tencent.jazz;

import java.io.File;
import java.util.List;

import org.objectweb.asm.Opcodes;

import com.tencent.jazz.util.CommonUtils;

/**
 * 根据Mapping进行修复
 * @author noverguo
 */
public class Main extends ClassLoader implements Opcodes {
	private static void printUseage() {
		System.out.println("useage: <jarPath> <mappingFilePath> ");
	}
	
	public static void main(String[] args) throws Exception {
		List<String> mappingInfo = null; 
		String jarPath = null;
		if(args.length == 2 && new File(args[1]).exists()) {
			jarPath = args[0];
			mappingInfo = CommonUtils.fileToList(args[1]);
		} else {
			printUseage();
			System.exit(-1);
		}
		MergedMethodCallerFixer.fix(mappingInfo, jarPath);
	}

}
