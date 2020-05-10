package com.gcp.pubsub.pubsub;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class WebAppController {

  // tag::autowireGateway[]

  @PostMapping("/publishMessage")
  public ResponseEntity<String> publishMessage(@RequestParam("message") String message) {
    return ResponseEntity.ok("OK");
  }
}
