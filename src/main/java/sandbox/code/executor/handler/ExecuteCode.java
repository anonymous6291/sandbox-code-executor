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
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import sandbox.code.executor.executors.CodeExecutionHandler;
import sandbox.code.executor.executors.CodeExecutor;

@RestController
public class ExecuteCode {
    private final CodeExecutor codeExecutor;

    public ExecuteCode(CodeExecutor codeExecutor) {
        this.codeExecutor = codeExecutor;
    }

    @PostMapping({"/execute", "/execute/{dir_name}"})
    public ResponseEntity<CodeExecutionHandler.Result> executeCode(
            @PathVariable(value = "dir_name", required = false) String dir_name,
            @RequestBody CodeExecutionHandler.Query query
    ) {
        CodeExecutionHandler.Result result = codeExecutor.executeCode(query, dir_name);

        return ResponseEntity.ok(result);
    }
}