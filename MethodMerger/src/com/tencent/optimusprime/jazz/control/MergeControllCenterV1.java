package com.tencent.optimusprime.jazz.control;

import java.util.ArrayList;
import java.util.List;

import com.tencent.jazz.util.CommonUtils;
import com.tencent.optimusprime.MClassNode;
import com.tencent.optimusprime.MethodInfo;
import com.tencent.optimusprime.jazz.Env;

/**
 * 第1版：只合并类里私有函数
 * @author noverguo
 */
public class MergeControllCenterV1 extends AbstractMergeControllCenter {

	@Override
	public void merge() throws Exception {
		mergerPrivateMethodInSameClass(Env.classTree.getClasses());
	}
	
	/**
	 * 在同一类里的私有方法	
	 * @param classNode
	 * @throws Exception
	 */
	public static void mergerPrivateMethodInSameClass(List<MClassNode> classNodeList) throws Exception {
		if(CommonUtils.getSize(classNodeList) > 0) {
			for(MClassNode classNode : classNodeList) {
				mergerPrivateMethodInSameClass(classNode);
			}
		}
	}
	/**
	 * 在同一类里的私有方法	
	 * @param classNode
	 * @throws Exception
	 */
	public static void mergerPrivateMethodInSameClass(MClassNode classNode) throws Exception {
		List<MethodInfo> methodInfos = classNode.methodInfos;
		if(CommonUtils.getSize(methodInfos) == 0) {
			return;
		}
		List<MethodInfo> privateInstanceList = new ArrayList<MethodInfo>();
		// 取出类中所有受限制的私有实例方法 
		for(MethodInfo mi : methodInfos) {
			if(mi.canMer && MethodInfo.isPrivate(mi.methodNode) && !MethodInfo.isStatic(mi.methodNode)) {
				privateInstanceList.add(mi);
			}
		}
		if(privateInstanceList.size() > 0) {
			// 把受限制的私有实例方法转成私有静态方法
			changeInstance2PrivateStatic(privateInstanceList);
		}
		privateInstanceList.clear();
		List<MethodInfo> staticInstanceList = privateInstanceList;
		// 取出类中所有私有静态方法
		for(MethodInfo mi : methodInfos) {
			if(mi.canMer && MethodInfo.isPrivate(mi.methodNode) && MethodInfo.isStatic(mi.methodNode)) {
				staticInstanceList.add(mi);
			}
		}
		if(staticInstanceList.size() > 0) {
			// 合并私有静态方法
			mergerStatic(staticInstanceList);
		}
	}
}
