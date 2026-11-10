package ventai.ventaicodingagentsandbox.executors;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Base64;
import java.util.List;

public abstract class CodeExecutor {

    String INPUT_DIR = "input";
    String OUTPUT_DIR = "output";
    long MAX_RUNTIME_MILLIS = 3 * 1000;
    Duration RECHECK_DELAY = Duration.ofMillis(50);
    Base64.Decoder base64Decoder = Base64.getDecoder();
    Base64.Encoder base64Encoder = Base64.getEncoder();

    abstract String getFileName();

    abstract String[] getProcessCmdArgs();

    public String getInputFileDirectory() {
        return INPUT_DIR;
    }

    public String getOutputFileDirectory() {
        return OUTPUT_DIR;
    }

    private String sanitizeFileName(String fileName) {
        return Path.of(fileName).normalize().getFileName().toString();
    }

    private String base64Encode(byte[] data) {
        return base64Encoder.encodeToString(data);
    }

    private String base64Decode(String data) {
        return new String(base64Decoder.decode(data), StandardCharsets.UTF_8);
    }

    Result executeCode(Query query, String workingDir) throws Exception {
        Path codePath = Path.of(
                workingDir,
                getFileName()
        );

        Files.writeString(codePath, base64Decode(query.base64_code()));

        if (query.documents() != null) {
            for (DocumentNameAndData documentNameAndData : query.documents()) {

                Path documentPath = Path.of(
                        workingDir,
                        INPUT_DIR,
                        sanitizeFileName(documentNameAndData.document_name())
                );

                String documentData = base64Decode(documentNameAndData.base64_data());

                Files.writeString(documentPath, documentData);
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

        List<DocumentNameAndData> generatedDocuments =
                Files
                        .list(Path.of(workingDir, OUTPUT_DIR))
                        .map(
                                x ->
                                {
                                    try {
                                        return new DocumentNameAndData(
                                                x.getFileName().toString(),
                                                base64Encode(Files.readAllBytes(x))
                                        );
                                    } catch (IOException e) {
                                        throw new RuntimeException(e);
                                    }
                                }
                        )
                        .toList();

        return new Result(
                false,
                normalOutput,
                errorOutput,
                generatedDocuments
        );
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
