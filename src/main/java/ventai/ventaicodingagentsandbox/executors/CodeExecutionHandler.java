package ventai.ventaicodingagentsandbox.executors;

import org.springframework.stereotype.Service;

import java.nio.file.Path;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.Semaphore;

@Service
public class CodeExecutionHandler {
    private static final int MAX_PARALLEL_EXECUTION = 2;
    private final Executor executor = Executors.newVirtualThreadPerTaskExecutor();
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

    public CodeExecutor.Result executeCode(CodeExecutor.Query query, String department, String userId) {
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
        }
    }
}
