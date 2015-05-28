package com.tencent.optimusprime.jazz;

import java.util.List;

import com.tencent.jazz.model.ClassFieldInfo;
import com.tencent.jazz.model.ClassMethodInfo;
import com.tencent.optimusprime.Caller;
import com.tencent.optimusprime.FieldInfo;
import com.tencent.optimusprime.Invoker;
import com.tencent.optimusprime.MethodInfo;

/**
 * MethodInfo与ClassMethodInfo类型转换
 * @author noverguo
 */
public class TypeConvertor {

	public static ClassMethodInfo get(MethodInfo methodInfo) {
		ClassMethodInfo res = new ClassMethodInfo(methodInfo.classNode, methodInfo.methodNode);
		List<Caller> callers = methodInfo.callers;
		if(callers != null) {
			for(Caller caller : callers) {
				ClassMethodInfo cmi = get(caller);
				res.callerMap.put(cmi.toString(), cmi);
			}
		}
		List<Invoker> invokers = methodInfo.invokers;
		if(invokers != null) {
			for(Invoker invoker : invokers) {
				ClassMethodInfo cmi = get(invoker);
				if(cmi != null) {
					res.invokerMethodMap.put(cmi.toString(), cmi);
				}
			}
		}
		List<FieldInfo> fieldInfos = methodInfo.fieldInfos;
		if(invokers != null) {
			for(FieldInfo fi : fieldInfos) {
				ClassFieldInfo cmi = get(fi);
				res.invokerFieldMap.put(cmi.toString(), cmi);
			}
		}
		return res;
	}
	/**
	 * 执行者需要保存其caller信息，以便在执行者的访问修饰更改后能够修复其对应的caller
	 * @param invoker
	 * @return
	 */
	public static ClassMethodInfo get(Invoker invoker) {
		MethodInfo invokerMethodInfo = Env.classTree.getMethodInfo(invoker.classNode.name, invoker.methodNode.name, invoker.methodNode.desc);
		if(invokerMethodInfo == null) {
			return null;
		}
		ClassMethodInfo res = new ClassMethodInfo(invokerMethodInfo.classNode, invokerMethodInfo.methodNode);
		List<Caller> callers = invokerMethodInfo.callers;
		if(callers != null) {
			for(Caller caller : callers) {
				ClassMethodInfo cmi = get(caller);
				res.callerMap.put(cmi.toString(), cmi);
			}
		}
		return res;
	}
	
	public static ClassMethodInfo get(Caller caller) {
		return new ClassMethodInfo(caller.classNode, caller.methodNode);
	}
	public static ClassFieldInfo get(FieldInfo fieldInfo) {
		return new ClassFieldInfo(fieldInfo.classNode, fieldInfo.fieldNode);
	}
}
