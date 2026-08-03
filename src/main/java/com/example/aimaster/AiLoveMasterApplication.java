package com.example.aimaster;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.io.FileDescriptor;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.nio.charset.Charset;

@SpringBootApplication(scanBasePackages = "com.example.aimaster")
@MapperScan("com.example.aimaster.mapper")
public class AiLoveMasterApplication {

    public static void main(String[] args) {
        // Windows 下 IDE/终端默认用 GBK 解析控制台，用 GBK 输出可避免乱码；非 Windows 用 UTF-8
        boolean isWindows = System.getProperty("os.name", "").toLowerCase().contains("windows");
        if (isWindows) {
            System.setProperty("log.console.charset", "GBK");
        }
        try {
            Charset consoleCharset = isWindows ? Charset.forName("GBK") : java.nio.charset.StandardCharsets.UTF_8;
            System.setOut(new PrintStream(new FileOutputStream(FileDescriptor.out), true, consoleCharset));
            System.setErr(new PrintStream(new FileOutputStream(FileDescriptor.err), true, consoleCharset));
        } catch (Exception ignored) {
        }
        SpringApplication.run(AiLoveMasterApplication.class, args);
    }
}
