package com.tencent.retchet;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author VinceXie vince.xie@qq.com
 * @version 2014-7-22 上午11:31:02
 * State
 * java -jar mretrace.jar mmapping.txt error.txt
 */
public class Main {
	
	private static final String REGEX_LINENUMBER=":[0-9]+\\)";
	
	public static void main(String args[]) throws Exception
	{
		if(args.length!=2)
		{
			System.out.println("Usage:java -jar ratchet.jar mapping-merger.txt error.txt");
			return;
		}
		
		
		ArrayList<String> outputs=getOutStrings(args[0], args[1]);
		
		for(String output:outputs)
		{
			System.out.println(output);
		}
	}
	
	public static ArrayList<String> getOutStrings(String mapPath,String errorPath) throws Exception
	{
		ArrayList<String> outputs=new ArrayList<String>();
		Pattern pattern=Pattern.compile(REGEX_LINENUMBER);
		
		//读入error信息
		ArrayList<String> errStrings=new ArrayList<String>();
		BufferedReader br = new BufferedReader(new InputStreamReader(
				new FileInputStream(errorPath)));
		for (String line = br.readLine(); line != null; line = br.readLine()) {
			errStrings.add(line);
		}
		br.close();
		
		//读入mapping
		ArrayList<Record> records=new ArrayList<Record>();
		br = new BufferedReader(new InputStreamReader(
				new FileInputStream(mapPath)));
		for (String line = br.readLine(); line != null; line = br.readLine()) {
			records.add(new Record(line));
		}
		br.close();
		

		//遍历替换出错信息
		for(String error:errStrings)
		{
			Matcher matcher=pattern.matcher(error);
			if(!matcher.find())
			{
				outputs.add(error);
				continue;
			}
			int errorLineNumber=Integer.valueOf(matcher.group(0).substring(1,matcher.group(0).length()-1));
			for(Record record:records)
			{			
				if(error.contains(record.getMerName())&&errorLineNumber>=record.getStart()&&errorLineNumber<=record.getEnd())
				{
					int orgLineNumber=errorLineNumber-record.getOffset();
					error=error.replaceAll(record.getMerName(), record.getOrgName());
					error=error.replaceAll(":"+errorLineNumber+"\\)", ":"+orgLineNumber+")");
				}
			}
			outputs.add(error);
		}
		return outputs;
	}

}
