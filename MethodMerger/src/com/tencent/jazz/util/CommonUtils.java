package com.tencent.jazz.util;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * 通用工具类
 * @author noverguo
 */
@SuppressWarnings("rawtypes")
public class CommonUtils {
	public static Charset sCharset = Charset.forName("UTF-8");
	public static int getSize(Object[] arr) {
		if(arr == null)
			return 0;
		return arr.length;
	}
	public static int getSize(Collection coll) {
		if(coll == null)
			return 0;
		return coll.size();
	}
	public static int getSize(Map map) {
		if(map == null)
			return 0;
		return map.size();
	}
	public static boolean isEmpty(Collection coll) {
		return coll == null || coll.isEmpty();
	}
	public static boolean isEmpty(Map map) {
		return map == null || map.isEmpty();
	}
	public static void inputStreamToOutputStream(InputStream in, OutputStream out) throws IOException {
		inputStreamToOutputStream(in, out, false);
	}
	public static void inputStreamToOutputStream(InputStream in, OutputStream out, boolean close) throws IOException {
		try {
			byte[] buf = new byte[4096];
			int len = -1;
			while((len = in.read(buf)) > 0) {
				out.write(buf, 0, len);
			}
		} finally {
			if(close) {
				in.close();
				out.close();
			}
		}
	}

	public static void copy(File srcFile, File destFile) throws FileNotFoundException, IOException {
		inputStreamToOutputStream(new FileInputStream(srcFile), new FileOutputStream(destFile), true);
	}
	
	public static String inputStreamToString(InputStream is) throws IOException {
		return inputStreamToString(is, sCharset);
	}

	public static String inputStreamToString(InputStream is, Charset charset) throws IOException {
		if (is == null) {
			return null;
		}
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		try {
			byte[] buf = new byte[8];
			int len = -1;

			while ((len = is.read(buf)) > -1) {
				out.write(buf, 0, len);
			}
		} finally {
			is.close();
		}

		return new String(out.toByteArray(), charset);
	}

	public static void inputStreamToOutputStream(String src, String dest) throws IOException {
		// FileUtils.create(dest);
		inputStreamToOutputStream(new FileInputStream(src), new FileOutputStream(dest));
	}

	public static String fileToString(File path, Charset charset) throws IOException {
		return fileToString(path.getAbsolutePath(), charset);
	}

	public static String fileToString(String path, Charset charset) throws IOException {
		return inputStreamToString(new FileInputStream(path), charset);
	}

	public static String fileToString(File path) throws IOException {
		return fileToString(path.getAbsolutePath());
	}

	public static String fileToString(String path) throws IOException {
		String res = inputStreamToString(new FileInputStream(path));
		return res;
	}

	public static void saveFileFromInputStream(InputStream is, String path) throws IOException {
//		FileUtils.create(path);
		FileOutputStream fos = new FileOutputStream(path);
		inputStreamToOutputStream(is, fos);
	}

	public static void saveFileFromString(String value, String path, Charset charset) throws IOException {
//		FileUtils.create(path);
		FileOutputStream out = new FileOutputStream(path);
		saveFileFromString(value, out, charset);
	}
	public static void saveFileFromString(String value, String path) throws IOException {
		saveFileFromString(value, path, sCharset);
	}

	public static void saveFileFromString(String value, FileOutputStream out, Charset charset) throws IOException {
		InputStream in = new ByteArrayInputStream(value.getBytes(charset));
		inputStreamToOutputStream(in, out);
	}
	public static void saveFileFromString(String value, FileOutputStream out) throws IOException {
		saveFileFromString(value, out, sCharset);
	}
	public static void makesureDirExist(File dir) {
		if(!dir.exists()) {
			dir.mkdirs();
		}
	}
	public static List<String> fileToList(String grepMethodPath) throws IOException {
		List<String> res = new ArrayList<String>();
		BufferedReader br = new BufferedReader(new FileReader(grepMethodPath));
		String line = br.readLine();
		while(line != null) {
			res.add(line);
			line = br.readLine();
		}
		br.close();
		return res;
	}
	public static boolean isEmpty(Object[] firstRetType) {
		if(firstRetType == null || firstRetType.length == 0) {
			return true;
		}
		return false;
	}
}
