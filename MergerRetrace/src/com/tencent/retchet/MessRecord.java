package com.tencent.retchet;
/**
 * @author VinceXie vince.xie@qq.com
 * @version 2014-9-15 下午3:35:37
 * State
 */
public class MessRecord {

	public String orgName;
	
	public String messName;
	
	public MessRecord(String recordString)
	{
		String[] tmp=recordString.split(" ");
		orgName=tmp[0];
		messName=tmp[2];
	}
	
	public MessRecord(String orgName,String messName)
	{
		this.orgName=orgName;
		this.messName=messName;
	}
	
	public String toString()
	{
		return orgName+" -> "+messName;
	}
}
