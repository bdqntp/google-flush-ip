package google.ip.flush;


import cn.hutool.core.util.StrUtil;
import org.apache.commons.io.FileUtils;

import java.io.*;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

public class HandleMain {
    private static String remote_ip = "https://raw.githubusercontent.com/hcfyapp/google-translate-cn-ip/main/ips.txt";
    private static String local_ip_name = "ips.txt";

    public static void main(String[] args) {
        replaceIp();
    }

    //获取远程ip列表
    public static File getRemoteIpFile() {
        File file = new File(local_ip_name);
        try {
            FileUtils.copyURLToFile(new URL(remote_ip), file, 3000, 3000);
        } catch (IOException e) {
            System.err.println("获取云端ip失败,请重试几次，网络不稳定，会尝试使用本地缓存");
        }
        return file;
    }

    private static final int isPingIPReachable(String hostname) throws IOException {
        if (System.getProperty("os.name").contains("Windows")) {
            return isPingIPReachableWin(hostname);
        } else {
            return isPingIPReachableMac(hostname);
        }
    }


    //判读ip延迟window
    private static final int isPingIPReachableMac(String hostname) throws IOException {
        BufferedReader br = null;
        InputStreamReader inputStreamReader = null;
        try {
            Process exec = Runtime.getRuntime().exec("ping -c 1 " + hostname);
            inputStreamReader = new InputStreamReader(exec.getInputStream(), "GB2312");
            br = new BufferedReader(inputStreamReader);
            String line = null;
            StringBuffer sb = new StringBuffer();
            while ((line = br.readLine()) != null) {
                if (line.toString().contains("time")) {
                    String[] var = line.split(" ");
                    String time = var[6].substring(var[6].toString().lastIndexOf("time") + 5, var[6].length() - 1);
                    return Double.valueOf(time).intValue();
                }
            }
            return -1;
        } catch (IOException e) {
            e.printStackTrace();
            return -1;
        } finally {
            if (null != br) {
                br.close();
            }
            if (null != inputStreamReader) {
                inputStreamReader.close();
            }
        }
    }

    //判读ip延迟windows
    private static final int isPingIPReachableWin(String hostname) throws IOException {
        BufferedReader br = null;
        InputStreamReader inputStreamReader = null;
        try {
            Process exec = Runtime.getRuntime().exec("ping -c 1 " + hostname);
            inputStreamReader = new InputStreamReader(exec.getInputStream(), "GB2312");
            br = new BufferedReader(inputStreamReader);
            String line = null;
            StringBuffer sb = new StringBuffer();
            while ((line = br.readLine()) != null) {
                if (line.toString().contains("时间")) {
                    String[] var = line.split(" ");
                    String time = var[4].substring(var[4].toString().lastIndexOf("time") + 4, var[4].length() - 2);
                    return Double.valueOf(time).intValue();
                }
            }
            return -1;
        } catch (IOException e) {
            e.printStackTrace();
            quit();
            return -1;
        } finally {
            if (null != br) {
                br.close();
            }
            if (null != inputStreamReader) {
                inputStreamReader.close();
            }
        }
    }

    //刷新host
    private static final void flushHost() {
        try {
            String command = "/etc/init.d/network restart";
            if (System.getProperty("os.name").contains("Windows")) {
                command = "ipconfig /flushdns";
            } else if (System.getProperty("os.name").contains("Mac")) {
                command = "killall -HUP mDNSResponder";
            }
            Process exec = Runtime.getRuntime().exec(command);
        } catch (IOException e) {
            e.printStackTrace();
            quit();
        }
    }

    //获取可用ip
    public static String getAvailableIp() throws IOException {
        File file = getRemoteIpFile();
        System.out.println("获取远程ip成功");

        FileReader fr = null;
        BufferedReader br = null;
        try {
            fr = new FileReader(file);
            br = new BufferedReader(fr);
            String line;
            while ((line = br.readLine()) != null) {
                System.out.println("检测ip" + line + "中......");
                int pingTime = isPingIPReachable(line);
                if (pingTime != -1) {
                    System.out.println("检测ip" + line + "成功，延迟" + pingTime + "毫秒");
                    return line;
                } else {
                    System.out.println("检测ip" + line + "，不能连通，继续检测下一个");
                }
            }
        } catch (IOException e) {
            System.err.println("检测ip延迟出错,请重试，并且开始管理员运行");
            e.printStackTrace();
            quit();
        } finally {
            try {
                if (null != fr) {
                    fr.close();
                }
                if (null != br) {
                    br.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
                quit();
                throw new RuntimeException(e);
            }
        }

        return null;
    }

    //替换机器现有host
    public static void replaceIp() {
        String filePath = "/etc/hosts";
        if (System.getProperty("os.name").contains("Windows")) {
            filePath = "C:/windows/system32/drivers/etc/hosts";
        }
        System.out.println("获取host地址：" + filePath);

        File oldHostFile = new File(filePath);

        FileWriter fw = null;
        BufferedWriter bw = null;

        FileReader fr = null;
        BufferedReader br = null;

        try {
            List<String> newData = new ArrayList<>();

            fr = new FileReader(oldHostFile);
            br = new BufferedReader(fr);
            String line;
            while ((line = br.readLine()) != null) {
                if (!StrUtil.isBlank(line) && !line.contains("translate.googleapis.com")) {
                    newData.add(line);
                }
            }
            String availableIp = getAvailableIp();
            line = availableIp + " translate.googleapis.com";
            newData.add(line);

            try {
                if (null != br) {
                    br.close();
                }
                if (null != fr) {
                    fr.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
                throw new RuntimeException(e);
            }
            fw = new FileWriter(new File(filePath));
            bw = new BufferedWriter(fw);

            System.out.println("------------------替换后的文件内容-------------------");
            for (String newDatum : newData) {
                System.out.println(newDatum);
                bw.newLine();
                bw.write(newDatum);
                bw.flush();
            }
            System.out.println("------------------替换后的文件内容-------------------");
            quit();
        } catch (IOException e) {
            System.err.println("替换文件失败，请使用管理员身份运行重试");
            e.printStackTrace();
            quit();
        } finally {
            try {
                if (null != bw) {
                    bw.close();
                }
                if (null != fw) {
                    fw.close();
                }
                if (null != br) {
                    br.close();
                }
                if (null != fr) {
                    fr.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
                quit();
                throw new RuntimeException(e);
            }
            flushHost();
        }
    }

    public static void quit(){
        System.out.println("10秒后自动推出");
        try {
            Thread.sleep(10000);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }
}
