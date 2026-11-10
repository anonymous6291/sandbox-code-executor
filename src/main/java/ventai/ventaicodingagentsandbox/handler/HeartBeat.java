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

@RestController
public class HeartBeat {
    @GetMapping("/")
    public ResponseEntity<String> checkHeartBeat() {
        return ResponseEntity.ok("Ok");
    }
}
