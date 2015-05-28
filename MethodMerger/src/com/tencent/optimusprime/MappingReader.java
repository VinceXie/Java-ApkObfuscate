package com.tencent.optimusprime;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;

/**
 * @author VinceXie vince.xie@qq.com
 * @version 2014-6-11 上午11:28:50 State 读取proguard生成的mapping
 */
public class MappingReader {

	/**
	 * 混淆过方法：类名+方法名+参数
	 */
	private ArrayList<String> obfuseds = new ArrayList<String>();

	private String path;

	/**
	 * 测试
	 * @param args
	 */
	public static void main(String[] args) {

		MappingReader mReader=new MappingReader("mapping.txt");
		System.out.println(mReader.getObfuseds());
	}

	public MappingReader(String path) {
		this.path = path;

		read();
	}

	public ArrayList<String> getObfuseds() {
		return obfuseds;
	}

	private void read() {
		BufferedReader br;
		try {
			
			br = new BufferedReader(new InputStreamReader(new FileInputStream(
					path)));
			String className=null;
			for (String line = br.readLine(); line != null; line = br
					.readLine()) {
								
				if(line.charAt(0)!=' ')
				{			

					String[] tmp=line.split(" ");
					//获得混淆后的类名
					tmp[2]=tmp[2].substring(0, tmp[2].length()-1);
					className=tmp[2];
					
					// 判断该类是否可变 tcs.cey与tcs/cey的区别
//					if(!tmp[0].equals(tmp[2])&&!Tools.isInterface(ClassTree.hashStrToNode.get(tmp[2].replaceAll(".", "/"))))
//					{
//						ClassTree.hashStrToNode.get(tmp[2]).canMess=true;
//					}

					continue;
				}
				if(line.contains("("))
				{
					String[] tmp=line.split(" ");
					String param="("+line.split("\\(")[1].substring(0,line.split("\\(")[1].indexOf(")"))+")";
					if(!line.split("\\(")[0].split(" ")[line.split("\\(")[0].split(" ").length-1].equals(tmp[tmp.length-1]))
					{
						obfuseds.add(className+tmp[tmp.length-1]+param);
					}				
					continue;
				}
			}
			br.close();
			
		} catch (IOException e) {

			e.printStackTrace();
		}
	}

}
