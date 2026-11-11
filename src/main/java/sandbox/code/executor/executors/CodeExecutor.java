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

import org.springframework.stereotype.Service;
import tools.jackson.databind.ObjectMapper;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Semaphore;

@Service
public class CodeExecutor {
    private static final String CONFIG_FILE_NAME = "configuration.json";
    private static final ObjectMapper jsonParser = new ObjectMapper();
    private final Config config;
    private final Semaphore lock;
    private final Map<String, LanguageConfig> languagesConfigurations;

    public CodeExecutor() throws Exception {
        String configString = Files.readString(Path.of(CONFIG_FILE_NAME));

        config = jsonParser.readValue(configString, Config.class);

        lock = new Semaphore(config.max_parallel_execution, true);

        languagesConfigurations = new HashMap<>();

        config
                .languages_config()
                .forEach(
                        languageConfig ->
                                languagesConfigurations.put(
                                        languageConfig.name.toLowerCase(),
                                        languageConfig
                                )
                );
    }

    public Info getInfo() {
        List<LanguageInfo> languageInfos =
                config
                        .languages_config()
                        .stream()
                        .map(
                                languageConfig ->
                                        new LanguageInfo(
                                                languageConfig.name(),
                                                languageConfig.code_file_name()
                                        )
                        )
                        .toList();

        return new Info(
                config.max_runtime_millis(),
                config.input_directory(),
                config.output_directory,
                languageInfos
        );
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

    public CodeExecutionHandler.Result executeCode(CodeExecutionHandler.Query query, String directory) {
        if (!setLock()) {
            return null;
        }

        try {
            String language = query.code_type().toLowerCase();

            LanguageConfig languageConfig = languagesConfigurations.get(language);

            CodeExecutionHandler codeExecutionHandler =
                    CodeExecutionHandler
                            .newBuilder(languageConfig.code_file_name(), languageConfig.run_arguments())
                            .setLanguageName(languageConfig.name())
                            .setMaxRuntimeMillis(config.max_runtime_millis())
                            .setInputDirectory(config.input_directory())
                            .setOutputDirectory(config.output_directory())
                            .build();

            if (codeExecutionHandler == null) {
                return new CodeExecutionHandler.Result(
                        true,
                        null,
                        "Code type not supported.",
                        null
                );
            }

            return codeExecutionHandler.executeCode(query, directory);
        } catch (Exception e) {
            return new CodeExecutionHandler.Result(true, null, e.toString(), null);
        } finally {
            releaseLock();
        }
    }

    public record Config(int max_parallel_execution, long max_runtime_millis, String input_directory,
                         String output_directory,
                         List<LanguageConfig> languages_config) {
    }

    public record LanguageConfig(String name, String code_file_name, String[] run_arguments) {
    }

    public record Info(
            long max_runtime_millis,
            String input_dir,
            String output_dir,
            List<LanguageInfo> languages
    ) {
    }

    public record LanguageInfo(String name, String code_file_name) {
    }
}
