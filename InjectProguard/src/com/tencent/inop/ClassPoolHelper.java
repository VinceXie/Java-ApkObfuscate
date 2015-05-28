package com.tencent.inop;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.JarOutputStream;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.tree.ClassNode;



/**
 * @author VinceXie vince.xie@qq.com
 * @version 2014-8-18 上午11:01:13
 * State
 */
public class ClassPoolHelper {

	ClassPool classPool = new ClassPool();

	String jarPath = null;
	
	ArrayList<byte[]> otherEntries=new ArrayList<byte[]>();
	ArrayList<String> otherNames=new ArrayList<String>();

	/**
	 * 加载jar文件 
	 * @param jarPath 		jar文件的路径，默认为存进来的参数
	 * @param mappingPath	proguard混淆后的mapping
	 * @return
	 */
	public void loadJar(String jarPath,boolean isOpguard) {

		try {
			this.jarPath = jarPath;
			MClassNode classNode;
			JarFile jfile = new JarFile(jarPath);
			Enumeration<?> files = jfile.entries();
			// 读取jar里所有class文件
			while (files.hasMoreElements()) {
				JarEntry entry = (JarEntry) files.nextElement();
				if(entry.getName().contains("OpguardIsInjected"))
				{
					System.out.println("请不要重复注入");
					System.exit(1);
				}
				if((entry.getName().contains("META-INF/")||entry.getName().contains("proguard/"))
						&&isOpguard)
				{
					continue;
				}
				if (entry.getName().endsWith(".class")) {
					InputStream in;

					in = jfile.getInputStream(entry);

					ClassReader cr = new ClassReader(in);
					classNode = new MClassNode();
					// XXX  ClassReader.SKIP_DEBUG| 决定了是否保留行号！
					cr.accept(classNode,ClassReader.EXPAND_FRAMES);
					in.close();

					classPool.addNode(classNode);
				}
				else {
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
					otherEntries.add(result);
					otherNames.add(entry.getName());
				}
			}
		} catch (IOException e) {
			e.printStackTrace();
			System.out.println("load jar error.");
		}

	}

	/**
	 * 使用前请先loadjar加载进去
	 * @return 获得一个ClassTree
	 */
	public ClassPool inject() {
		classPool.build();
		return classPool;
	}

	/**
	 * 保存的jar路径，若为空，保存为原来加载的路径。
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
			for (ClassNode classNode : classPool.classes) {
				String classPath;
				classPath = classNode.name.replace(".", "/");
				classPath=classPath+".class";
				JarEntry jarEntry = new JarEntry(classPath);
				jarOutputStream.putNextEntry(jarEntry);
				ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_MAXS);
				classNode.accept(cw);
				byte[] code = cw.toByteArray();
				AsmVerify.check(code);
				jarOutputStream.write(code);
			}
			jarOutputStream.putNextEntry(new JarEntry("OpguardIsInjected"));	
			jarOutputStream.write(new byte[]{1,1,0});
			int i=0;
			for(byte[] bytes:otherEntries)
			{				
				jarOutputStream.putNextEntry(new JarEntry(otherNames.get(i)));
				jarOutputStream.write(bytes);
				i++;
			}
			jarOutputStream.close();
			fileOutputStream.close();

		} catch (Exception e) {

			e.printStackTrace();
			System.exit(1);
		} 

	}
}
