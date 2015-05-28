
package com.tencent.obfuscate.dex;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Random;

import org.apache.commons.io.FileUtils;

/**
 * @author VinceXie vince.xie@qq.com
 * @version 2014-10-24 上午11:18:33 State
 */
public class DexObfuscator {

    public static String LineBreak = System.getProperties().getProperty(
            "line.separator");

    // 少于n的只切分为两份 多于section数的只切分为section
    public static int n = 3;
    public static int Section = 6;
    // 1为默认模式,0为最大兼容模式。
    public static int Mode = 0;
    public static boolean isJunk = true;
    public static final String INJECT_STRING = "    invoke-static {}, LEm;->Junk()V";

    public static void main(String[] args) throws Exception {

        // 读取参数
        if (args != null && args.length != 0) {
            int i = 0;
            while (true)
            {
                if (args[0].equals("-j"))
                {
                    isJunk = true;
                    i++;
                }
                else if (args[0].equals("-m"))
                {
                    Mode = Integer.valueOf(args[i + 1]);
                    i++;
                    i++;
                }

                if (i == args.length)
                {
                    break;
                }
            }

        }

        Upset2();

        if (isJunk)
        {
            InjectJunk();
        }
    }

    public static void InjectJunk() throws Exception {
        InputStream is = DexObfuscator.class.getResourceAsStream("Em.smali");
        FileUtils.copyInputStreamToFile(is, new File("out/Em.smali"));
        Collection<File> smalis = FileUtils.listFiles(new File("out"),
                new String[] {
                    "smali"
                }, true);
        for (File smali : smalis) {

            String newSmali = "";

            // 读取
            BufferedReader br = new BufferedReader(new InputStreamReader(
                    new FileInputStream(smali)));
            for (String line = br.readLine(); line != null; line = br
                    .readLine()) {

                if (line.startsWith("    if-")
                        || line.startsWith("    throw")
                        || line.startsWith("    in")) {
                    if (randInt(1, 10) > 7)
                    {
                        newSmali = newSmali + INJECT_STRING + LineBreak;
                    }
                }

                // if (line.startsWith("    throw")) {
                // if(randInt(1, 10)>7)
                // {
                // newSmali = newSmali + INJECT_STRING + LineBreak;
                // }
                // }
                //
                // if(line.startsWith("    in"))
                // {
                // if(randInt(1, 10)>7)
                // {
                // newSmali = newSmali + INJECT_STRING + LineBreak;
                // }
                // }

                newSmali = newSmali + line + LineBreak;

            }
            br.close();

            // 写入
            FileUtils.writeStringToFile(smali, newSmali, false);
        }
        return;

    }

    // 测试模式
    // public static void Upset1() throws Exception {
    // Collection<File> smalis = FileUtils.listFiles(new File("out"),
    // new String[] { "smali" }, true);
    // for (File smali : smalis) {
    // System.out.println(smali.getAbsolutePath());
    //
    // String lineSmali = "";
    // String newSmali = "";
    // String oldSmali = "";
    // ArrayList<String> oneLine = null;
    // ArrayList<ArrayList<String>> lines = null;
    // boolean inMethod = false;
    // boolean inLine = false;
    // boolean unDo = false;
    // boolean inPrologue = false;
    // boolean hasArray = false;
    //
    // // 读取
    // BufferedReader br = new BufferedReader(new InputStreamReader(
    // new FileInputStream(smali)));
    // for (String line = br.readLine(); line != null; line = br
    // .readLine()) {
    // if (line.equals("")) {
    // continue;
    // }
    // if (line.startsWith(".method") && !inMethod) {
    // inMethod = true;
    // newSmali = newSmali + line + LineBreak;
    // continue;
    // }
    // if (line.startsWith(".end method") && inMethod) {
    // inMethod = false;
    // inLine = false;
    // inPrologue = false;
    //
    // if ((lines == null || (lines != null && lines.size() == 1))
    // && !unDo && !hasArray) {
    // // newSmali = newSmali + DoUpsetWithoutLine(oldSmali);
    // newSmali = newSmali + oldSmali;
    // } else if ((lines == null || (lines != null && lines.size() == 1))
    // && (unDo || hasArray)) {
    // newSmali = newSmali + oldSmali;
    // } else if (lines != null && !unDo) {
    // newSmali = newSmali + lineSmali + DoUpset(lines);
    //
    // } else if (lines != null && unDo) {
    // newSmali = newSmali + lineSmali + UndoUpset(lines);
    // }
    // //newSmali = newSmali + oldSmali;
    //
    // newSmali = newSmali + line + LineBreak;
    // lines = null;
    // unDo = false;
    // oldSmali = "";
    // lineSmali = "";
    // continue;
    // }
    // if (inMethod) {
    // if (line.startsWith("    .catch")||line.startsWith("    move")) {
    // unDo = true;
    // }
    // if (line.startsWith("    .array-data")
    // || line.startsWith("    .packed-switch")
    // || line.startsWith("    .sparse-switch")) {
    // hasArray = true;
    // }
    //
    // if (inPrologue) {
    // oldSmali = oldSmali + line + LineBreak;
    // } else if (!line.startsWith("    .")
    // && line.charAt(4) != ' ') {
    // oldSmali = oldSmali + line + LineBreak;
    // inPrologue = true;
    // }
    //
    // if (line.startsWith("    .line")) {
    // inLine = true;
    // oneLine = new ArrayList<String>();
    // if (lines == null) {
    // lines = new ArrayList<ArrayList<String>>();
    // }
    // lines.add(oneLine);
    // oneLine.add(line);
    // } else if (inLine == true) {
    // oneLine.add(line);
    // } else if (inPrologue) {
    // lineSmali = lineSmali + line + LineBreak;
    // } else {
    // newSmali = newSmali + line + LineBreak;
    // }
    // } else {
    // newSmali = newSmali + line + LineBreak;
    // }
    // }
    // br.close();
    //
    // // 写入
    // FileUtils.writeStringToFile(smali, newSmali, false);
    // }
    // }

    static boolean inLine = false;

    public static void Upset2() throws Exception {
        Collection<File> smalis = FileUtils.listFiles(new File("out"),
                new String[] {
                    "smali"
                }, true);
        for (File smali : smalis) {
            System.out.println(smali.getAbsolutePath());

            // String lineSmali = "";
            String newSmali = "";
            String oldSmali = "";
            // Code oneLine = null;
            // ArrayList<Code> lines = null;
            boolean inMethod = false;
            boolean inLine1 = false;
            boolean unDo = false;
            boolean inPrologue = false;
            // boolean hasArray = false;

            // 读取
            BufferedReader br = new BufferedReader(new InputStreamReader(
                    new FileInputStream(smali)));
            for (String line = br.readLine(); line != null; line = br
                    .readLine()) {
                if (line.equals("")) {
                    continue;
                }
                if (line.startsWith(".method") && !inMethod) {
                    inMethod = true;
                    newSmali = newSmali + line + LineBreak;
                    continue;
                }
                if (line.startsWith(".end method") && inMethod) {

                    // if ((lines == null || (lines != null && lines.size() ==
                    // 1))
                    // && !unDo && !hasArray) {
                    // newSmali = newSmali + DoUpset2WithoutLine(oldSmali);
                    // } else if ((lines == null || (lines != null &&
                    // lines.size() == 1))
                    // && (unDo || hasArray)) {
                    // newSmali = newSmali + oldSmali;
                    // } else if (lines != null && !unDo) {
                    // newSmali = newSmali + DoUpset2WithoutLine(oldSmali);
                    //
                    // } else if (lines != null && unDo) {
                    // newSmali = newSmali + lineSmali + UndoUpset2(lines);
                    // }
                    if (!unDo) {
                        newSmali = newSmali + DoUpset2(oldSmali);
                    } else {
                        newSmali = newSmali + oldSmali;
                    }

                    newSmali = newSmali + line + LineBreak;
                    inLine = false;
                    unDo = false;
                    oldSmali = "";
                    inMethod = false;
                    inLine1 = false;
                    inPrologue = false;
                    // lineSmali = "";
                    continue;
                }
                if (inMethod) {
                    // ||line.startsWith("    if")
                    if (line.startsWith("    .catch")
                            || line.startsWith("    if")
                            || line.startsWith("    .array-data")
                            || line.startsWith("    .packed-switch")
                            || line.startsWith("    .sparse-switch")) {
                        unDo = true;
                    }

                    if (inPrologue) {
                        oldSmali = oldSmali + line + LineBreak;
                    } else if (!line.startsWith("    .")
                            && line.charAt(4) != ' ') {
                        oldSmali = oldSmali + line + LineBreak;
                        inPrologue = true;
                    }

                    if (line.startsWith("    .line") && !inLine1) {
                        inLine1 = true;
                    } else if (line.startsWith("    .line") && inLine1) {
                        inLine = true;
                    }
                    // inLine = true;
                    // oneLine = new Code();
                    // if (lines == null) {
                    // lines = new ArrayList<Code>();
                    // }
                    // lines.add(oneLine);
                    // oneLine.codes.add(line);
                    // }
                    // else if (inLine == true) {
                    // oneLine.codes.add(line);
                    // } else
                    // if (inPrologue) {
                    // lineSmali = lineSmali + line + LineBreak;
                    // } else {
                    // newSmali = newSmali + line + LineBreak;
                    // }
                    if (!inPrologue) {
                        newSmali = newSmali + line + LineBreak;
                    }
                } else {
                    newSmali = newSmali + line + LineBreak;
                }
            }
            br.close();

            // 写入
            FileUtils.writeStringToFile(smali, newSmali, false);
        }
        return;
    }

    // public static String UndoUpset(ArrayList<ArrayList<String>> lines) {
    // String newSmali = "";
    // for (int i = 0; i < lines.size(); i++) {
    // for (String line : lines.get(i)) {
    // newSmali = newSmali + line + LineBreak;
    // }
    // }
    // return newSmali;
    // }

    // public static String UndoUpset2(ArrayList<Code> lines) {
    // String newSmali = "";
    // for (int i = 0; i < lines.size(); i++) {
    // for (String line : lines.get(i).codes) {
    // newSmali = newSmali + line + LineBreak;
    // }
    // }
    // return newSmali;
    // }

    public static String DoUpset2(ArrayList<Code> lines) {
        lines = MerLines(lines);
        System.out.println(lines.size());
        String newSmali = "";
        if (lines.size() == 1) {
            for (int i = 0; i < lines.size(); i++) {
                for (String line : lines.get(i).codes) {
                    newSmali = newSmali + line + LineBreak;
                }
            }
        } else if (lines.size() > 1 && lines.size() < n) {
            newSmali = newSmali + "goto :lab1" + LineBreak;
            newSmali = newSmali + ":lab2" + LineBreak;
            for (int i = lines.size() / 2; i < lines.size(); i++) {
                for (String line : lines.get(i).codes) {
                    newSmali = newSmali + line + LineBreak;
                }
            }
            newSmali = newSmali + ":lab1" + LineBreak;
            for (int i = 0; i < lines.size() / 2; i++) {
                for (String line : lines.get(i).codes) {
                    newSmali = newSmali + line + LineBreak;
                }
            }
            newSmali = newSmali + "goto :lab2" + LineBreak;
        } else {
            if (lines.size() > Section) {
                // 合并多段代码
                int surplus = lines.size() - Section;
                ArrayList<Integer> mergeInts = new ArrayList<Integer>();
                for (int i = 0; i < surplus; i++) {
                    mergeInts.add(randInt(1, lines.size() - 1 - i));
                }
                for (Integer mergeInt : mergeInts) {
                    lines.get(mergeInt.intValue() - 1).codes.addAll(lines
                            .get(mergeInt.intValue()).codes);
                    lines.remove(mergeInt.intValue());
                }
            }

            for (Code code : lines) {
                code.pos = lines.indexOf(code);
            }

            long seed = System.nanoTime();
            Collections.shuffle(lines, new Random(seed));
            String go = "goto :lab";
            String lab = ":lab";
            newSmali = go + 0 + LineBreak;
            for (Code code : lines) {
                newSmali = newSmali + lab + code.pos + LineBreak;
                for (String line : code.codes) {
                    newSmali = newSmali + line + LineBreak;
                }
                if (code.pos != lines.size() - 1) {
                    newSmali = newSmali + go + (code.pos + 1) + LineBreak;
                }
            }

        }
        return newSmali;

    }

    // public static String DoUpset(ArrayList<ArrayList<String>> lines) {
    //
    // String goto1 = "goto :lab1";
    // String lab1 = ":lab1";
    // String goto2 = "goto :lab2";
    // String lab2 = ":lab2";
    // String goto3 = "goto :lab3";
    // String lab3 = ":lab3";
    // String goto4 = "goto :lab4";
    // String lab4 = ":lab4";
    //
    // String newSmali = "";
    // if (lines.size() == 1) {
    // for (int i = 0; i < lines.size(); i++) {
    // for (String line : lines.get(i)) {
    // newSmali = newSmali + line + LineBreak;
    // }
    // }
    // } else if (lines.size() > 1 && lines.size() < 6) {
    // newSmali = newSmali + "goto :lab1" + LineBreak;
    // newSmali = newSmali + ":lab2" + LineBreak;
    // for (int i = lines.size() / 2; i < lines.size(); i++) {
    // for (String line : lines.get(i)) {
    // newSmali = newSmali + line + LineBreak;
    // }
    // }
    // newSmali = newSmali + ":lab1" + LineBreak;
    // for (int i = 0; i < lines.size() / 2; i++) {
    // for (String line : lines.get(i)) {
    // newSmali = newSmali + line + LineBreak;
    // }
    // }
    // newSmali = newSmali + "goto :lab2" + LineBreak;
    // } else {
    // int space = lines.size() / 4;
    // newSmali = newSmali + goto1 + LineBreak;
    //
    // newSmali = newSmali + lab4 + LineBreak;
    // for (int i = space * 3; i < lines.size(); i++) {
    // for (String line : lines.get(i)) {
    // newSmali = newSmali + line + LineBreak;
    // }
    // }
    //
    // newSmali = newSmali + lab3 + LineBreak;
    // for (int i = space * 2; i < space * 3; i++) {
    // for (String line : lines.get(i)) {
    // newSmali = newSmali + line + LineBreak;
    // }
    // }
    // newSmali = newSmali + goto4 + LineBreak;
    //
    // newSmali = newSmali + lab2 + LineBreak;
    // for (int i = space * 1; i < space * 2; i++) {
    // for (String line : lines.get(i)) {
    // newSmali = newSmali + line + LineBreak;
    // }
    // }
    // newSmali = newSmali + goto3 + LineBreak;
    //
    // newSmali = newSmali + lab1 + LineBreak;
    // for (int i = 0; i < space; i++) {
    // for (String line : lines.get(i)) {
    // newSmali = newSmali + line + LineBreak;
    // }
    // }
    // newSmali = newSmali + goto2 + LineBreak;
    // }
    //
    // return newSmali;
    // }

    // public static String DoUpsetWithoutLine(String oldsmali) {
    //
    // String[] codes = oldsmali.split(LineBreak);
    // if (codes.length == 1) {
    // return oldsmali;
    // }
    //
    // ArrayList<String> codesList = new ArrayList<String>(
    // Arrays.asList(codes));
    // String newSmali = "";
    //
    // ArrayList<String> oneLine = null;
    // ArrayList<ArrayList<String>> lines = new ArrayList<ArrayList<String>>();
    //
    // if (codesList.get(0).startsWith("    .line")) {
    // newSmali = codesList.get(0) + LineBreak;
    // codesList.remove(0);
    // }
    //
    // for (int i = 0; i < codesList.size(); i++) {
    // String code = codesList.get(i);
    // if (!code.startsWith("    move")||lines.size()==0) {
    // oneLine = new ArrayList<String>();
    // oneLine.add(code);
    // lines.add(oneLine);
    // } else {
    // oneLine = lines.get(lines.size() - 1);
    // oneLine.add(code);
    // }
    // }
    //
    // return newSmali + DoUpset(lines);
    // }

    public static String DoUpset2(String oldsmali) {

        String[] codes = oldsmali.split(LineBreak);
        if (codes.length == 1) {
            return oldsmali;
        }

        ArrayList<String> codesList = new ArrayList<String>(
                Arrays.asList(codes));
        String newSmali = "";

        Code oneLine = null;
        ArrayList<Code> lines = new ArrayList<Code>();

        // if (codesList.get(0).startsWith("    .line")) {
        // newSmali = codesList.get(0) + LineBreak;
        // codesList.remove(0);
        // }

        for (int i = 0; i < codesList.size(); i++) {
            String code = codesList.get(i);
            // if (!code.startsWith("    move")||lines.size()==0) {
            // oneLine = new Code();
            // oneLine.codes.add(code);
            // lines.add(oneLine);
            // } else {
            // oneLine = lines.get(lines.size() - 1);
            // oneLine.codes.add(code);
            // }
            oneLine = new Code();
            oneLine.codes.add(code);
            lines.add(oneLine);
        }

        return newSmali + DoUpset2(lines);
    }

    public static int randInt(int min, int max) {

        // NOTE: Usually this should be a field rather than a method
        // variable so that it is not re-seeded every call.
        Random rand = new Random();

        // nextInt is normally exclusive of the top value,
        // so add 1 to make it inclusive
        int randomNum = rand.nextInt((max - min) + 1) + min;

        return randomNum;
    }

    public static ArrayList<Code> MerLines(ArrayList<Code> lines) {
        // boolean hasMove=false;
        // for(Code code:lines)
        // {
        // if(code.codes.get(code.codes.size()-1).startsWith("    move"))
        // {
        // hasMove=true;
        // break;
        // }
        // }
        // if(!hasMove)
        // {
        // return lines;
        // }

        // 自下向上以move为分界
        // for(int i=lines.size()-1;i>=0;i--)
        // {
        // Code code=lines.get(i);
        // if(i!=0&&(!lines.get(i-1).codes.get(lines.get(i-1).codes.size()-1).startsWith("    move")
        // ))
        // {
        // lines.get(i-1).codes.addAll(code.codes);
        // lines.remove(i);
        // }
        // }

        // //不以if的上下界作为分界
        // for (int i = 0; i < lines.size(); i++) {
        // if (lines.get(i).codes.get(lines.get(i).codes.size() -
        // 1).startsWith("    if")
        // ||lines.get(i).codes.get(lines.get(i).codes.size() -
        // 1).startsWith("    :")
        // ) {
        // if(i!=0)
        // {
        // lines.get(i-1).codes.addAll(lines.get(i).codes);
        // lines.remove(i);
        // i--;
        // }
        // if(i!=0)
        // {
        // lines.get(i-1).codes.addAll(lines.get(i).codes);
        // lines.remove(i);
        // i--;
        // }
        // if(i>-1&&i!=lines.size()-1)
        // {
        // lines.get(i).codes.addAll(lines.get(i + 1).codes);
        // lines.remove(i + 1);
        // i--;
        // }
        // if(i>-1&&i!=lines.size()-1)
        // {
        // lines.get(i).codes.addAll(lines.get(i + 1).codes);
        // lines.remove(i + 1);
        // i--;
        // }
        //
        // }
        // }

        // 添加行号切分功能
        if (inLine)
        {
            System.out.println("yes!");
            for (int i = 1; i < lines.size(); i++) {
                Code code = lines.get(i);
                if (!(code.codes.get(0).startsWith("    .line"))) {
                    lines.get(i - 1).codes.addAll(lines.get(i).codes);
                    lines.remove(i);
                    i--;
                }
            }
            return lines;
        }

        if (Mode == 0) {
            // 只以move的下界为分界
            for (int i = 0; i < lines.size(); i++) {
                Code code = lines.get(i);
                if (!code.codes.get(code.codes.size() - 1).startsWith(
                        "    move")
                        && i != lines.size() - 1) {
                    code.codes.addAll(lines.get(i + 1).codes);
                    lines.remove(i + 1);
                    i--;
                }
            }
        } else {
            // 不以move的上界为分界
            for (int i = 0; i < lines.size(); i++) {
                Code code = lines.get(i);
                if ((code.codes.get(0).startsWith("    move")
                        || code.codes.get(0).startsWith("    i") ||
                        code.codes.get(0).startsWith("    ."))
                        && i != 0) {
                    lines.get(i - 1).codes.addAll(lines.get(i).codes);
                    lines.remove(i);
                    i--;
                }
            }
        }
        // System.out.println(lines.size());
        return lines;
    }

}
