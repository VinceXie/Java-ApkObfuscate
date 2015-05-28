package com.tencent.obfuscate.hide;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;

/**
 * @author VinceXie vince.xie@qq.com
 * @version 2014-4-2 下午4:35:53 State
 * readline会塞掉一个键盘输入
 */
public class RunCommand {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		Run("cmd",false);
	}

	public static void Run(String cmd, boolean isCommunicate) {
		Runtime rt = Runtime.getRuntime();
		Process process;
		try {
			process = rt.exec(cmd);

			// 获取进程的标准输入流
			final InputStream is1 = process.getInputStream();
			// 获取进城的错误流
			final InputStream is2 = process.getErrorStream();
			// 启动两个线程，一个线程负责读标准输出流，另一个负责读标准错误流

			new Thread() {
				public void run() {

					try {
						BufferedReader br1 = new BufferedReader(
								new InputStreamReader(is1, "gbk"));
						int ch = 0;
						while ((ch = br1.read()) != -1) {
							System.out.print((char) ch);
						}

					} catch (IOException e) {
						e.printStackTrace();
					} finally {
						try {
							is1.close();
						} catch (IOException e) {
							e.printStackTrace();
						}
					}
				}
			}.start();

			new Thread() {
				public void run() {

					try {
						BufferedReader br2 = new BufferedReader(
								new InputStreamReader(is2, "gbk"));
						int ch = 0;
						while ((ch = br2.read()) != -1) {
							System.out.print((char) ch);
						}
					} catch (IOException e) {
						e.printStackTrace();
					} finally {
						try {
							is2.close();
						} catch (IOException e) {
							e.printStackTrace();
						}
					}
				}
			}.start();

			Thread outThread=null;
			if (isCommunicate) {
				// 进行输入
				PrintWriter writer = new PrintWriter(process.getOutputStream());
				outThread = new CommandThread(writer);
				outThread.start();
			}

			process.waitFor();
			process.destroy();
			outThread.isInterrupted();

			if (isCommunicate) {
				outThread.interrupt();
			}
			System.out.println("done");

		} catch (Exception e) {
			System.out.println(e.getMessage());

		}
	}

	/**
	 * 输入线程
	 * 
	 * @author vincexie
	 * 
	 */
	static class CommandThread extends Thread {
		PrintWriter writer;
		BufferedReader br = null;

		CommandThread(PrintWriter writer) {
			this.writer = writer;
			br = new BufferedReader(new InputStreamReader(System.in));
			this.setDaemon(true);
		}

		@Override
		public void run() {
			try {

				// Scanner scanner=new Scanner(System.in);
				String cmd;
				// 等待键盘输入,stop后无法立刻终止。
				while ((cmd = br.readLine()) != null) {

					writer.println(cmd);
					writer.flush();
					if (Thread.currentThread().isInterrupted()) {
						break;
					}
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}
}
