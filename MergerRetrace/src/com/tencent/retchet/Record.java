package com.tencent.retchet;

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

	public static final int INSTANCE_TO_STATIC = 1;
	public static final int STATIC_TO_MERGED_V1 = 2;
	public static final int STATIC_TO_MERGED_V2 = 3;
	public int type;

	public int pos = -1;
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
		type = Integer.parseInt(arr[3]);
		if(type == STATIC_TO_MERGED_V1) {
			argInfo = NewOldMethodArgMapInfo.fromJson(arr[4]);
		} else if(type == STATIC_TO_MERGED_V2) {
			this.pos = Integer.parseInt(arr[4]);
		}
	}

	public Record(String newClassName, String newMethodName, String newMethodDesc, String oldClassName, String oldMethodName, String oldMethodDesc, int offset, int start, int end, int type, int index) {
		this.newClassName = newClassName;
		this.newMethodName = newMethodName;
		this.newMethodDesc = newMethodDesc;
		this.oldClassName = oldClassName;
		this.oldMethodName = oldMethodName;
		this.oldMethodDesc = oldMethodDesc;
		this.offset = offset;
		this.start = start;
		this.end = end;
		this.type = type;
		this.pos = index;
	}
	public Record(String newClassName, String newMethodName, String newMethodDesc, String oldClassName, String oldMethodName, String oldMethodDesc, int offset, int start, int end, int type, NewOldMethodArgMapInfo argInfo) {
		this.newClassName = newClassName;
		this.newMethodName = newMethodName;
		this.newMethodDesc = newMethodDesc;
		this.oldClassName = oldClassName;
		this.oldMethodName = oldMethodName;
		this.oldMethodDesc = oldMethodDesc;
		this.offset = offset;
		this.start = start;
		this.end = end;
		this.type = type;
		this.argInfo = argInfo;
	}

	public String toString() {
		String res = getMerName() + 
						" " + offset + " " + 
						getOrgName() + 
						" " + start + ":" + end + 
						"|" + newMethodDesc + "|" + oldMethodDesc + "|" + type +
						(type == INSTANCE_TO_STATIC ? "" : "|" + (type == STATIC_TO_MERGED_V1 ? argInfo.toJson() : "" + pos));
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
	
	

}
