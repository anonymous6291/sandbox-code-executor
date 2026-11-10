package ventai.ventaicodingagentsandbox.executors;

import org.springframework.stereotype.Service;

@Service
public class PythonCodeExecutor extends CodeExecutor {
    private static final String FILE_NAME = "run.py";
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
