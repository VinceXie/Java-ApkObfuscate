package com.tencent.optimusprime.jazz;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.objectweb.asm.Type;

import com.tencent.jazz.model.ClassMethodInfo;
import com.tencent.jazz.util.ASMTools;
import com.tencent.jazz.util.CommonUtils;
import com.tencent.optimusprime.Caller;
import com.tencent.optimusprime.MethodInfo;
import com.tencent.retchet.util.ASMUtils;

/**
 * MethodInfo与ClassMethodInfo转换的适配器
 * @author noverguo
 */
public class MergeAdapter {

	public static void convert(List<MethodInfo> methodInfos, List<ClassMethodInfo> beDealList, Map<String, ClassMethodInfo> callerMap) {
		Iterator<MethodInfo> iter = methodInfos.iterator();
		for(;iter.hasNext();) {
			MethodInfo mi = iter.next();
			if(!MethodChecker.check(mi)) {
				iter.remove();
				continue;
			}
			ClassMethodInfo cmi = new ClassMethodInfo(mi.classNode, mi.methodNode);
			beDealList.add(cmi);
			List<Caller> callers = mi.callers;
			if(callers != null && callerMap != null) {
				for(Caller caller: callers) {
					cmi = new ClassMethodInfo(caller.classNode, caller.methodNode);
					callerMap.put(cmi.toString(), cmi);
				}
			}
		}
	}

	private TypeConvertCacher mTCC;
	public MergeAdapter(List<MethodInfo> methodInfos) {
		grep(methodInfos);
		// 使用缓存
		mTCC = TypeConvertCacher.getInstance(methodInfos);
	}

	private void grep(List<MethodInfo> methodInfos) {
		Iterator<MethodInfo> iter = methodInfos.iterator();
		while(iter.hasNext()) {
			MethodInfo mi = iter.next();
			// 过滤掉带注解的方法
			if(MethodChecker.hasAnnotation(mi.methodNode) || MethodChecker.isInit(mi) || MethodChecker.isAbstract(mi)) {
				iter.remove();
			}
		}
	}
	/**
	 * 已经合并的方法的缓存，这缓存会一直保留，不释放
	 */
	public static Set<String> sMergedSet = new HashSet<String>();
	public List<ClassMethodInfo> getOneListByRetTypeWithCache(List<MethodInfo> methodInfos) throws Exception {
		grep(methodInfos);
		List<ClassMethodInfo> list = new ArrayList<ClassMethodInfo>();
		List<MethodInfo> tmpList = new ArrayList<MethodInfo>();
		Type useRetType = null;
		// 对根据静态方法的参数进行分类
		for(;;) {
			if(CommonUtils.isEmpty(methodInfos)) {
				break;
			}
			// 取第一个
			MethodInfo mi = methodInfos.remove(0);
			ClassMethodInfo cmi = mTCC.get(mi);
			if(!MethodChecker.checkClassVersion(cmi)) {
				continue;
			}
			if(sMergedSet.contains(cmi.toString())) {
				continue;
			}
			Type retType = ASMUtils.castType(Type.getReturnType(cmi.mMethodNode.desc));
			// 取第一个为默认的返回值
			if(useRetType == null) {
				useRetType = retType;
			}
			if(!useRetType.equals(retType)) {
				// 先不使用
				tmpList.add(mi);
				continue;
			}
			// 凑够了就结束
			int oldLen = ASMTools.computeLength(list);
			int newLen = ASMTools.getLength(cmi);
			int allLen = oldLen + newLen;
			// 大于5000的先不合并
			if(newLen > 5000) {
				continue;
			}
			// 总方法代码在5000~7000间，多于7000则先不使用
			if(allLen > 7000) {
				tmpList.add(mi);
				continue;
			}
			list.add(cmi);
			// 限制每次最多能合并的方法数
			if(list.size() > 10) {
				break;
			}
			// 超过5000，则不用再找了
			if(allLen > 5000) {
				break;
			}
		}
		// 把未使用的放回去
		methodInfos.addAll(tmpList);
		return list;
	}
	public ClassMethodInfo get(MethodInfo mi) {
		return mTCC.get(mi);
	}
	public void fix(List<String> oldMethods, ClassMethodInfo newMethod) {
		TypeConvertCacher tcc = TypeConvertCacher.getInstance();
		for(String oldMethod : oldMethods) {
			tcc.fix(oldMethod, newMethod);
		}
		sMergedSet.add(newMethod.toString());
	}
	public void destroy() {
		TypeConvertCacher.getInstance().destroy();
	}
}
