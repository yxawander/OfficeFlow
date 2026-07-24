package com.officeflow.ai.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * 将文档目录解析为稳定的绝对路径。
 * 支持从项目根目录、backend 或具体服务目录启动应用。
 */
@Component
public class DocumentPathResolver {

    private final String configuredPath;

    public DocumentPathResolver(@Value("${app.document.path:docs/pdf}") String configuredPath) {
        this.configuredPath = configuredPath;
    }

    public Path resolveDirectory() {
        return resolve(configuredPath, Paths.get("").toAbsolutePath());
    }

    static Path resolve(String configuredPath, Path workingDirectory) {
        Path configured = Paths.get(configuredPath);
        if (configured.isAbsolute()) {
            return configured.normalize();
        }

        Path current = workingDirectory.toAbsolutePath().normalize();
        Path projectRoot = null;
        while (current != null) {
            Path candidate = current.resolve(configured).normalize();
            if (Files.isDirectory(candidate)) {
                return candidate;
            }
            if (Files.isDirectory(current.resolve("backend"))
                    && Files.exists(current.resolve("docker-compose.yml"))) {
                projectRoot = current;
            }
            current = current.getParent();
        }

        if (projectRoot != null) {
            return projectRoot.resolve(configured).normalize();
        }
        return workingDirectory.toAbsolutePath().normalize().resolve(configured).normalize();
    }
}
