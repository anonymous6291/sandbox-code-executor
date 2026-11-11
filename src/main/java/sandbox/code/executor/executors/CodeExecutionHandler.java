package sandbox.code.executor.executors;

/*
 * Copyright (c) 2026 Anish Kumar Shaw
 *
 * Licensed under the MIT License.
 * See the LICENSE file in the project root for license terms.
 *
 * Original project:
 * https://github.com/anonymous6291/
 */

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.UUID;

public class CodeExecutionHandler {

    private static final Duration RECHECK_DELAY = Duration.ofMillis(50);
    private static final Base64.Decoder base64Decoder = Base64.getDecoder();
    private static final Base64.Encoder base64Encoder = Base64.getEncoder();
    private final long maxRuntimeMillis;
    private final String inputDirectory;
    private final String outputDirectory;
    private final String languageName;
    private final String codeFileName;
    private final String[] runArguments;

    private CodeExecutionHandler(long maxRuntimeMillis, String inputDirectory, String outputDirectory, String languageName, String codeFileName, String[] runArguments) {
        this.maxRuntimeMillis = maxRuntimeMillis;
        this.inputDirectory = inputDirectory;
        this.outputDirectory = outputDirectory;
        this.languageName = languageName;
        this.codeFileName = codeFileName;
        this.runArguments = runArguments;
    }

    public static Builder newBuilder(String codeFileName, String[] runArguments) {
        return new Builder(codeFileName, runArguments);
    }

    private String sanitizeFileName(String fileName) {
        return Path.of(fileName).normalize().getFileName().toString();
    }

    private String base64Encode(byte[] data) {
        return base64Encoder.encodeToString(data);
    }

    private byte[] base64Decode(String data) {
        return base64Decoder.decode(data);
    }

    public Result executeCode(Query query, String workingDir) throws Exception {

        if (workingDir != null) {
            workingDir = sanitizeFileName(workingDir);
        }

        if (workingDir == null || workingDir.isBlank()) {
            workingDir = UUID.randomUUID().toString();
        }

        Path workingDirPath = Path.of(workingDir);

        deleteAllSubFiles(workingDirPath);

        Files.createDirectories(workingDirPath);

        try {

            boolean addOutputFiles = !(query.ignore_output() || outputDirectory == null || outputDirectory.isBlank());

            if (addOutputFiles) {
                Path outputDir = Path.of(workingDir, outputDirectory);
                Files.createDirectories(outputDir);
            }

            if (inputDirectory != null && !inputDirectory.isBlank()) {
                Path inputDir = Path.of(workingDir, inputDirectory);
                Files.createDirectories(inputDir);

                if (query.documents() != null) {
                    for (DocumentNameAndData documentNameAndData : query.documents()) {

                        Path documentPath = Path.of(
                                inputDir.toString(),
                                sanitizeFileName(documentNameAndData.document_name())
                        );

                        Files.write(documentPath, base64Decode(documentNameAndData.base64_data()));
                    }
                }
            }

            Path codePath = Path.of(
                    workingDir,
                    codeFileName
            );

            Files.write(codePath, base64Decode(query.base64_code()));

            ProcessBuilder processBuilder = new ProcessBuilder(runArguments);

            processBuilder.directory(new File(workingDir));

            Process process = processBuilder.start();

            long timeOut = System.currentTimeMillis() + maxRuntimeMillis;

            while (process.isAlive() && System.currentTimeMillis() < timeOut) {
                Thread.sleep(RECHECK_DELAY);
            }

            if (process.isAlive()) {
                process.destroyForcibly();
            }

            String normalOutput = new String(
                    process.getInputStream().readAllBytes()
            );

            String errorOutput = new String(
                    process.getErrorStream().readAllBytes()
            );

            process.close();

            List<DocumentNameAndData> generatedDocuments = new ArrayList<>();

            if (addOutputFiles) {
                getAllGeneratedDocuments(Path.of(workingDirPath.toString(), outputDirectory), generatedDocuments);
            }

            return new Result(
                    false,
                    normalOutput,
                    errorOutput,
                    generatedDocuments
            );
        } finally {
            try {
                deleteAllSubFiles(workingDirPath);
            } catch (Exception _) {
            }
        }
    }

    private void deleteAllSubFiles(Path path) {
        if (!Files.exists(path)) {
            return;
        }

        try {
            if (Files.isDirectory(path)) {
                Files.list(path).forEach(this::deleteAllSubFiles);
            }
            Files.delete(path);
        } catch (Exception _) {
        }
    }

    private void getAllGeneratedDocuments(
            Path path,
            List<DocumentNameAndData> documents
    ) {

        if (!Files.exists(path)) {
            return;
        }

        try {
            if (Files.isDirectory(path)) {
                Files.list(path).forEach(x -> getAllGeneratedDocuments(x, documents));
            } else {
                documents.add(
                        new DocumentNameAndData(
                                path.getFileName().toString(),
                                base64Encode(Files.readAllBytes(path))
                        )
                );
            }
        } catch (Exception _) {
        }
    }

    public record Query(String code_type, String base64_code,
                        List<DocumentNameAndData> documents, boolean ignore_output) {
    }

    public record DocumentNameAndData(String document_name, String base64_data) {
    }

    public record Result(boolean error, String normal_output, String error_output,
                         List<DocumentNameAndData> generated_documents) {
    }

    public static class Builder {
        private final String codeFileName;
        private final String[] runArguments;
        private long maxRuntimeMillis = -1;
        private String inputDirectory = null;
        private String outputDirectory = null;
        private String languageName = null;

        private Builder(String codeFileName, String[] runArguments) {
            this.codeFileName = codeFileName;
            this.runArguments = runArguments;
        }

        public Builder setMaxRuntimeMillis(long maxRuntimeMillis) {
            this.maxRuntimeMillis = maxRuntimeMillis;
            return this;
        }

        public Builder setInputDirectory(String inputDirectory) {
            this.inputDirectory = inputDirectory;
            return this;
        }

        public Builder setOutputDirectory(String outputDirectory) {
            this.outputDirectory = outputDirectory;
            return this;
        }

        public Builder setLanguageName(String languageName) {
            this.languageName = languageName;
            return this;
        }

        public CodeExecutionHandler build() {
            return new CodeExecutionHandler(
                    this.maxRuntimeMillis,
                    this.inputDirectory,
                    this.outputDirectory,
                    this.languageName,
                    this.codeFileName,
                    this.runArguments
            );
        }
    }
}
