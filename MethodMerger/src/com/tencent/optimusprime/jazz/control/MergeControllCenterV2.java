package com.tencent.optimusprime.jazz.control;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.tencent.jazz.util.CommonUtils;
import com.tencent.optimusprime.ClassTree;
import com.tencent.optimusprime.MClassNode;
import com.tencent.optimusprime.MethodInfo;
import com.tencent.optimusprime.jazz.Env;
import com.tencent.optimusprime.jazz.MergeAdapter;
import com.tencent.optimusprime.jazz.MethodChecker;

/**
 * 第2版：先进行类内所有受限的私有方法合并，再进行类外所有孤立的私有方法合并
 * @author noverguo
 */
public class MergeControllCenterV2 extends AbstractMergeControllCenter {

	@Override
	public void merge() throws Exception {
		mergerLimitedPrivateMethodInSameClass(Env.classTree.getClasses());
		Env.rebuild();
		mergeAlonePrivateMethod();
		Env.rebuild();
		mergerLimitedPrivateMethodInSameClass(Env.classTree.getClasses());
	}
	
	/**
	 * 在同一类里合并受限制的私有方法	受限制的方法：该方法调用了父类的非公有的方法
	 * @param classNodeList
	 * @throws Exception
	 */
	public static void mergerLimitedPrivateMethodInSameClass(List<MClassNode> classNodeList) throws Exception {
		if(!CommonUtils.isEmpty(classNodeList)) {
			for(MClassNode classNode : classNodeList) {
				changeLimitedPrivateInstanceMethodInSameClass(classNode);
			}
			List<MethodInfo> beMergedList = new ArrayList<MethodInfo>();
			// 先得到要所有需要进行类里合并的的方法 
			for(MClassNode classNode : classNodeList) {
				beMergedList.addAll(getLimitedPrivateStaticMethodInSameClass(classNode));
			}
			// 把这些方法进行统一的缓存
			MergeAdapter ma = new MergeAdapter(beMergedList);
			for(MClassNode classNode : classNodeList) {
				mergerLimitedPrivateMethodInSameClass(classNode, ma);
			}
			ma.destroy();
		}
	}
	/**
	 * 在同一类里合并受限制的私有方法	受限制的方法：该方法调用了父类的非公有的方法
	 * @param classNode
	 * @throws Exception
	 */
	public static void changeLimitedPrivateInstanceMethodInSameClass(MClassNode classNode) throws Exception {
		List<MethodInfo> methodInfos = classNode.methodInfos;
		if(CommonUtils.isEmpty(methodInfos)) {
			return;
		}
		boolean merInside = MethodChecker.hasCinit(classNode);
		List<MethodInfo> privateInstanceList = new ArrayList<MethodInfo>();
		// 取出类中所有受限制的私有实例方法 
		for(MethodInfo mi : methodInfos) {
			if(mi.canMer && MethodInfo.isPrivate(mi.methodNode) && !MethodInfo.isStatic(mi.methodNode)) {
				if(merInside || MethodChecker.isComponent(classNode) || !mi.canMerOutside) {
					privateInstanceList.add(mi);
				}
			}
		}
		if(privateInstanceList.size() > 0) {
			// 把受限制的私有实例方法转成私有静态方法
			changeInstance2PrivateStatic(privateInstanceList);
		}
	}
	/**
	 * 得到同一类里合并受限制的私有静态方法
	 * @param classNode
	 * @throws Exception
	 */
	public static List<MethodInfo> getLimitedPrivateStaticMethodInSameClass(MClassNode classNode) throws Exception {
		List<MethodInfo> methodInfos = classNode.methodInfos;
		if(CommonUtils.isEmpty(methodInfos)) {
			return Collections.emptyList();
		}
		boolean merInside = MethodChecker.hasCinit(classNode);
		List<MethodInfo> privateStaticList = new ArrayList<MethodInfo>();
		// 取出类中所有受限制的私有实例方法 
		for(MethodInfo mi : methodInfos) {
			if(mi.canMer && MethodInfo.isPrivate(mi.methodNode) && MethodInfo.isStatic(mi.methodNode)) {
				if(merInside || MethodChecker.isComponent(classNode) || !mi.canMerOutside) {
					privateStaticList.add(mi);
				}
			}
		}
		return privateStaticList;
	}

	/**
	 * 在同一类里合并受限制的私有方法	受限制的方法：该方法调用了父类的非公有的方法
	 * @param classNode
	 * @param ma 
	 * @throws Exception
	 */
	public static void mergerLimitedPrivateMethodInSameClass(MClassNode classNode, MergeAdapter ma) throws Exception {
		List<MethodInfo> methodInfos = classNode.methodInfos;
		if(CommonUtils.isEmpty(methodInfos)) {
			return;
		}
		List<MethodInfo> beMergedList = getLimitedPrivateStaticMethodInSameClass(classNode);
		if(!CommonUtils.isEmpty(beMergedList)) {
			// 合并私有静态方法
			mergerStaticWithMergeAdapter(beMergedList, ma);
			beMergedList = getLimitedPrivateStaticMethodInSameClass(classNode);
			changeAccess(beMergedList, ACC_PUBLIC);
		}
	}
	
	/**
	 * 合并孤立的私有方法  	孤立方法：不依赖于父类且不被子类所依赖的方法
	 * @param classTree
	 * @throws Exception
	 */
	public static void mergeAlonePrivateMethod() throws Exception {
		List<MethodInfo> methodInfos = Env.classTree.getMethodInfos(ClassTree.FLAG_ALL);
		if(CommonUtils.isEmpty(methodInfos)) {
			return;
		}
		// 过滤掉受限制的方法
		List<MethodInfo> beMergeMethodList = new ArrayList<MethodInfo>();
		List<MethodInfo> instanceList = new ArrayList<MethodInfo>();
		for(MethodInfo mi : methodInfos) {
			if(mi.canMer && mi.canMerOutside && MethodInfo.isPrivate(mi.methodNode) && !MethodChecker.isComponent(mi.classNode)) {
				if(MethodChecker.hasCinit(mi.classNode)) {
					continue;
				}
				if(!MethodInfo.isStatic(mi.methodNode)) {
					instanceList.add(mi);
				} else {
					beMergeMethodList.add(mi);
				}
			}
		}
		if(!instanceList.isEmpty()) {
			// 把私有方法转换为私有静态方法
			changeInstance2PrivateStatic(instanceList);
			beMergeMethodList.addAll(instanceList);
		}
		if(!beMergeMethodList.isEmpty()) {
			// 作个缓存
			List<MethodInfo> allBeMergedMethods = new ArrayList<MethodInfo>(beMergeMethodList);
			// 多个静态方法进行合并
			mergerStatic(beMergeMethodList);
			// 合并后，可能有些静态方法还没合并，为保证之前由私有实例方法转换为私有静态的方法能正常被访问，这里把全部静态方法转换为公有的
			changeAccess(allBeMergedMethods, ACC_PUBLIC);
		}
	}

}
