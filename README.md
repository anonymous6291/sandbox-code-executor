# Sandbox Code Executor

[![Java 27](https://img.shields.io/badge/Java-27-ED8B00?logo=openjdk&logoColor=white)](https://openjdk.org/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-4.1.2-6DB33F?logo=springboot&logoColor=white)](https://spring.io/projects/spring-boot)
[![License: MIT](https://img.shields.io/badge/License-MIT-blue.svg)](LICENSE)

**Sandbox Code Executor**  is a lightweight, extensible HTTP REST service built with Spring Boot and Java 27 designed for executing arbitrary code snippets in isolated local workspaces. It is tailored for AI coding agents, online judges, and automated evaluation systems that require dynamic code execution with input/output file handling, configurable timeouts, and concurrency throttling.

---

## Table of Contents

- [Overview](#overview)
- [Key Features](#key-features)
- [How It Works](#how-it-works)
- [Prerequisites](#prerequisites)
- [Setup & Installation](#setup--installation)
  - [1. Clone Repository](#1-clone-repository)
  - [2. Configure the Service](#2-configure-the-service)
  - [3. Build & Run](#3-build--run)
- [Configuration Reference (`configuration.json`)](#configuration-reference-configurationjson)
  - [Adding New Languages](#adding-new-languages)
- [API Reference](#api-reference)
  - [1. Heartbeat Check](#1-heartbeat-check)
  - [2. Fetch Runtime Info](#2-fetch-runtime-info)
  - [3. Execute Code](#3-execute-code)
  - [4. Demo Self-Check](#4-demo-self-check)
- [Usage Examples](#usage-examples)
  - [cURL: Simple Python Execution](#curl-simple-python-execution)
  - [cURL: Java Execution with File Input & Output](#curl-java-execution-with-file-input--output)
  - [Python Client Script](#python-client-script)
- [Security Considerations](#security-considerations)
- [License](#license)

---

## Overview

Executing code remotely often requires passing sample files, gathering output files, enforcing runtime limits, and preventing system resource exhaustion. **Sandbox Code Executor** solves these challenges by providing:
- A standardized JSON over HTTP interface for submitting Base64-encoded code and input files.
- Per-execution isolated directories to prevent cross-contamination between tasks.
- Automatic capture and Base64-encoding of output files generated in a designated folder.
- Configurable execution time limits and process termination.
- Automatic workspace cleanup upon completion.

---

## Key Features

- **Multi-Language Support**: Easily configure any language or compiler toolchain (Java, Python, C++, Node.js, Go, Rust, Bash, etc.) via `configuration.json`.
- **Bidirectional File Handling**:
  - Inject multiple input files (text or binary) via Base64 data directly into an `input/` folder before execution.
  - Automatically detect and extract all artifacts written to the `output/` folder and return them in the HTTP response.
- **Controlled Concurrency**: A fair `Semaphore` limits parallel executions to `max_parallel_execution` to protect host CPU and memory.
- **Configurable Runtime Timeouts**: Terminate hung or long-running processes forcibly (`Process.destroyForcibly()`) after `max_runtime_millis`.
- **Automatic Workspace Isolation & Cleanup**: Every execution creates a clean directory (random UUID or custom directory name) that is completely removed after execution.
- **Standard Stream Capture**: Gathers both standard output (`stdout`) and error output (`stderr`).

---

## How It Works

```
 Client (HTTP POST /execute)
           │
           ▼
 Semaphore Concurrency Guard (max_parallel_execution)
           │
           ▼
 Create Workspace Directory (<uuid> or <dir_name>)
           │
   ┌───────┴────────────────────────────┐
   │ 1. Decode & write input files      │
   │    into /<workspace>/input/        │
   │ 2. Decode & write source code      │
   │    into /<workspace>/<code_file>   │
   └───────┬────────────────────────────┘
           │
           ▼
 Spawn Process (`run_arguments` inside working directory)
           │
      ┌────┴────┐
      │ Timeout │─── Exceeded ───► process.destroyForcibly()
      └────┬────┘
           │ Completed
           ▼
 Collect stdout, stderr, and output files from /<workspace>/output/
           │
           ▼
 Delete Workspace Directory (finally block cleanup)
           │
           ▼
 Release Semaphore Lock
           │
           ▼
 Return JSON Result to Client
```

---

## Prerequisites

- **Java**: OpenJDK 27 or higher.
- **Maven**: Version 3.9+ (or use the bundled `./mvnw` wrapper).
- **Host Compilers / Interpreters**: Ensure the CLI tools defined in `configuration.json` (e.g. `java`, `python3`, `node`, `gcc`) are installed and available on your system `PATH`.

---

## Setup & Installation

### 1. Clone Repository

```bash
git clone https://github.com/anonymous6291/sandbox-code-executor.git
cd sandbox-code-executor
```

### 2. Configure the Service

Verify or edit `configuration.json` in the project root to match your system paths and desired limits (see [Configuration Reference](#configuration-reference-configurationjson)).

### 3. Build & Run

#### Using Maven Wrapper:
```bash
# Build the project
./mvnw clean package

# Run the Spring Boot application
./mvnw spring-boot:run
```

#### Running the Packaged JAR:
```bash
java -jar target/sandbox-code-executor-1.0.0.jar
```

The server starts by default on `http://0.0.0.0:8080`. You can configure the port in `src/main/resources/application.properties`:
```properties
server.address=0.0.0.0
server.port=8080
```

---

## Configuration Reference (`configuration.json`)

The server reads `configuration.json` from the current working directory upon startup.

```json
{
  "max_parallel_execution": 2,
  "max_runtime_millis": 3000,
  "input_directory": "input",
  "output_directory": "output",
  "languages_config": [
    {
      "name": "java",
      "code_file_name": "run.java",
      "run_arguments": [
        "java",
        "-Xmx300m",
        "run.java"
      ]
    },
    {
      "name": "python",
      "code_file_name": "run.py",
      "run_arguments": [
        "python3",
        "run.py"
      ]
    }
  ]
}
```

### Parameter Explanations

| Field | Type | Description |
| :--- | :--- | :--- |
| `max_parallel_execution` | `integer` | Maximum number of concurrent code execution processes allowed simultaneously. |
| `max_runtime_millis` | `long` | Maximum time allowed for process execution in milliseconds. Set to `-1` to disable timeout. |
| `input_directory` | `string` | Name of the subfolder created inside the execution workspace where submitted documents are placed. Set to `null` or `""` to disable. |
| `output_directory` | `string` | Name of the subfolder checked for generated output files after execution. Set to `null` or `""` to ignore output files. |
| `languages_config` | `array` | List of supported programming language configurations. |
| `languages_config[].name` | `string` | Case-insensitive language identifier (matches `code_type` in execution requests). |
| `languages_config[].code_file_name` | `string` | Filename to which the source code will be written inside the working directory. |
| `languages_config[].run_arguments` | `array of string` | Command-line arguments used to execute the code. **Note**: The source code file name must be explicitly included in this array if required by the command. |

### Adding New Languages

To add a new language, append an entry to `languages_config`:

```json
{
  "name": "javascript",
  "code_file_name": "index.js",
  "run_arguments": ["node", "index.js"]
},
{
  "name": "cpp",
  "code_file_name": "main.cpp",
  "run_arguments": ["sh", "-c", "g++ main.cpp -O2 -o run && ./run"]
}
```

---

## API Reference

### 1. Heartbeat Check

Check service health and connectivity.

- **URL**: `/`
- **Method**: `GET`
- **Response**: `200 OK`
- **Content-Type**: `text/plain`

```
Ok
```

---

### 2. Fetch Runtime Info

Retrieve current runtime constraints and supported languages configured on the server.

- **URL**: `/info`
- **Method**: `GET`
- **Response**: `200 OK`
- **Content-Type**: `application/json`

#### Response Example:
```json
{
  "max_runtime_millis": 3000,
  "input_dir": "input",
  "output_dir": "output",
  "languages": [
    {
      "name": "java",
      "code_file_name": "run.java"
    },
    {
      "name": "python",
      "code_file_name": "run.py"
    }
  ]
}
```

---

### 3. Execute Code

Executes the provided code snippet within an isolated directory.

- **URL**: 
  - `/execute` (creates a random UUID directory)
  - `/execute/{dir_name}` (uses a sanitized custom directory name)
- **Method**: `POST`
- **Headers**: `Content-Type: application/json`

#### Request Body (`Query`)

| Field | Type | Required | Description |
| :--- | :--- | :--- | :--- |
| `code_type` | `string` | **Yes** | Language key matching an entry in `configuration.json` (e.g. `"python"`, `"java"`). |
| `base64_code` | `string` | **Yes** | Base64-encoded source code string. |
| `ignore_output` | `boolean` | No | If `true`, ignores output file scanning and output directory creation. Default is `false`. |
| `documents` | `array` | No | List of files to place in the `input_directory` before running the code. |
| `documents[].document_name` | `string` | **Yes** | Filename of the input file (path traversal sanitized). |
| `documents[].base64_data` | `string` | **Yes** | Base64-encoded file content. |

#### Response Body (`Result`)

| Field | Type | Description |
| :--- | :--- | :--- |
| `error` | `boolean` | `true` if an internal exception or unsupported language occurred, otherwise `false`. |
| `normal_output` | `string` | Standard output (`stdout`) produced by the process. |
| `error_output` | `string` | Standard error (`stderr`) produced by the process. |
| `generated_documents` | `array` | List of generated files discovered in the `output_directory`. |
| `generated_documents[].document_name` | `string` | Filename of the generated document. |
| `generated_documents[].base64_data` | `string` | Base64-encoded content of the output file. |

#### Request Example:
```json
{
  "code_type": "python",
  "base64_code": "cHJpbnQoIkhlbGxvIGZyb20gU2FuZGJveCEiKQ==",
  "ignore_output": false,
  "documents": []
}
```

#### Response Example:
```json
{
  "error": false,
  "normal_output": "Hello from Sandbox!\n",
  "error_output": "",
  "generated_documents": []
}
```

---

### 4. Demo Self-Check

Runs an internal smoke test testing both Java (with input/output files) and Python executions against `http://127.0.0.1:8080/execute`.

- **URL**: `/check`
- **Method**: `GET`
- **Response**: `200 OK` (logs results to standard console output)

---

## Usage Examples

### cURL: Simple Python Execution

```bash
# Code: print("Hello from Sandbox!")
# Base64: cHJpbnQoIkhlbGxvIGZyb20gU2FuZGJveCEiKQ==

curl -X POST http://localhost:8080/execute \
  -H "Content-Type: application/json" \
  -d '{
    "code_type": "python",
    "base64_code": "cHJpbnQoIkhlbGxvIGZyb20gU2FuZGJveCEiKQ==",
    "ignore_output": true,
    "documents": []
  }'
```

---

### cURL: Java Execution with File Input & Output

In this example, the Java program reads `input/message.txt` and writes an uppercase version to `output/response.txt`.

**Java Source Code:**
```java
import java.io.*;
import java.nio.file.*;

public class run {
    public static void main(String[] args) throws Exception {
        String input = Files.readString(Path.of("input/message.txt"));
        System.out.println("Processing: " + input);
        Files.writeString(Path.of("output/response.txt"), input.toUpperCase());
    }
}
```

**cURL Request:**
```bash
curl -X POST http://localhost:8080/execute \
  -H "Content-Type: application/json" \
  -d '{
    "code_type": "java",
    "base64_code": "aW1wb3J0IGphdmEuaW8uKjsKaW1wb3J0IGphdmEubmlvLmZpbGUuKjsKCnB1YmxpYyBjbGFzcyBydW4gewogICAgcHVibGljIHN0YXRpYyB2b2lkIG1haW4oU3RyaW5nW10gYXJncykgdGhyb3dzIEV4Y2VwdGlvbiB7CiAgICAgICAgU3RyaW5nIGlucHV0ID0gRmlsZXMucmVhZFN0cmluZyhQYXRoLm9mKCJpbnB1dC9tZXNzYWdlLnR4dCIpKTsKICAgICAgICBTeXN0ZW0ub3V0LnByaW50bG4oIlByb2Nlc3Npbmc6ICIgKyBpbnB1dCk7CiAgICAgICAgRmlsZXMud3JpdGVTdHJpbmcoUGF0aC5vZigib3V0cHV0L3Jlc3BvbnNlLnR4dCIpLCBpbnB1dC50b1VwcGVyQ2FzZSgpKTsKICAgIH0KfQ==",
    "ignore_output": false,
    "documents": [
      {
        "document_name": "message.txt",
        "base64_data": "aGVsbG8gd29ybGQ="
      }
    ]
  }'
```

**Sample Response:**
```json
{
  "error": false,
  "normal_output": "Processing: hello world\n",
  "error_output": "",
  "generated_documents": [
    {
      "document_name": "response.txt",
      "base64_data": "SEVMTE8gV09STEQ="
    }
  ]
}
```

---

### Python Client Script

Here is a ready-to-use Python helper function to interact with the sandbox:

```python
import base64
import requests

SERVER_URL = "http://localhost:8080/execute"

def execute_code(language: str, code: str, input_files: dict = None, ignore_output: bool = False):
    encoded_code = base64.b64encode(code.encode("utf-8")).decode("utf-8")
    
    documents = []
    if input_files:
        for filename, content in input_files.items():
            if isinstance(content, str):
                content = content.encode("utf-8")
            documents.append({
                "document_name": filename,
                "base64_data": base64.b64encode(content).decode("utf-8")
            })

    payload = {
        "code_type": language,
        "base64_code": encoded_code,
        "ignore_output": ignore_output,
        "documents": documents
    }

    response = requests.post(SERVER_URL, json=payload)
    response.raise_for_status()
    result = response.json()

    # Decode any generated output documents
    output_files = {}
    for doc in result.get("generated_documents", []):
        name = doc["document_name"]
        data = base64.b64decode(doc["base64_data"])
        output_files[name] = data

    return {
        "error": result.get("error"),
        "stdout": result.get("normal_output"),
        "stderr": result.get("error_output"),
        "output_files": output_files
    }

# Example usage:
if __name__ == "__main__":
    py_code = """
with open("input/data.txt") as f:
    text = f.read()

print(f"Read input: {text}")

with open("output/result.txt", "w") as f:
    f.write(f"Processed: {text.strip().upper()}")
"""
    res = execute_code(
        language="python",
        code=py_code,
        input_files={"data.txt": "hello from python client"}
    )
    print("STDOUT:", res["stdout"])
    print("OUTPUT FILES:", {k: v.decode() for k, v in res["output_files"].items()})
```

---

## Security Considerations

1. **Host Process Execution**: Code is launched as a subprocess on the host OS via `ProcessBuilder`. While file paths and directory names are normalized using `Path.normalize().getFileName()` to prevent directory traversal, the executed code inherits host permissions.
2. **Containerization for Untrusted Code**: When executing untrusted code in production or multi-tenant environments, run this service inside:
   - A Docker / Podman container with dropped capabilities (`--cap-drop=ALL`), limited memory/CPU, and read-only root filesystem.
   - Sandbox environments such as **gVisor** (`runsc`), **nsjail**, or **Firejail**.
3. **Resource Quotas**:
   - Keep `max_runtime_millis` strictly enforced to kill infinite loops or fork bombs.
   - Enforce memory caps via compiler/interpreter flags (e.g. `-Xmx300m` for Java).
   - Adjust `max_parallel_execution` according to your available CPU cores and memory limits.

---

## License

This project is licensed under the [MIT License](LICENSE).  
Copyright (c) 2026 Anish Kumar Shaw.
