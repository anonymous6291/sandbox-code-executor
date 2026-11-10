package ventai.ventaicodingagentsandbox.executors;

import org.springframework.stereotype.Service;

@Service
public class JavaCodeExecutor extends CodeExecutor {
    private static final String FILE_NAME = "run.java";
    private final String[] commandArgs = new String[]{"java", FILE_NAME};

    JavaCodeExecutor(CodeExecutionHandler codeExecutionHandler) {
        codeExecutionHandler.registerCodeExecutor("java", this);
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
