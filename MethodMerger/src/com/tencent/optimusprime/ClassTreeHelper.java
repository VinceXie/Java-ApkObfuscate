package com.tencent.optimusprime;

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

import com.tencent.jazz.util.AsmVerify;
import com.tencent.jazz.util.CommonUtils;
import com.tencent.optimusprime.jazz.Env;

/**
 * @author VinceXie vince.xie@qq.com
 * @version 2014-5-29 上午10:34:18 State
 */
public class ClassTreeHelper {

	ClassTree classTree = new ClassTree();

	String jarPath = null;
	
	ArrayList<byte[]> otherEntries=new ArrayList<byte[]>();
	ArrayList<String> otherNames=new ArrayList<String>();

	/**
	 * 加载jar文件 
	 * @param jarPath 		jar文件的路径，默认为存进来的参数
	 * @param mappingPath	proguard混淆后的mapping
	 * @return
	 */
	public void loadJar(String jarPath, String mappingPath,List<String> conStrings) {

		try {
			if(Env.savePath != null) {
				File srcFile = new File(jarPath);
				CommonUtils.copy(srcFile, new File(Env.savePath, srcFile.getName().substring(0, srcFile.getName().lastIndexOf(".")) + "_org.jar"));
			}
			
			this.jarPath = jarPath;
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
					// XXX  ClassReader.SKIP_DEBUG| 决定了是否保留行号！
					cr.accept(classNode,ClassReader.EXPAND_FRAMES);
					in.close();

					classTree.addNode(classNode);
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
		if(mappingPath!=null)
		{
			classTree.setmReader(new MappingReader(mappingPath));
		}
		if(conStrings!=null)
		{
			classTree.setConStrings(conStrings);
		}
	}

	/**
	 * 使用前请先loadjar加载进去
	 * @return 获得一个ClassTree
	 */
	public ClassTree buildTree() {
		classTree.build();
		return classTree;
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
			for (ClassNode classNode : classTree.getClasses()) {
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
			int i=0;
			for(byte[] bytes:otherEntries)
			{				
				jarOutputStream.putNextEntry(new JarEntry(otherNames.get(i)));
				jarOutputStream.write(bytes);
				i++;
			}
			jarOutputStream.close();
			fileOutputStream.close();

		} catch (FileNotFoundException e) {

			e.printStackTrace();
		} catch (IOException e) {

			e.printStackTrace();
		}

	}
}
