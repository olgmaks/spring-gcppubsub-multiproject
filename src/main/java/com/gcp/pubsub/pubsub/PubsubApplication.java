package com.gcp.pubsub.pubsub;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.gcp.autoconfigure.pubsub.GcpPubSubAutoConfiguration;
import org.springframework.cloud.gcp.autoconfigure.pubsub.GcpPubSubProperties;
import org.springframework.cloud.gcp.autoconfigure.pubsub.GcpPubSubReactiveAutoConfiguration;
import org.springframework.cloud.gcp.pubsub.support.DefaultPublisherFactory;
import org.springframework.context.annotation.Bean;

@SpringBootApplication(exclude = {
		GcpPubSubAutoConfiguration.class,
		GcpPubSubReactiveAutoConfiguration.class
})
public class PubsubApplication {

	public static void main(String[] args) {
		SpringApplication.run(PubsubApplication.class, args);
	}

}
