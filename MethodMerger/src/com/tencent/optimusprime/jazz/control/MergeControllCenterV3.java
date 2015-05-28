package com.tencent.optimusprime.jazz.control;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.tencent.jazz.util.CommonUtils;
import com.tencent.jazz.util.Log;
import com.tencent.optimusprime.ClassTree;
import com.tencent.optimusprime.MClassNode;
import com.tencent.optimusprime.MethodInfo;
import com.tencent.optimusprime.jazz.Env;
import com.tencent.optimusprime.jazz.MergeAdapter;
import com.tencent.optimusprime.jazz.MethodChecker;

/**
 * 第3版：先进行类内所有受限的方法合并，再进行类外所有孤立的方法合并
 * @author noverguo
 */
public class MergeControllCenterV3 extends AbstractMergeControllCenter {

	@Override
	public void merge() throws Exception {
		// 先合并受限的方法，再合并孤立的方法
		long start = System.currentTimeMillis();
		mergerLimitedMethodInSameClass(Env.classTree.getClasses());
		Log.o("merge limited in same: " + (System.currentTimeMillis() - start) / 1000.0 + "s");
		
		start = System.currentTimeMillis();
		Env.rebuild();
		Log.o("rebuild: " + (System.currentTimeMillis() - start) / 1000.0 + "s");
		
		start = System.currentTimeMillis();
		mergeAloneMethod();
		Log.o("merge alone: " + (System.currentTimeMillis() - start) / 1000.0 + "s");
		
		start = System.currentTimeMillis();
		Env.rebuild();
		Log.o("rebuild: " + (System.currentTimeMillis() - start) / 1000.0 + "s");
		
		start = System.currentTimeMillis();
		mergerLimitedMethodInSameClass(Env.classTree.getClasses());
		Log.o("merge limited in same: " + (System.currentTimeMillis() - start) / 1000.0 + "s");
	}
	
	/**
	 * 在同一类里合并受限制的方法	受限制的方法：该方法调用了父类的非公有的方法
	 * @param classNodeList
	 * @throws Exception
	 */
	public static void mergerLimitedMethodInSameClass(List<MClassNode> classNodeList) throws Exception {
		if(!CommonUtils.isEmpty(classNodeList)) {
			List<MethodInfo> beMergedList = new ArrayList<MethodInfo>();
			// 先得到要所有需要进行类里合并的的方法 
			for(MClassNode classNode : classNodeList) {
				beMergedList.addAll(getBeMergedInSameClassList(classNode));
			}
			for(MClassNode classNode : classNodeList) {
				beMergedList.addAll(changeLimitedInstanceMethodInSameClass(classNode));
			}
			// 把这些方法进行统一的缓存
			MergeAdapter ma = new MergeAdapter(beMergedList);
			for(MClassNode classNode : classNodeList) {
				mergerLimitedMethodInSameClass(classNode, ma);
			}
			ma.destroy();
		}
	}
	
	/**
	 * 在同一类里合并受限制的方法	受限制的方法：该方法调用了父类的非公有的方法
	 * @param classNode
	 * @throws Exception
	 */
	public static void mergerLimitedMethodInSameClass(MClassNode classNode, MergeAdapter ma) throws Exception {
		List<MethodInfo> methodInfos = classNode.methodInfos;
		if(CommonUtils.isEmpty(methodInfos)) {
			return;
		}
		List<MethodInfo> beMergedList = getBeMergedInSameClassList(classNode);
		if(!CommonUtils.isEmpty(beMergedList)) {
			List<MethodInfo> tmpList = new ArrayList<MethodInfo>(beMergedList);
			// 合并静态方法
			mergerStaticWithMergeAdapter(beMergedList, ma);
			changeAccess(tmpList, ACC_PUBLIC);
		}
	}
	/**
	 * 更改受限的实例方法为静态方法
	 * @param classNode
	 * @param ma
	 * @throws Exception
	 */
	public static List<MethodInfo> changeLimitedInstanceMethodInSameClass(MClassNode classNode) throws Exception {
		List<MethodInfo> methodInfos = classNode.methodInfos;
		if(CommonUtils.getSize(methodInfos) == 0) {
			return Collections.emptyList();
		}
		boolean merInside = MethodChecker.hasCinit(classNode);
		List<MethodInfo> instanceList = new ArrayList<MethodInfo>();
		// 取出类中所有受限制的所有实例方法 
		for(MethodInfo mi : methodInfos) {
			if(mi.canMer && mi.unOverride && !MethodInfo.isStatic(mi.methodNode)) {
				// 是四大组件时，其方法均进行类内合并
				// 不是四大组件时，则只对仅能进行类内合并的方法进行类内合并
				if(merInside || MethodChecker.isComponent(classNode) || !mi.canMerOutside) {
					instanceList.add(mi);
				}
			}
		}
		if(instanceList.size() > 0) {
			// 把受限制的实例方法转成私有静态方法
			changeInstance2PrivateStatic(instanceList);
		}
		return instanceList;
	}
	
	private static List<MethodInfo> getBeMergedInSameClassList(MClassNode classNode) {
		List<MethodInfo> methodInfos = classNode.methodInfos;
		if(CommonUtils.getSize(methodInfos) == 0) {
			return Collections.emptyList();
		}
		boolean merInside = MethodChecker.hasCinit(classNode);
		List<MethodInfo> staticList = new ArrayList<MethodInfo>();
		// 取出类中所有受限制的所有实例方法 
		for(MethodInfo mi : methodInfos) {
			if(mi.canMer && mi.unOverride && MethodInfo.isStatic(mi.methodNode)) {
				// 是四大组件时，其方法均进行类内合并
				// 不是四大组件时，则只对仅能进行类内合并的方法进行类内合并
				if(merInside || MethodChecker.isComponent(classNode) || !mi.canMerOutside) {
					staticList.add(mi);
				}
			}
		}
		return staticList;
	}

	/**
	 * 合并孤立的方法  	孤立方法：不依赖于父类且不被子类所依赖的方法
	 * @param classTree
	 * @throws Exception
	 */
	public static void mergeAloneMethod() throws Exception {
		List<MethodInfo> methodInfos = Env.classTree.getMethodInfos(ClassTree.FLAG_ALL);
		if(CommonUtils.isEmpty(methodInfos)) {
			return;
		}
		// 过滤掉受限制的方法
		List<MethodInfo> instanceList = new ArrayList<MethodInfo>();
		List<MethodInfo> beMergedStaticList = new ArrayList<MethodInfo>();
		for(MethodInfo mi : methodInfos) {
			// 不同类的方法合并时，不能有四大组件里
			if(mi.canMer && mi.canMerOutside && mi.unOverride && !MethodChecker.isComponent(mi.classNode)) {
				if(MethodChecker.hasCinit(mi.classNode)) {
					continue;
				}
				if(MethodInfo.isStatic(mi.methodNode)) {
					beMergedStaticList.add(mi);
				} else {
					instanceList.add(mi);
				}
			}
		}
		if(!CommonUtils.isEmpty(instanceList)) {
			// 把实例方法转换为私有静态方法
			changeInstance2PrivateStatic(instanceList);
			beMergedStaticList.addAll(instanceList);
		}
		if(!CommonUtils.isEmpty(beMergedStaticList)) {
			// 作个缓存
			List<MethodInfo> allBeMergedMethods = new ArrayList<MethodInfo>(beMergedStaticList);
			// 多个静态方法进行合并
			mergerStatic(beMergedStaticList);
			// 合并后，可能有些静态方法还没合并，为保证之前由私有实例方法转换为私有静态的方法能正常被访问，这里把全部静态方法转换为公有的
			changeAccess(allBeMergedMethods, ACC_PUBLIC);
		}
	}
}
