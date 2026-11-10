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
import ventai.ventaicodingagentsandbox.executors.CodeExecutor;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;

@RestController
public class DemoChecker {
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
    private final String HOST_URL = "http://127.0.0.1:8080/execute/x1/x2";
    private final Base64.Decoder decoder = Base64.getDecoder();
    private final Base64.Encoder encoder = Base64.getEncoder();
    private final HttpClient httpClient = HttpClient.newHttpClient();

    DemoChecker() {
    }

    @GetMapping("/check")
    public ResponseEntity<Void> check() {
        try {
            try {
                CodeExecutor.Query query = new CodeExecutor.Query(
                        "java",
                        encoder.encodeToString(java_code1.getBytes(StandardCharsets.UTF_8)),
                        List.of(
                                new CodeExecutor.DocumentNameAndData(
                                        document_name1,
                                        encoder.encodeToString(document_data1.getBytes(StandardCharsets.UTF_8))
                                )
                        )
                );

                String result = sendRequest(query);
                IO.println(result);
            } catch (Exception e) {
                IO.println(e);
            }
            try {
                CodeExecutor.Query query = new CodeExecutor.Query(
                        "python",
                        encoder.encodeToString(python_code1.getBytes(StandardCharsets.UTF_8)),
                        List.of()
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

    private String sendRequest(CodeExecutor.Query query) throws Exception {
        String queryString = "{\"code_type\" : \"%s\", \"base64_code\" : \"%s\", \"documents\" : [%s]}"
                .formatted(query.code_type(), query.base64_code(), convertToDocumentsData(query.documents()));
        IO.println(queryString);
        HttpRequest httpRequest = HttpRequest.newBuilder(URI.create(HOST_URL)).header("Content-Type", "application/json").POST(HttpRequest.BodyPublishers.ofString(queryString)).build();
        HttpResponse<String> httpResponse = httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofString());
        return httpResponse.body();
    }

    private String convertToDocumentsData(List<CodeExecutor.DocumentNameAndData> documentNameAndData) {
        if (documentNameAndData.isEmpty()) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        for (CodeExecutor.DocumentNameAndData d : documentNameAndData) {
            sb.append("{\"document_name\" : \"%s\", \"base64_data\" : \"%s\"},"
                    .formatted(d.document_name(), d.base64_data()));
        }
        sb.deleteCharAt(sb.length() - 1);
        return sb.toString();
    }
}
