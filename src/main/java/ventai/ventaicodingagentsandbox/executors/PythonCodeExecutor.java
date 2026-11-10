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

@Service
public class PythonCodeExecutor extends CodeExecutor {
    private static final String FILE_NAME = CODE_FILE_NAME + ".py";
    private final String[] commandArgs = new String[]{"python3", FILE_NAME};

    PythonCodeExecutor(CodeExecutionHandler codeExecutionHandler) {
        codeExecutionHandler.registerCodeExecutor("python", this);
    }

    @Override
    public String getFileName() {
        return FILE_NAME;
    }

    @Override
    public String[] getProcessCmdArgs() {
        return commandArgs;
    }
}
