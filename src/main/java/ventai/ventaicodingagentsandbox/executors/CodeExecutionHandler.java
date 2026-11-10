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

import org.springframework.stereotype.Service;

import java.nio.file.Path;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Semaphore;

@Service
public class CodeExecutionHandler {
    private static final int MAX_PARALLEL_EXECUTION = 2;
    private final Semaphore lock = new Semaphore(MAX_PARALLEL_EXECUTION, true);
    private final ConcurrentHashMap<String, CodeExecutor> codeExecutors = new ConcurrentHashMap<>();

    public void registerCodeExecutor(String language, CodeExecutor codeExecutor) {
        codeExecutors.put(language.toLowerCase(), codeExecutor);
    }

    private String sanitizeFileName(String fileName) {
        return Path.of(fileName).normalize().getFileName().toString();
    }

    private String getWorkingDir(String department, String userId) {
        return Path.of(sanitizeFileName(department), sanitizeFileName(userId)).toString();
    }

    public Set<String> getSupportedLanguages() {
        return new HashSet<>(codeExecutors.keySet());
    }

    public String getCodesFileName() {
        return CodeExecutor.CODE_FILE_NAME;
    }

    public String getCodesInputDirectory() {
        return CodeExecutor.INPUT_DIR;
    }

    public String getCodesOutputDirectory() {
        return CodeExecutor.OUTPUT_DIR;
    }

    public long getMaxRuntimeMillis() {
        return CodeExecutor.MAX_RUNTIME_MILLIS;
    }

    public long getMaxSpaceMB() {
        return CodeExecutor.MAX_SPACE_MB;
    }

    private boolean setLock() {
        try {
            lock.acquire();
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private void releaseLock() {
        lock.release();
    }

    public CodeExecutor.Result executeCode(CodeExecutor.Query query, String department, String userId) {
        if (!setLock()) {
            return null;
        }

        try {
            String language = query.code_type().toLowerCase();

            CodeExecutor codeExecutor = codeExecutors.get(language);

            if (codeExecutor == null) {
                return new CodeExecutor.Result(
                        true,
                        null,
                        "Code type not supported.",
                        null
                );
            }

            return codeExecutor.executeCode(query, getWorkingDir(department, userId));
        } catch (Exception e) {
            return new CodeExecutor.Result(true, null, e.toString(), null);
        } finally {
            releaseLock();
        }
    }
}
