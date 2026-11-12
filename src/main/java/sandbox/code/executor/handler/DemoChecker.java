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
import sandbox.code.executor.executors.CodeExecutionHandler;
import tools.jackson.databind.ObjectMapper;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;

@RestController
public class DemoChecker {
    private final ObjectMapper jsonParser = new ObjectMapper();
    private final String java_code1 = """
            import java.io.*;
            public class run {
                public static void main() {
                    try {
                        File inputFile = new File("input/hello.txt");
                        FileInputStream fis = new FileInputStream(inputFile);
                        String data = new String(fis.readAllBytes());
                        fis.close();
                        FileOutputStream fos = new FileOutputStream(new File("output/hello.txt"));
                        fos.write(data.getBytes());
                        fos.close();
                    } catch(Exception e) {
                        IO.println(e);
                    }
                }
            }
            """;
    private final String python_code1 = """
            print("Hello World!!!!");
            """;
    private final String document_name1 = "hello.txt";
    private final String document_data1 = """
            Hello World!!!!
            """;
    private final String HOST_URL = "http://127.0.0.1:8080/execute";
    private final Base64.Decoder decoder = Base64.getDecoder();
    private final Base64.Encoder encoder = Base64.getEncoder();
    private final HttpClient httpClient = HttpClient.newHttpClient();

    @GetMapping("/check")
    public ResponseEntity<Void> check() {
        try {
            try {
                CodeExecutionHandler.Query query = new CodeExecutionHandler.Query(
                        "java",
                        encoder.encodeToString(java_code1.getBytes(StandardCharsets.UTF_8)),
                        List.of(
                                new CodeExecutionHandler.DocumentNameAndData(
                                        document_name1,
                                        encoder.encodeToString(document_data1.getBytes(StandardCharsets.UTF_8))
                                )
                        ),
                        false
                );

                String result = sendRequest(query);
                IO.println(result);
            } catch (Exception e) {
                IO.println(e);
            }
            try {
                CodeExecutionHandler.Query query = new CodeExecutionHandler.Query(
                        "python",
                        encoder.encodeToString(python_code1.getBytes(StandardCharsets.UTF_8)),
                        List.of(),
                        false
                );

                String result = sendRequest(query);
                IO.println(result);
            } catch (Exception e) {
                IO.println(e);
            }
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    private String sendRequest(CodeExecutionHandler.Query query) throws Exception {
        String queryString = jsonParser.writeValueAsString(query);
        IO.println(queryString);
        HttpRequest httpRequest = HttpRequest.newBuilder(URI.create(HOST_URL)).header("Content-Type", "application/json").POST(HttpRequest.BodyPublishers.ofString(queryString)).build();
        HttpResponse<String> httpResponse = httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofString());
        return httpResponse.body();
    }
}
