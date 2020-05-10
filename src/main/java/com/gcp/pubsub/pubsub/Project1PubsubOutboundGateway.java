package com.gcp.pubsub.pubsub;

import org.springframework.integration.annotation.MessagingGateway;
import org.springframework.stereotype.Service;

@Service
@MessagingGateway(defaultRequestChannel = "project1_pubsubOutputChannel")
public interface Project1PubsubOutboundGateway {

  void sendToPubsub(String text);
}
