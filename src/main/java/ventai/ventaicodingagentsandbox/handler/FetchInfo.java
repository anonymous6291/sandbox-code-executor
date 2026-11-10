package ventai.ventaicodingagentsandbox.handler;

/*
 * Copyright (c) 2026 Anish Kumar Shaw
 *
 * Licensed under the MIT License.
 * See the LICENSE file in the project root for license terms.
 *
 * Original project:
 * https://github.com/anonymous6291/
 */

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import ventai.ventaicodingagentsandbox.executors.CodeExecutionHandler;

import java.util.Set;

@RestController
public class FetchInfo {
    private final CodeExecutionHandler codeExecutionHandler;

    public FetchInfo(CodeExecutionHandler codeExecutionHandler) {
        this.codeExecutionHandler = codeExecutionHandler;
    }

    @GetMapping("/info")
    public ResponseEntity<Info> getCodeInputDirectory() {
        return ResponseEntity.ok(
                new Info(
                        codeExecutionHandler.getCodesFileName(),
                        codeExecutionHandler.getCodesInputDirectory(),
                        codeExecutionHandler.getCodesOutputDirectory(),
                        codeExecutionHandler.getMaxRuntimeMillis(),
                        codeExecutionHandler.getMaxSpaceMB(),
                        codeExecutionHandler.getSupportedLanguages()
                )
        );
    }

    public record Info(
            String code_file_name,
            String input_dir,
            String output_dir,
            long max_runtime_millis,
            long max_space_mb,
            Set<String> supported_languages
    ) {
    }
}
