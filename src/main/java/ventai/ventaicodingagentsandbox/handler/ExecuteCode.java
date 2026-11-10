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
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import ventai.ventaicodingagentsandbox.executors.CodeExecutionHandler;
import ventai.ventaicodingagentsandbox.executors.CodeExecutor;

@RestController
public class ExecuteCode {
    private final CodeExecutionHandler codeExecutionHandler;

    public ExecuteCode(CodeExecutionHandler codeExecutionHandler) {
        this.codeExecutionHandler = codeExecutionHandler;
    }

    @PostMapping("/execute/{department}/{user_id}")
    public ResponseEntity<CodeExecutor.Result> executeCode(
            @PathVariable("department") String department,
            @PathVariable("user_id") String user_id,
            @RequestBody CodeExecutor.Query query
    ) {
        CodeExecutor.Result result = codeExecutionHandler.executeCode(query, department, user_id);

        return ResponseEntity.ok(result);
    }
}