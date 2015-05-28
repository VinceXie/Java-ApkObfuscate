package com.tencent.retchet;

import java.util.Arrays;

import org.objectweb.asm.Type;

import com.google.gson.Gson;
import com.tencent.retchet.util.ASMUtils;

/**
 * 新旧函数的参数映射表
 * @author noverguo
 */
public class NewOldMethodArgMapInfo {
	// 函数标识，用于新函数中识别调用的是哪个旧函数
	public int mWhich;
	public IndexTypeMap[] mMap;
	public int mMaxArgIndex;
	
	public NewOldMethodArgMapInfo() {
	}
	public NewOldMethodArgMapInfo(int mWhich, int size) {
		this.mWhich = mWhich;
		mMap = new NewOldMethodArgMapInfo.IndexTypeMap[size];
	}
	public void addIndexType(int pos, int oldIndex, int oldPos, int newIndex, int newPos, Type oldType) {
		setIndexType(pos, oldIndex, oldPos, newIndex, newPos, oldType, ASMUtils.castType(oldType));
	}
	public void setIndexType(int pos, int oldIndex, int oldPos, int newIndex, int newPos, Type oldType, Type newType) {
		if(mMap == null) {
			mMap = new IndexTypeMap[pos+1];
		} else if(mMap.length <= pos) {
			mMap = Arrays.copyOf(mMap, pos+1);
		}
		mMap[pos] = new IndexTypeMap(oldIndex, oldPos, newIndex, newPos, oldType, newType);
	}
	// 参数位置及参数类型映射表
	public static class IndexTypeMap {
		public int mOldIndex;
		public int mOldPos;
		public int mNewIndex;
		public int mNewPos;
		public Type mOldType;
		public Type mNewType;

		public IndexTypeMap() {
		}
		public IndexTypeMap(int oldIndex, int oldPos, int newIndex, int newPos, Type oldType, Type newType) {
			this.mOldIndex = oldIndex;
			this.mOldPos = oldPos;
			this.mNewIndex = newIndex;
			this.mNewPos = newPos;
			this.mOldType = oldType;
			this.mNewType = newType;
		}
	}
	public String toJson() {
		return new Gson().toJson(this);
	}
	public static NewOldMethodArgMapInfo fromJson(String json) {
		return new Gson().fromJson(json, NewOldMethodArgMapInfo.class);
	}
}
