package com.tencent.obfuscate.hide;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.lingala.zip4j.core.ZipFile;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import com.tencent.obfuscate.pool.MClassNode;
import com.tencent.obfuscate.pool.Pool;

/**
 * @author VinceXie vince.xie@qq.com
 * @version 2014-11-26 上午11:58:05 State
 */
public class Hide {

	public static ArrayList<String> listHidenNode = new ArrayList<String>();

	public static String pathOutSmali = "out/smali";
	public static String pathOut = "out";
	public static String pathHide = "hide";
	public static String cmdBackSmali = "java -jar Tools/apktool.jar d -o out ";
	public static String cmdSmaliOut = "java -jar Tools/apktool.jar b -o new.apk out";
	public static String cmdSmaliHide = "java -jar Tools/smali.jar -o classes.dex hide";
	public static String cmdSmaliHide2 = "java -jar Tools/smali.jar -o classes.dex out/smali";
	public static String strDex = "classes.dex";
	public static String strHideDex = "classes.zip";
	public static String strAssets = "assets";
	public static String strOldApk;
	public static String strNewApk = "new.apk";
	public static String strNewApkZip = "new.zip";
	public static String pathAMXL = "out/AndroidManifest.xml";
	public static String cmdUpdateDex = "Tools/7z/7z u " + strNewApkZip + " "
			+ strDex;
	public static String cmdUpdateJar = "Tools/7z/7z u " + strNewApkZip + " "
			+ strAssets;
	public static String cmdZipDex = "Tools/7z/7z a classes.zip classes.dex";

	public static String smaliNewApplication = "NApplication.zip";
	public static String smaliShellApplication = "SApplication.zip";
	public static String smaliTargetSA = pathOutSmali
			+ "/hide/TApplication.smali";

	public static String smaliMultidex = "MultiDex.zip";
	public static String smaliShell = "hide.zip";
	public static String smaliTargetM = pathOutSmali + "/" + smaliMultidex;

	public static String txtMulSmali = "ab.txt";
	public static String txtInitSmali = "init.txt";
	public static String strMulti = "multidex";
	public static String strAttachBase = ".method protected attachBaseContext(Landroid/content/Context;)V";
	public static String strNewAttachBase = ".method public attachBaseContext(Landroid/content/Context;)V";
	public static String strInsertMul = "invoke-static {p0}, Landroid/support/multidex/t/MultiDex;->install(Landroid/content/Context;)V";

	public static String newline = System.getProperties().getProperty(
			"line.separator");

	public static String aplctn;
	public static ArrayList<String> fldApl = new ArrayList<String>();
	public static String strNewInit = ".method public newinit()V";
	public static String strInsertInit;

	public static String in = "hide.TApplication";
	public static String strShellApl = "Lcom/example/abc/MyApplication;";

	public static void Clean() {
		File dirOut = new File(pathOut);
		File dirHide = new File(pathHide);

		deleteFile(dirOut);
		deleteFile(dirHide);
		deleteFile(new File(strAssets));
		deleteFile(new File(strDex));
		deleteFile(new File(strHideDex));
	}

	public static void main(String[] args) throws Exception {
		if (args == null || args.length < 1) {
			System.out.println("useage: <beHide.jar>");
			return;
		}

		strOldApk = args[0];
		Clean();

		File dirOut = new File(pathOutSmali);
		File dirHide = new File(pathHide);
		File apkNew = new File(strNewApk);
		File zipNew = new File(strNewApkZip);
		;
		File zip = new File(strHideDex);
		File axml = new File(pathAMXL);

		RunCommand.Run(cmdBackSmali + strOldApk, false);


		AXML.Handle2(axml);


		
		
		// hide代码压回dex并压缩在zip
		RunCommand.Run(cmdSmaliHide2, false);
		RunCommand.Run(cmdZipDex, false);
		// TODO 对classes.zip加密

		// 删掉原代码
		deleteFile(dirOut);
		// 插入壳
		InsertShell();
		
		// out代码压回dex
		RunCommand.Run(cmdSmaliOut, false);


		// 转化为zip并更新hide代码进去
		apkNew.renameTo(zipNew);
		FileUtils.moveFileToDirectory(zip, new File(strAssets), true);
		RunCommand.Run(cmdUpdateJar, false);
		
		// zip转化为apk
		zipNew.renameTo(apkNew);

		Clean();
	}

	public static void PacCode(File dirOut) throws Exception
	{
		
	}
	
	public static void HandleOut(File dirOut) throws Exception {
		// 找出out文件夹中继承object或非multidex或fld的记录下来并删掉
		for (File file : FileUtils.listFiles(dirOut, new String[] { "smali" },
				true)) {
			BufferedReader br = new BufferedReader(new InputStreamReader(
					new FileInputStream(file)));
			int i = 0;
			String strClass = null;
			for (String line = br.readLine(); line != null; line = br
					.readLine()) {
				if (i == 0) {
					// 属于multidex部分不隐藏
					if (line.contains(strMulti)) {
						break;
					}
					// 属于apl的fld部分不隐藏
					boolean find=false;
					for (String str : fldApl) {
						if (line.contains(str)) {
							find=true;
							break;
						}
					}
					if(find)
					{
						break;
					}

					strClass = line;
					i++;
					continue;
				} else if (i == 1) {
					br.close();
					if (line.contains(".super Ljava/lang/Object;")) {

						file.delete();
						listHidenNode.add(strClass);
					}
					break;
				}

			}

		}
	}

	public static void HandleHide(File dirHide) throws Exception {
		// 找出hide文件夹中非需要隐藏的删掉
		for (File file : FileUtils.listFiles(dirHide, new String[] { "smali" },
				true)) {
			BufferedReader br = new BufferedReader(new InputStreamReader(
					new FileInputStream(file)));

			for (String line = br.readLine(); line != null;) {

				br.close();
				if (!listHidenNode.contains(line)) {
					file.delete();
				}
				break;

			}

		}
	}

	public static void InsertShell() throws Exception {
		InputStream is = Hide.class.getResourceAsStream(smaliShell);
		FileUtils.copyInputStreamToFile(is, new File(smaliShell));
		ZipFile zipFile = new ZipFile(smaliShell);
		zipFile.extractAll(pathOutSmali);
		new File(smaliShell).delete();

	}
	
	public static void InsertMulti() throws Exception {
		InputStream is = Hide.class.getResourceAsStream(smaliMultidex);
		FileUtils.copyInputStreamToFile(is, new File(smaliMultidex));
		ZipFile zipFile = new ZipFile(smaliMultidex);
		zipFile.extractAll(pathOutSmali);
		new File(smaliMultidex).delete();

	}

	public static void CreateApl() throws Exception {
		InputStream is = Hide.class.getResourceAsStream(smaliNewApplication);
		FileUtils.copyInputStreamToFile(is, new File(smaliNewApplication));
		ZipFile zipFile = new ZipFile(smaliNewApplication);
		zipFile.extractAll(pathOutSmali);
		new File(smaliNewApplication).delete();
	}

	public static void InsertApl(File dirOut, String apl) throws Exception {
		// // 释放壳
		// InputStream iss =
		// Hide.class.getResourceAsStream(smaliShellApplication);
		// FileUtils.copyInputStreamToFile(iss, new
		// File(smaliShellApplication));
		// ZipFile zipFile = new ZipFile(smaliShellApplication);
		// zipFile.extractAll(pathOutSmali);
		// new File(smaliShellApplication).delete();
		//
		// // 处理壳里的内容
		// File filShell = new File(smaliTargetSA);
		// String sh = FileUtils.readFileToString(filShell);
		// apl = "L" + Dot2Line(apl) + ";";
		// sh = sh.replace(strShellApl, apl);
		// FileUtils.write(filShell, sh);
		//
		// // 处理原本的apl
		// for (File file : FileUtils.listFiles(dirOut, new String[] { "smali"
		// },
		// true)) {
		//
		// String oldApl = FileUtils.readFileToString(file);
		// // 判断是否为apl
		// if (!oldApl.startsWith(".class public " + apl)) {
		// continue;
		// }
		// // attachbase改为public
		// if(oldApl.contains(strAttachBase))
		// {
		// oldApl=oldApl.replace(strAttachBase, strNewAttachBase);
		// FileUtils.write(file, oldApl);
		// break;
		// }
		// else {
		// InputStream is=Hide.class.getResourceAsStream(txtMulSmali);
		// FileUtils.write(file, IOUtils.toString(is), true);
		// }
		//
		// }

		for (File file : FileUtils.listFiles(dirOut, new String[] { "smali" },
				true)) {
			BufferedReader br = new BufferedReader(new InputStreamReader(
					new FileInputStream(file)));
			int i = 0;
			// 需要添加attach base
			boolean needJoin = true;
			boolean target = false;
			boolean hasAB = false;
			boolean hasInit = false;
			String strNew = null;
			for (String line = br.readLine(); line != null; line = br
					.readLine()) {
				// 判断是否apl
				if (i == 0 && !line.contains(Dot2Line(apl) + ";")) {
					break;
				} else if (i == 0) {
					i++;
					needJoin = true;
					target = true;
					strNew = line;
					continue;
				}
				
				String reg="L.*?;";
				Pattern p=Pattern.compile(reg);
				Matcher m=p.matcher(line);
				while(m.find())
				{
					System.out.println(m.group());
					fldApl.add(m.group());
				}
				// 判断fld
				if (line.startsWith(".field ")) {
					fldApl.add(line.split(":")[1]);
				}

//				// 判断init
//				if (line.startsWith(".method public constructor <init>()V")) {
//					hasInit = true;
//					line = strNewInit;
//				}
//				if (line.equals("    invoke-direct {p0}, Landroid/app/Application;-><init>()V")
//						&& hasInit) {
//					continue;
//				}
//				if (line.contains(".end method") && hasInit) {
//					hasInit = false;
//				}

				// 判断attachbase
				if (line.contains(strAttachBase)) {
					needJoin = false;
					hasAB = true;
					strNew = strNew + line + newline;
					continue;
				}
				if (line.startsWith("    return-void") && hasAB) {
					strNew = strNew + strInsertMul + newline + strInsertInit
							+ newline + line + newline;
					continue;
				}
				if (line.startsWith(".end method") && hasAB) {
					strNew = strNew + line + newline;
					hasAB = false;
					continue;
				}
				strNew = strNew + line + newline;
			}

			if (target && !needJoin) {
				file.delete();
				FileUtils.write(file, strNew);
//				InputStream is = Hide.class.getResourceAsStream(txtInitSmali);
//				FileUtils.write(file, IOUtils.toString(is), true);
			}

			if (target && needJoin) {
				InputStream is = Hide.class.getResourceAsStream(txtMulSmali);
				String ab = IOUtils.toString(is);
				ab = ab.replace("return-void", strInsertMul + newline
						+ "return-void" + newline);
				FileUtils.write(file, ab, true);
			}
			br.close();

		}

	}

	public static void Parse(Pool pool) {
		ArrayList<MClassNode> classNodes = pool.classNodes;

		for (MClassNode classNode : classNodes) {
			classNode.methodInfos.clear();
			classNode.fieldInfos.clear();

			// boolean canHide=false;
			if (classNode.superName.equals("java/lang/Object")) {

				listHidenNode.add(classNode.name);
				// mapName2Node.put(classNode.name,classNode);
				// mapClass2Var.put(classNode.name, "cls"+System.nanoTime());
				// canHide=true;

			}

			// for (MethodNode methodNode : classNode.methods) {
			// MethodInfo methodInfo = new MethodInfo(methodNode, classNode);
			// classNode.methodInfos.add(methodInfo);
			// // if(canHide)
			// // {
			// //
			// mapMethod2Var.put(classNode.name+methodNode.name+methodNode.desc,
			// "mtd"+System.nanoTime());
			// // }
			// }

			// for (FieldNode fieldNode : classNode.fields) {
			// FieldInfo fieldInfo = new FieldInfo(classNode, fieldNode);
			// classNode.fieldInfos.add(fieldInfo);
			// // if(canHide)
			// // {
			// // mapField2Var.put(classNode.name+fieldNode.name,
			// "fld"+System.nanoTime());
			// // }
			//
			// }
		}

	}

	private static void deleteFile(File file) {
		if (file.exists()) {
			if (file.isFile()) {
				file.delete();
			} else if (file.isDirectory()) {
				File files[] = file.listFiles();
				for (int i = 0; i < files.length; i++) {
					deleteFile(files[i]);
				}
			}
			file.delete();
		} else {
			System.out.println("所删除的文件不存在！" + '\n');
		}
	}

	/**
	 * 保存的jar路径，若为空，保存为原来加载的路径。
	 * 
	 * @param jarPath
	 */
	public static void SaveJar(String jarPath, File[] files) {

		FileOutputStream fileOutputStream;
		JarOutputStream jarOutputStream;
		try {
			fileOutputStream = new FileOutputStream(new File(jarPath));
			jarOutputStream = new JarOutputStream(fileOutputStream);

			for (File file : files) {
				jarOutputStream.putNextEntry(new JarEntry(file.getName()));
				jarOutputStream.write(IOUtils.toByteArray(new FileInputStream(
						file)));

			}
			jarOutputStream.close();
			fileOutputStream.close();

		} catch (Exception e) {

			e.printStackTrace();
		}

	}

	public static String Dot2Line(String dot) {
		return dot.replace(".", "/");
	}
}
