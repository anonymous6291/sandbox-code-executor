package ventai.ventaicodingagentsandbox.handler;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import ventai.ventaicodingagentsandbox.executors.CodeExecutor;

@RestController
public class ExecuteCode {
    @PostMapping("/execute/{department}/{user_id}")
    public ResponseEntity<String> executeCode(
            @PathVariable("department") String department,
            @PathVariable("user_id") String user_id,
            @RequestBody CodeExecutor.Query query
    ) {
        return ResponseEntity.ok("");
    }
}