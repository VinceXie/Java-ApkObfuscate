package com.tencent.obfuscate.pool;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.JarOutputStream;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.tree.ClassNode;

import com.tencent.obfuscate.Configuration;
import com.tencent.obfuscate.jar.JarObfuscator2;



/**
 * @author VinceXie vince.xie@qq.com
 * @version 2014-10-16 下午5:22:57
 * State
 */
public class Pool {

	String jarPath = null;

	ArrayList<byte[]> otherEntries = new ArrayList<byte[]>();
	ArrayList<String> otherNames = new ArrayList<String>();
	
	public ArrayList<MClassNode> classNodes=new ArrayList<MClassNode>();

	/**
	 * 加载jar文件
	 */
	public static Pool loadJar() {

		Pool pool=new Pool();
		String jarPath = Configuration.jarPath;
		try {


			pool.jarPath = jarPath;
			MClassNode classNode;
			JarFile jfile = new JarFile(jarPath);
			Enumeration<?> files = jfile.entries();
			// 读取jar里所有class文件
			while (files.hasMoreElements()) {
				JarEntry entry = (JarEntry) files.nextElement();
				if (entry.getName().endsWith(".class")) {
					InputStream in;

					in = jfile.getInputStream(entry);

					ClassReader cr = new ClassReader(in);
					classNode = new MClassNode();
					// XXX ClassReader.SKIP_DEBUG| 决定了是否保留行号！
					cr.accept(classNode, ClassReader.SKIP_FRAMES);
					in.close();

					pool.classNodes.add(classNode);
				} else {
					InputStream in;

					in = jfile.getInputStream(entry);
					ByteArrayOutputStream bos = new ByteArrayOutputStream();
					int next = in.read();
					while (next > -1) {
						bos.write(next);
						next = in.read();
					}
					bos.flush();
					byte[] result = bos.toByteArray();
					pool.otherEntries.add(result);
					pool.otherNames.add(entry.getName());
				}
			}
		} catch (IOException e) {
			e.printStackTrace();
			System.out.println("load jar error.");
		}
		Configuration.read();
		
		return pool;
	}


	/**
	 * 保存的jar路径，若为空，保存为原来加载的路径。
	 * 
	 * @param jarPath
	 */
	public void saveJar(String jarPath) {
		if (jarPath == null) {
			jarPath = this.jarPath;
		}
		FileOutputStream fileOutputStream;
		JarOutputStream jarOutputStream;
		try {
			fileOutputStream = new FileOutputStream(new File(jarPath));
			jarOutputStream = new JarOutputStream(fileOutputStream);
			for (ClassNode classNode : classNodes) {
				String classPath;
				classPath = classNode.name.replace(".", "/");
				classPath = classPath + ".class";
				JarEntry jarEntry = new JarEntry(classPath);
				jarOutputStream.putNextEntry(jarEntry);
				ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_MAXS);
				classNode.accept(cw);
				byte[] code = cw.toByteArray();
				AsmVerify.check(code);
				jarOutputStream.write(code);
			}
			
			//插入一个自己的类
//			String classPath=JarObfuscator2.className+".class";
//			JarEntry jarEntry = new JarEntry(classPath);
//			jarOutputStream.putNextEntry(jarEntry);
//			jarOutputStream.write(JarObfuscator2.dump());
			
			int i = 0;
			for (byte[] bytes : otherEntries) {
				jarOutputStream.putNextEntry(new JarEntry(otherNames.get(i)));
				jarOutputStream.write(bytes);
				i++;
			}
			jarOutputStream.close();
			fileOutputStream.close();

		} catch (Exception e) {

			e.printStackTrace();
		}

	}
}
