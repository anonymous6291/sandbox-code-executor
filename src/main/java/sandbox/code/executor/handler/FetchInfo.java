package sandbox.code.executor.handler;

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
import sandbox.code.executor.executors.CodeExecutor;

@RestController
public class FetchInfo {
    private final CodeExecutor codeExecutor;

    public FetchInfo(CodeExecutor codeExecutor) {
        this.codeExecutor = codeExecutor;
    }

    @GetMapping("/info")
    public ResponseEntity<CodeExecutor.Info> getCodeInputDirectory() {
        return ResponseEntity.ok(
                codeExecutor.getInfo()
        );
    }
}
