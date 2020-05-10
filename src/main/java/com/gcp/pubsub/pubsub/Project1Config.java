package com.gcp.pubsub.pubsub;

import com.google.api.gax.core.CredentialsProvider;
import com.google.auth.Credentials;
import com.google.auth.oauth2.ServiceAccountCredentials;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
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
public class Project1Config {

  private static final Logger LOGGER = LoggerFactory.getLogger(Project1Config.class);

  @Bean(name = "project1_IdProvider")
  public GcpProjectIdProvider project1_IdProvider(@Value("${project1.id}") String projectid) {
    return () -> projectid;
  }

  @Bean(name = "project1_credentialsProvider")
  public CredentialsProvider project1_credentialsProvider(@Value("${project1.creds}") String creds) throws IOException {
    return () -> ServiceAccountCredentials.fromStream(new ClassPathResource(creds).getInputStream());
  }

  @Bean("project1_pubSubSubscriberTemplate")
  public PubSubSubscriberTemplate pubSubSubscriberTemplate(
      @Qualifier("project1_subscriberFactory") SubscriberFactory subscriberFactory) {
    return new PubSubSubscriberTemplate(subscriberFactory);
  }

  @Bean("project1_publisherFactory")
  public DefaultPublisherFactory publisherFactory(
      @Qualifier("project1_IdProvider") GcpProjectIdProvider projectIdProvider,
      @Qualifier("project1_credentialsProvider") CredentialsProvider credentialsProvider) {
    final DefaultPublisherFactory defaultPublisherFactory =
        new DefaultPublisherFactory(projectIdProvider);
    defaultPublisherFactory.setCredentialsProvider(credentialsProvider);
    return defaultPublisherFactory;
  }

  @Bean("project1_subscriberFactory")
  public DefaultSubscriberFactory subscriberFactory(
      @Qualifier("project1_IdProvider") GcpProjectIdProvider projectIdProvider,
      @Qualifier("project1_credentialsProvider") CredentialsProvider credentialsProvider) {
    final DefaultSubscriberFactory defaultSubscriberFactory =
        new DefaultSubscriberFactory(projectIdProvider);
    defaultSubscriberFactory.setCredentialsProvider(credentialsProvider);
    return defaultSubscriberFactory;
  }

  @Bean(name = "project1_pubsubInputChannel")
  public MessageChannel pubsubInputChannel() {
    return new DirectChannel();
  }

  @Bean(name = "project1_pubSubTemplate")
  public PubSubTemplate project1_PubSubTemplate(
      @Qualifier("project1_publisherFactory") PublisherFactory publisherFactory,
      @Qualifier("project1_subscriberFactory") SubscriberFactory subscriberFactory,
      @Qualifier("project1_credentialsProvider") CredentialsProvider credentialsProvider) {
    if (publisherFactory instanceof DefaultPublisherFactory) {
      ((DefaultPublisherFactory) publisherFactory).setCredentialsProvider(credentialsProvider);
    }
    return new PubSubTemplate(publisherFactory, subscriberFactory);
  }

  @Bean(name = "project1_messageChannelAdapter")
  public PubSubInboundChannelAdapter messageChannelAdapter(
      @Qualifier("project1_pubsubInputChannel") MessageChannel inputChannel,
      @Qualifier("project1_pubSubTemplate") PubSubTemplate pubSubTemplate) {

    PubSubInboundChannelAdapter adapter =
        new PubSubInboundChannelAdapter(pubSubTemplate, "YOURSUBSCRIPTIONNAME");
    adapter.setOutputChannel(inputChannel);
    adapter.setAckMode(AckMode.MANUAL);
    return adapter;
  }

  @Bean("project1_messageReceiver")
  @ServiceActivator(inputChannel = "project1_pubsubInputChannel")
  public MessageHandler messageReceiver() {
    return message -> {
      LOGGER.info("Message arrived! Payload: " + new String((byte[]) message.getPayload()));
      LOGGER.info("Message headers {}", message.getHeaders());
      BasicAcknowledgeablePubsubMessage originalMessage =
          message
              .getHeaders()
              .get(GcpPubSubHeaders.ORIGINAL_MESSAGE, BasicAcknowledgeablePubsubMessage.class);
      originalMessage.ack();
    };
  }

  @Bean("project1_messageSender")
  @ServiceActivator(inputChannel = "project1_pubsubOutputChannel")
  public MessageHandler messageSender(
      @Qualifier("project1_pubSubTemplate") PubSubTemplate pubsubTemplate) {
    return new PubSubMessageHandler(pubsubTemplate, "YOURTOPICNAME");
  }
}
