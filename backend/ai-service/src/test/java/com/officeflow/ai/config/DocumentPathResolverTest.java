package com.officeflow.ai.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("AI 文档目录解析测试")
class DocumentPathResolverTest {

    @TempDir
    Path tempDir;

    @Test
    @DisplayName("从 backend 目录启动时应找到项目根目录的 docs/pdf")
    void shouldResolveDocsFromBackendWorkingDirectory() throws Exception {
        Path projectRoot = tempDir.resolve("OfficeFlow");
        Path backend = Files.createDirectories(projectRoot.resolve("backend"));
        Path documents = Files.createDirectories(projectRoot.resolve("docs/pdf"));
        Files.createFile(projectRoot.resolve("docker-compose.yml"));

        Path resolved = DocumentPathResolver.resolve("docs/pdf", backend);

        assertThat(resolved).isEqualTo(documents);
    }

    @Test
    @DisplayName("从具体服务目录启动时也应向上找到 docs/pdf")
    void shouldResolveDocsFromServiceWorkingDirectory() throws Exception {
        Path projectRoot = tempDir.resolve("OfficeFlow");
        Path serviceDir = Files.createDirectories(projectRoot.resolve("backend/ai-service"));
        Path documents = Files.createDirectories(projectRoot.resolve("docs/pdf"));

        Path resolved = DocumentPathResolver.resolve("docs/pdf", serviceDir);

        assertThat(resolved).isEqualTo(documents);
    }
}
