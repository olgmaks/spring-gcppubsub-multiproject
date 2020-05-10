package com.gcp.pubsub.pubsub;

import com.google.api.gax.core.CredentialsProvider;
import com.google.auth.Credentials;
import com.google.auth.oauth2.ServiceAccountCredentials;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.cloud.gcp.autoconfigure.core.GcpContextAutoConfiguration;
import org.springframework.cloud.gcp.autoconfigure.pubsub.GcpPubSubAutoConfiguration;
import org.springframework.cloud.gcp.autoconfigure.pubsub.GcpPubSubProperties;
import org.springframework.cloud.gcp.core.DefaultGcpProjectIdProvider;
import org.springframework.cloud.gcp.core.GcpProjectIdProvider;
import org.springframework.cloud.gcp.pubsub.core.PubSubTemplate;
import org.springframework.cloud.gcp.pubsub.core.subscriber.PubSubSubscriberTemplate;
import org.springframework.cloud.gcp.pubsub.integration.AckMode;
import org.springframework.cloud.gcp.pubsub.integration.inbound.PubSubInboundChannelAdapter;
import org.springframework.cloud.gcp.pubsub.integration.outbound.PubSubMessageHandler;
import org.springframework.cloud.gcp.pubsub.support.*;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.integration.annotation.ServiceActivator;
import org.springframework.integration.channel.DirectChannel;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessageHandler;

import java.io.IOException;

@Configuration
public class Project2Config {

  private static final Logger LOGGER = LoggerFactory.getLogger(Project2Config.class);

  @Bean(name = "project2_IdProvider")
  public GcpProjectIdProvider project1_IdProvider(@Value("${project2.id}") String projectid) {
    return () -> projectid;
  }

  @Bean(name = "project2_credentialsProvider")
  public CredentialsProvider project1_credentialsProvider(@Value("${project2.creds}") String creds) throws IOException {
    return () -> ServiceAccountCredentials.fromStream(new ClassPathResource(creds).getInputStream());
  }
  @Bean("project2_pubSubSubscriberTemplate")
  public PubSubSubscriberTemplate pubSubSubscriberTemplate(
          @Qualifier("project2_subscriberFactory") SubscriberFactory subscriberFactory) {
    return new PubSubSubscriberTemplate(subscriberFactory);
  }

  @Bean("project2_publisherFactory")
  public DefaultPublisherFactory publisherFactory(
          @Qualifier("project2_IdProvider") GcpProjectIdProvider projectIdProvider,
          @Qualifier("project2_credentialsProvider") CredentialsProvider credentialsProvider) {
    final DefaultPublisherFactory defaultPublisherFactory = new DefaultPublisherFactory(projectIdProvider);
    defaultPublisherFactory.setCredentialsProvider(credentialsProvider);
    return defaultPublisherFactory;
  }

  @Bean("project2_subscriberFactory")
  public DefaultSubscriberFactory subscriberFactory(
          @Qualifier("project2_IdProvider") GcpProjectIdProvider projectIdProvider,
          @Qualifier("project2_credentialsProvider") CredentialsProvider credentialsProvider) {
    final DefaultSubscriberFactory defaultSubscriberFactory = new DefaultSubscriberFactory(projectIdProvider);
    defaultSubscriberFactory.setCredentialsProvider(credentialsProvider);
    return defaultSubscriberFactory;
  }

  @Bean(name = "project2_pubsubInputChannel")
  public MessageChannel pubsubInputChannel() {
    return new DirectChannel();
  }

  @Bean(name = "project2_pubSubTemplate")
  public PubSubTemplate project2_PubSubTemplate(
      @Qualifier("project2_publisherFactory") PublisherFactory publisherFactory,
      @Qualifier("project2_subscriberFactory") SubscriberFactory subscriberFactory,
      @Qualifier("project2_credentialsProvider") CredentialsProvider credentialsProvider) {
    if (publisherFactory instanceof DefaultPublisherFactory) {
      ((DefaultPublisherFactory) publisherFactory).setCredentialsProvider(credentialsProvider);
    }
    return new PubSubTemplate(publisherFactory, subscriberFactory);
  }

  @Bean(name = "project2_messageChannelAdapter")
  public PubSubInboundChannelAdapter messageChannelAdapter(
      @Qualifier("project2_pubsubInputChannel") MessageChannel inputChannel,
      @Qualifier("project2_pubSubTemplate") PubSubTemplate pubSubTemplate) {
    
    PubSubInboundChannelAdapter adapter =
        new PubSubInboundChannelAdapter(pubSubTemplate, "project2-testSubscription");
    adapter.setOutputChannel(inputChannel);
    adapter.setAckMode(AckMode.MANUAL);
    return adapter;
  }

  @Bean("project2_messageReceiver")
  @ServiceActivator(inputChannel = "project2_pubsubInputChannel")
  public MessageHandler messageReceiver() {
    return message -> {
      LOGGER.info("Message Payload: " + new String((byte[]) message.getPayload()));
      LOGGER.info("Message headers {}", message.getHeaders());
      BasicAcknowledgeablePubsubMessage originalMessage =
          message
              .getHeaders()
              .get(GcpPubSubHeaders.ORIGINAL_MESSAGE, BasicAcknowledgeablePubsubMessage.class);
      originalMessage.ack();
    };
  }

  @Bean(name = "project2_messageSender")
  @ServiceActivator(inputChannel = "project2_pubsubOutputChannel")
  public MessageHandler messageSender(
          @Qualifier("project2_pubSubTemplate") PubSubTemplate pubsubTemplate) {
    return new PubSubMessageHandler(pubsubTemplate, "project2-testTopic");
  }
}
