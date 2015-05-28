package com.tencent.optimusprime.jazz.control;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.ClassNode;

import com.tencent.jazz.MergedMethodCallerFixer;
import com.tencent.jazz.model.ClassMethodInfo;
import com.tencent.jazz.tools.ChangeModifyTools;
import com.tencent.jazz.tools.CheckcastTools;
import com.tencent.jazz.tools.MergeMethodTools;
import com.tencent.jazz.util.CommonUtils;
import com.tencent.jazz.util.Log;
import com.tencent.optimusprime.MethodInfo;
import com.tencent.optimusprime.jazz.Env;
import com.tencent.optimusprime.jazz.MergeAdapter;
import com.tencent.optimusprime.jazz.TypeConvertor;
import com.tencent.retchet.Record;
import com.tencent.retchet.util.ASMUtils;

/**
 * 合并控制中心的抽象，作为各个版本的父类
 * @author noverguo
 */
public abstract class AbstractMergeControllCenter implements Opcodes {
	/**
	 * 合并方法，具体功能由子类实现
	 * @throws Exception
	 */
	public abstract void merge() throws Exception;
	
	protected List<Record> records;
	public void init(List<Record> records) {
		this.records = records;
	}
	
	public void run() throws Exception {
		Log.o("records is null: " + (records == null));
		if(!CommonUtils.isEmpty(records)) {
			if(hasMethodInRecord()) {
				Log.o("mergeWithRecord");
				mergeWithRecord();
				Env.rebuild();
			} else {
				Log.o("fixByRecord");
				MergedMethodCallerFixer.fixByRecord(records, new ArrayList<ClassNode>(Env.classTree.getClasses()));
			}
		}
		Log.o("start merge");
		merge();
	}
	


	private void mergeWithRecord() throws IOException {
		Map<String, List<Record>> recordMap = new HashMap<String, List<Record>>();
		Map<String, MethodInfo> miMap = new HashMap<String, MethodInfo>();
		List<MethodInfo> miList = new ArrayList<MethodInfo>();
		List<ClassMethodInfo> instanceList = new ArrayList<ClassMethodInfo>();
		// 根据合并后的方法，把旧的实例方法全部找出来，并转换为静态方法
		for(Record record : records) {
			if(record.argInfo != null) {
				continue;
			}
			MethodInfo mi = Env.classTree.getMethodInfo(record.oldClassName, record.oldMethodName, record.oldMethodDesc);
			// 不是静态的方法，需要先转换为静态的方法
			if((mi.methodNode.access & ACC_STATIC) != ACC_STATIC) {
				ClassMethodInfo cmi = TypeConvertor.get(mi);
				instanceList.add(cmi);
				ChangeModifyTools.changeInstance2PrivateStatic(instanceList, cmi.callerMap.values());
				ChangeModifyTools.changeAccess(instanceList, ACC_PUBLIC);
				instanceList.clear();
				record.oldMethodName = cmi.mMethodNode.name;
				record.oldMethodDesc = cmi.mMethodNode.desc;
			} else {
				// TODO argInfo为空时，应该只能为实例方法
				System.err.println("[error] " + ASMUtils.toString(record.oldClassName, record.oldMethodName, record.oldMethodDesc) + "is instance but argInfo is null");
			}
		}
		Env.rebuild();
		// 根据合并后的方法，把旧的静态方法全部找出来
		for(Record record : records) {
			if(record.argInfo == null) {
				continue;
			}
			String key = ASMUtils.toString(record.newClassName, record.newMethodName, record.newMethodDesc);
			MethodInfo mi = Env.classTree.getMethodInfo(record.oldClassName, record.oldMethodName, record.oldMethodDesc);
			// 不是静态的方法，需要先转换为静态的方法
			if((mi.methodNode.access & ACC_STATIC) != ACC_STATIC) {
				// TODO argInfo不为空时，应该只能为静态方法
				System.err.println("[error] " + ASMUtils.toString(record.oldClassName, record.oldMethodName, record.oldMethodDesc) + " is static but argInfo is not null");
			}
			if(!recordMap.containsKey(key)) {
				recordMap.put(key, new ArrayList<Record>());
			}
			recordMap.get(key).add(record);
			miMap.put(record.getOldKey(), mi);
			miList.add(mi);
		}
		Env.rebuild();
		MergeAdapter ma = new MergeAdapter(miList);
		List<String> oldMethodKeys = new ArrayList<String>();
		for(String key : recordMap.keySet()) {
			List<Record> recordList = recordMap.get(key);
			int size = recordList.size();
			ClassMethodInfo[] oldMethods = new ClassMethodInfo[size];
			Record[] recordArr = new Record[size];
			ClassMethodInfo newMethod = new ClassMethodInfo(Env.classTree.getNode(recordList.get(0).newClassName), null);
			// 根据which的顺序来排列方法
			for(Record record : recordList) {
				ClassMethodInfo cmi = ma.get(miMap.get(record.getOldKey()));
				int index = record.argInfo.mWhich;
				oldMethods[index] = cmi;
				recordArr[index] = record;
			}
			// 记录旧方法的信息
			oldMethodKeys.clear();
			for(ClassMethodInfo cmi : oldMethods) {
				oldMethodKeys.add(cmi.toString());
			}
			MergeMethodTools.mergeMethod(newMethod, Arrays.asList(oldMethods), Arrays.asList(recordArr));
			fixCheckcast(newMethod);
			// 合并完成后，修复待合并的方法列表
			ma.fix(oldMethodKeys, newMethod);
		}
		ma.destroy();
	}

	private boolean hasMethodInRecord() {
		if(records == null) {
			return false;
		}
		for(Record record : records) {
			if(Env.classTree.getMethodInfo(record.oldClassName, record.oldMethodName, record.oldMethodDesc) != null) {
				return true;
			}
		}
		return false;
	}

	
	/**
	 * 把方法的访问权限改为access
	 */
	protected static void changeAccess(List<MethodInfo> methodInfos, int access) {
		List<ClassMethodInfo> beChangedMethodList = new ArrayList<ClassMethodInfo>(); 
		MergeAdapter.convert(methodInfos, beChangedMethodList, null);
		ChangeModifyTools.changeAccess(beChangedMethodList, access);
	}
	/**
	 * 把实例方法转换为私有静态的方法
	 * @param methodInfos 私有方法列表
	 */
	protected static void changeInstance2PrivateStatic(List<MethodInfo> methodInfos) {
		List<ClassMethodInfo> beChangedMethodList = new ArrayList<ClassMethodInfo>(); 
		Map<String, ClassMethodInfo> callerMap = new HashMap<String, ClassMethodInfo>();
		MergeAdapter.convert(methodInfos, beChangedMethodList, callerMap);
		ChangeModifyTools.changeInstance2PrivateStatic(beChangedMethodList, callerMap.values());
	}
	
	/**
	 * 把多个静态方法合并成公有静态的方法
	 * @param methodInfos
	 * @throws Exception 
	 */
	protected static void mergerStatic(List<MethodInfo> methodInfos) throws Exception {
		if(CommonUtils.isEmpty(methodInfos)) {
			return;
		}
		MergeAdapter ma = new MergeAdapter(methodInfos);
		mergerStaticWithMergeAdapter(methodInfos, ma);
		ma.destroy();
	}
	
	/**
	 * 第三版把用到的数据作了缓存，以优化性能
	 * @param methodInfos
	 * @throws Exception
	 */
	protected static void mergerStaticWithMergeAdapter(List<MethodInfo> methodInfos, MergeAdapter ma) throws Exception {
		List<String> oldMethodKeys = new ArrayList<String>();
		for(;;) {
			List<ClassMethodInfo> beMergedList = ma.getOneListByRetTypeWithCache(methodInfos);
			if(CommonUtils.isEmpty(beMergedList)) {
				break;
			}
			if(beMergedList.size() == 1) {
				 continue;
			}
			// 保存旧方法的Key
			oldMethodKeys.clear();
			for(ClassMethodInfo cmi : beMergedList) {
				oldMethodKeys.add(cmi.toString());
				if((cmi.mMethodNode.access & ACC_ABSTRACT) == ACC_ABSTRACT) {
					Log.o("isAbstarct: " + cmi);
				}
			}
			// 合并方法，并返回新的方法
			ClassMethodInfo newCMI = MergeMethodTools.mergeMethod(beMergedList);
			
			if(newCMI == null) {
				continue;
			}
			fixCheckcast(newCMI);
			// 合并完成后，修复待合并的方法列表
			ma.fix(oldMethodKeys, newCMI);
		}
	}
	
	protected static void fixCheckcast(ClassMethodInfo newMethodNode) {
		// 获取全部有进行强制类型转制的类
		List<String> checkcastClasses = CheckcastTools.getCheckcastClasses(newMethodNode.mMethodNode);
		if(checkcastClasses.size() > 0) {
			for(String clazzName : checkcastClasses) {
				// TODO bug点：未考虑到系统自带类为非public的问题，这种情况几乎不存在，所以先忽略
				// 修改checkcast对应的类的权限为public
				ClassNode node = Env.classTree.getNode(clazzName);
				if(node != null && (node.access & ACC_PUBLIC) != ACC_PUBLIC) {
					node.access = (node.access & (~ACC_PRIVATE) & (~ACC_PROTECTED) & (~ACC_PUBLIC)) | ACC_PUBLIC;
				}
			}
		}
	}
}
