package com.example.aimaster.console;

import com.example.aimaster.service.CareerMasterService;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * 控制台交互模式：在同一终端输入问题，AI 在终端中回复。
 * 需启用：--spring.profiles.active=dev,console 或 app.console.enabled=true
 */
@Component
@Order(1)
@ConditionalOnProperty(name = "app.console.enabled", havingValue = "true")
public class ConsoleChatRunner implements ApplicationRunner {

    private final CareerMasterService careerMasterService;

    public ConsoleChatRunner(CareerMasterService careerMasterService) {
        this.careerMasterService = careerMasterService;
    }

    @Override
    public void run(ApplicationArguments args) {
        ExecutorService executor = Executors.newSingleThreadExecutor(r -> {
            Thread t = new Thread(r, "console-chat");
            t.setDaemon(true);
            return t;
        });
        executor.submit(this::runLoop);
    }

    private void runLoop() {
        try {
            BufferedReader in = new BufferedReader(new InputStreamReader(System.in, StandardCharsets.UTF_8));
            PrintStream out = System.out;

            out.println();
            out.println("========== AI 计算机学生职规大师 (Console) ==========");
            out.println("输入问题后回车。输入 exit 或 quit 退出。");
            out.println("==============================================");
            String conversationId = UUID.randomUUID().toString();
            out.print("You> ");
            out.flush();

            String line;
            while ((line = in.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty()) {
                    out.print("You> ");
                    out.flush();
                    continue;
                }
                if ("exit".equalsIgnoreCase(line) || "quit".equalsIgnoreCase(line)) {
                    out.println("Bye ~");
                    break;
                }

                out.println("AI> Thinking...");
                try {
                    String reply = careerMasterService.chat(conversationId, line).getReply();
                    out.println("AI> " + reply);
                } catch (Exception e) {
                    out.println("AI> Error: " + e.getMessage());
                }
                out.println();
                out.print("You> ");
                out.flush();
            }
        } catch (IOException e) {
            // 非交互环境（如无 TTY）时可能失败，忽略
        }
    }
}
