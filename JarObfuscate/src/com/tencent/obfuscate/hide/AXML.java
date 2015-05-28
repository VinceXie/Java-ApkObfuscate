package com.tencent.obfuscate.hide;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;

/**
 * @author VinceXie vince.xie@qq.com
 * @version 2014-12-12 下午5:36:28 State
 */
public class AXML {

	// 匹配application标签
	public static String regex1 = "<application(.|\\n|\\r)*?android:name=\".*?\"(.|\\n|\\r)*?>";
	// 匹配android:name标签
	public static String regex3 = "android:name=\".*?\"";
	// 匹配包名
	public static String regex2 = "package=\".*?\"";

	public static String target = "<application";

	public static String replacement = "<application android:name=\"hide.MyProxyApplication\"";

	public static String in = "hide.MyProxyApplication";
	
	public static String metadata="<meta-data android:name=\"DELEGATE_APPLICATION_CLASS_NAME\" android:value=\"***\"/>";
	
	public static String strAppEnd="</application>";
	
	public static String txtMeta="meta-data.txt";

	public static void main(String[] args) throws IOException {
		System.out.println("test regex");
		Handle(new File(args[0]));

	}

	/**
	 * 对AndroidManifest处理。
	 * 
	 * @param file
	 *            AndroidManifest如果没有Application
	 *            android:name。则重构添加一个为我们的android:name。
	 * @return 是否有自定义入口
	 * @throws IOException
	 */
	public static String Handle(File file) throws IOException {
		String axml = FileUtils.readFileToString(file);
		String apl = null;
		String pkg = null;
		String allApl = null;

		Pattern p = Pattern.compile(regex1);
		Matcher m = p.matcher(axml);

		if (m.find()) {
			apl = m.group();
		} else {
			throw new RuntimeException();
		}

		// 找出Application的入口
		p = Pattern.compile(regex3);
		m = p.matcher(apl);
		if (m.find()) {
			apl = m.group().split("\"")[1];
		} else {
			// 找不到的情况下添加一个
			axml.replace(target, replacement);
			return null;
		}

		// 找到的话替换并获取出来
		if (apl.charAt(0) == '.') {
			p = Pattern.compile(regex2);
			m = p.matcher(axml);

			if (m.find()) {
				pkg = m.group().split("\"")[1];
			}
			axml = axml.replaceFirst(apl, in);

			allApl = pkg + apl;
		} else if (!apl.contains(".")) {
			p = Pattern.compile(regex2);
			m = p.matcher(axml);

			if (m.find()) {
				pkg = m.group().split("\"")[1];
			}
			axml = axml.replaceFirst(apl, in);

			allApl = pkg + "." + apl;
		} else {
			axml = axml.replaceFirst(apl, in);

			allApl = apl;
		}

		//FileUtils.write(file, axml);
		return allApl;

	}
	
	/**
	 * 新方法，利用加壳技术。
	 * @param file
	 * @throws IOException
	 */
	public static void Handle2(File file) throws IOException {
		String axml = FileUtils.readFileToString(file);
		String apl = null;

		// 找出Application标签
		Pattern p = Pattern.compile(regex1);
		Matcher m = p.matcher(axml);
		if (m.find()) {
			apl = m.group();
		} else {
			return;
		}

		// 找出Application name标签
		p = Pattern.compile(regex3);
		m = p.matcher(apl);
		if (m.find()) {
			apl = m.group().split("\"")[1];
		} else {
			return ;
		}

		// 找到的话替换并添加metadata
		axml=axml.replace("android:name=\""+apl+"\"", "android:name=\"hide.MyProxyApplication\"");
		InputStream is = Hide.class.getResourceAsStream(txtMeta);
		String meta=IOUtils.toString(is);
		meta=meta.replace("NeedToReplace", apl);
		axml=axml.replace("</application>", meta);


		FileUtils.write(file, axml);
		return;

	}
}
