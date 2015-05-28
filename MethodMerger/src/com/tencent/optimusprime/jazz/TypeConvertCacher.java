package com.tencent.optimusprime.jazz;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodNode;

import com.tencent.jazz.model.ClassFieldInfo;
import com.tencent.jazz.model.ClassMethodInfo;
import com.tencent.jazz.util.CommonUtils;
import com.tencent.optimusprime.Caller;
import com.tencent.optimusprime.FieldInfo;
import com.tencent.optimusprime.Invoker;
import com.tencent.optimusprime.MethodInfo;

/**
 * MethodInfo与ClassMethodInfo类型转换，带缓存
 * @author noverguo
 */
public class TypeConvertCacher {
	private Map<String, ClassMethodInfo> cache;
	private static TypeConvertCacher inst;

	public static TypeConvertCacher getInstance() {
		return getInstance(null);
	}
	public static TypeConvertCacher getInstance(List<MethodInfo> list) {
		if(inst == null) {
			synchronized (TypeConvertCacher.class) {
				if(inst == null) {
					inst = new TypeConvertCacher(list);
				}
			}
		}
		return inst;
	}
	
	public TypeConvertCacher(List<MethodInfo> list) {
		cache = new HashMap<String, ClassMethodInfo>();
		init(list);
	}
	private void init(List<MethodInfo> list) {
		for(MethodInfo mi : list) {
			getForInit(mi);
		}
	}
	private ClassMethodInfo getForInit(MethodInfo methodInfo) {
		ClassMethodInfo cmi = get(methodInfo);
		
		List<FieldInfo> fieldInfos = methodInfo.fieldInfos;
		if(fieldInfos != null) {
			for(FieldInfo fi : fieldInfos) {
				ClassFieldInfo cfi = get(fi);
				cmi.invokerFieldMap.put(cfi.toString(), cfi);
			}
		}
		List<Caller> callers = methodInfo.callers;
		if(callers != null) {
			for(Caller caller : callers) {
				ClassMethodInfo callerCMI = get(caller);
				cmi.callerMap.put(callerCMI.toString(), callerCMI);
			}
		}
		List<Invoker> invokers = methodInfo.invokers;
		if(invokers != null) {
			for(Invoker invoker : invokers) {
				ClassMethodInfo invokerCMI = getForInit(invoker);
				cmi.invokerMethodMap.put(invokerCMI.toString(), invokerCMI);
			}
		}
		
		return cmi;
	}
	private ClassMethodInfo getForInit(Invoker invoker) {
		ClassMethodInfo cmi = get(invoker);
		MethodInfo invokerMethodInfo = Env.classTree.getMethodInfo(invoker.classNode.name, invoker.methodNode.name, invoker.methodNode.desc);
		if(invokerMethodInfo != null) {
			List<Caller> callers = invokerMethodInfo.callers;
			if(callers != null && CommonUtils.isEmpty(cmi.callerMap)) {
				for(Caller caller : callers) {
					ClassMethodInfo callerCMI = get(caller);
					cmi.callerMap.put(callerCMI.toString(), callerCMI);
				}
			}
		}  else {
			
		}
		return cmi;
	}
	
	/**
	 * 如果缓存中有，则直接从缓存中取，没则创建一个
	 * @param mi
	 * @return
	 */
	private ClassMethodInfo get(ClassNode cn, MethodNode mn) {
		String key = ClassMethodInfo.toString(cn, mn);
		if(cache.containsKey(key)) {
			return cache.get(key);
		}
		ClassMethodInfo cmi = new ClassMethodInfo(cn, mn);
		cache.put(cmi.toString(), cmi);
		return cmi;
	}
	public ClassMethodInfo get(MethodInfo mi) {
		return get(mi.classNode, mi.methodNode);
	}
	public ClassMethodInfo get(Invoker invoker) {
		return get(invoker.classNode, invoker.methodNode);
	}
	public ClassMethodInfo get(Caller caller) {
		return get(caller.classNode, caller.methodNode);
	}
	public ClassMethodInfo get(String key) {
		return cache.get(key);
	}
	public ClassMethodInfo remove(ClassMethodInfo cmi) {
		return remove(cmi.toString());
	}
	public ClassMethodInfo remove(String key) {
		return cache.remove(key);
	}
	public void add(ClassMethodInfo cmi) {
		add(cmi.toString(), cmi);
	}
	public void add(String key, ClassMethodInfo cmi) {
		if(!cache.containsKey(key)) {
			cache.put(key, cmi);
		}
	}
	public ClassFieldInfo get(FieldInfo fieldInfo) {
		return TypeConvertor.get(fieldInfo);
	}
	public void destroy() {
		cache.clear();
		cache = null;
		inst = null;
	}
	/**
	 * 修复新旧函数关联
	 * @param oldMethod
	 * @param newMethod
	 */
	public void fix(String oldKey, ClassMethodInfo newMethod) {
		String newKey = newMethod.toString();
		ClassMethodInfo oldMethod = remove(oldKey);
		newMethod.callerMap.putAll(oldMethod.callerMap);
		newMethod.invokerMethodMap.putAll(oldMethod.invokerMethodMap);
		newMethod.invokerFieldMap.putAll(oldMethod.invokerFieldMap);
		add(newKey, newMethod);
		// 修复缓存的ClassMethodInfo中所有的map
		Iterator<String> iter = cache.keySet().iterator();
		while(iter.hasNext()) {
			String key = iter.next();
			ClassMethodInfo cmi = cache.get(key);
			replaceOld(cmi.callerMap, oldKey, newKey, newMethod);
			replaceOld(cmi.invokerMethodMap, oldKey, newKey, newMethod);
			cache.put(key, cmi);
		}
	}
	private void replaceOld(Map<String, ClassMethodInfo> map, String oldKey, String newKey, ClassMethodInfo newMethod) {
		if(CommonUtils.isEmpty(map)) {
			return;
		}
		if(map.containsKey(oldKey)) {
			map.remove(oldKey);
			if(!map.containsKey(newKey)) {
				map.put(newKey, newMethod);
			}
		}
	}
}
