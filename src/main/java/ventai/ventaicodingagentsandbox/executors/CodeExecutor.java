package ventai.ventaicodingagentsandbox.executors;

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

public abstract class CodeExecutor {

    public static final String CODE_FILE_NAME = "run";
    public static final String INPUT_DIR = "input";
    public static final String OUTPUT_DIR = "output";
    public static final long MAX_RUNTIME_MILLIS = 3 * 1000;
    public static final long MAX_SPACE_MB = 100;
    private static final Duration RECHECK_DELAY = Duration.ofMillis(50);
    private final Base64.Decoder base64Decoder = Base64.getDecoder();
    private final Base64.Encoder base64Encoder = Base64.getEncoder();

    abstract String getFileName();

    abstract String[] getProcessCmdArgs();

    private String sanitizeFileName(String fileName) {
        return Path.of(fileName).normalize().getFileName().toString();
    }

    private String base64Encode(byte[] data) {
        return base64Encoder.encodeToString(data);
    }

    private byte[] base64Decode(String data) {
        return base64Decoder.decode(data);
    }

    Result executeCode(Query query, String workingDir) throws Exception {
        Path workingDirPath = Path.of(workingDir);

        if (Files.exists(workingDirPath)) {
            deleteAllSubFiles(workingDirPath);
        }

        Path inputDir = Path.of(workingDir, INPUT_DIR);
        Path outputDir = Path.of(workingDir, OUTPUT_DIR);

        Files.createDirectories(workingDirPath);
        Files.createDirectories(inputDir);
        Files.createDirectories(outputDir);

        Path codePath = Path.of(
                workingDir,
                getFileName()
        );

        Files.write(codePath, base64Decode(query.base64_code()));

        if (query.documents() != null) {
            for (DocumentNameAndData documentNameAndData : query.documents()) {

                Path documentPath = Path.of(
                        workingDir,
                        INPUT_DIR,
                        sanitizeFileName(documentNameAndData.document_name())
                );

                Files.write(documentPath, base64Decode(documentNameAndData.base64_data()));
            }
        }

        ProcessBuilder processBuilder = new ProcessBuilder(getProcessCmdArgs());

        processBuilder.directory(new File(workingDir));

        Process process = processBuilder.start();

        long timeOut = System.currentTimeMillis() + MAX_RUNTIME_MILLIS;

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

        List<DocumentNameAndData> generatedDocuments = new ArrayList<>();

        getAllGeneratedDocuments(outputDir, generatedDocuments);

        try {
            deleteAllSubFiles(workingDirPath);
        } catch (Exception _) {
        }

        return new Result(
                false,
                normalOutput,
                errorOutput,
                generatedDocuments
        );
    }

    private void deleteAllSubFiles(Path path) {
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
                        List<DocumentNameAndData> documents) {
    }

    public record DocumentNameAndData(String document_name, String base64_data) {
    }

    public record Result(boolean error, String normal_output, String error_output,
                         List<DocumentNameAndData> generated_documents) {
    }
}
