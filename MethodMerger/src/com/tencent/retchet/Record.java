package com.tencent.retchet;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;

import com.tencent.retchet.util.ASMUtils;


/**
 * @author VinceXie vince.xie@qq.com
 * @version 2014-7-22 上午11:33:29 State com.example.abc.MainActivity.add 17
 *          com.example.abc.MainActivity.a 10:50
 */
public class Record {

	public String newClassName;
	public String newMethodName;
	public String newMethodDesc;

	public String oldClassName;
	public String oldMethodName;
	public String oldMethodDesc;

	public int offset;

	public int start;

	public int end;

	public NewOldMethodArgMapInfo argInfo;

	public Record(String recordString) {
		String[] arr = recordString.split("\\|");
		String[] tmp = arr[0].split(" ");
		String orgName = tmp[0];
		String merName = tmp[2];
		int index = orgName.lastIndexOf('.');
		this.newClassName = orgName.substring(0, index).replace(".", "/");
		this.newMethodName = orgName.substring(index + 1);
		
		index = merName.lastIndexOf('.');
		this.oldClassName = merName.substring(0, index).replace(".", "/");
		this.oldMethodName = merName.substring(index + 1);
		
		offset = Integer.valueOf(tmp[1]);
		start = Integer.valueOf(tmp[3].split(":")[0]);
		end = Integer.valueOf(tmp[3].split(":")[1]);
		
		newMethodDesc = arr[1];
		oldMethodDesc = arr[2];
		if(arr.length > 3) {
			argInfo = NewOldMethodArgMapInfo.fromJson(arr[3]);
		}
	}

	public Record(String newClassName, String newMethodName, String newMethodDesc, String oldClassName, String oldMethodName, String oldMethodDesc, int offset, int start, int end, NewOldMethodArgMapInfo argInfo) {
		super();
		this.newClassName = newClassName;
		this.newMethodName = newMethodName;
		this.newMethodDesc = newMethodDesc;
		this.oldClassName = oldClassName;
		this.oldMethodName = oldMethodName;
		this.oldMethodDesc = oldMethodDesc;
		this.offset = offset;
		this.start = start;
		this.end = end;
		this.argInfo = argInfo;
	}

	public String toString() {
		String res = getMerName() + 
						" " + offset + " " + 
						getOrgName() + 
						" " + start + ":" + end + 
						"|" + newMethodDesc + "|" + oldMethodDesc + (argInfo == null ? "" : "|" + argInfo.toJson());
		return res;
	}

	public String getNewKey() {
		return ASMUtils.toString(newClassName, newMethodName, newMethodDesc);
	}
	
	public String getOldKey() {
		return ASMUtils.toString(oldClassName, oldMethodName, oldMethodDesc);
	}
	
	public String getMerName() {
		return newClassName.replaceAll("/", ".") + "." + newMethodName;
	}

	public String getOrgName() {
		return oldClassName.replaceAll("/", ".") + "." + oldMethodName;
	}

	public int getStart() {
		return start;
	}

	public int getEnd() {
		return end;
	}

	public int getOffset() {
		return offset;
	}
	
	public static ArrayList<Record> fromFile(File file) throws Exception
	{
		ArrayList<Record> records=new ArrayList<Record>();
		BufferedReader br = new BufferedReader(new InputStreamReader(
				new FileInputStream(file)));
		for (String line = br.readLine(); line != null; line = br.readLine()) {
			records.add(new Record(line));
		}
		br.close();
		return records;
	}

}
