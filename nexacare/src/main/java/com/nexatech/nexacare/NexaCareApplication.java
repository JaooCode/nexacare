package com.nexatech.nexacare;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.nio.file.Files;
import java.nio.file.Path;

@SpringBootApplication
public class NexaCareApplication {
    public static void main(String[] args) {
        ajustarTempWindows();
        SpringApplication.run(NexaCareApplication.class, args);
    }


    private static void ajustarTempWindows() {
        if (!System.getProperty("os.name", "").toLowerCase().contains("win")
                || System.getProperty("jdk.net.unixdomain.tmpdir") != null) {
            return;
        }
        String[] candidatas = {System.getenv("PUBLIC"), "C:\\Users\\Public", "C:\\Windows\\Temp"};
        for (String pasta : candidatas) {
            if (pasta != null && Files.isDirectory(Path.of(pasta)) && Files.isWritable(Path.of(pasta))) {
                System.setProperty("jdk.net.unixdomain.tmpdir", pasta);
                return;
            }
        }
    }
}
