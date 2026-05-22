package com.kiero.global.notification.adapter.out.sqs;

import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kiero.global.notification.application.port.out.NotificationPublisherPort;
import com.kiero.global.notification.domain.PushNotificationPayload;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.SendMessageRequest;

@Slf4j
@Component
@RequiredArgsConstructor
public class SqsNotificationPublisherAdapter implements NotificationPublisherPort {

	private final SqsClient sqsClient;
	private final ObjectMapper objectMapper;

	@Value("${aws.sqs.notification-queue-url:}")
	private String queueUrl;

	@Override
	public void publish(PushNotificationPayload payload) {
		if (queueUrl == null || queueUrl.isBlank()) {
			log.warn("SQS 큐 URL 미설정으로 푸시 알림 스킵: type={}", payload.type());
			return;
		}

		try {
			String messageBody = objectMapper.writeValueAsString(Map.of(
				"fcmToken", payload.fcmToken(),
				"title", payload.title(),
				"body", payload.body(),
				"data", Map.of(
					"type", payload.type().name(),
					"targetId", payload.targetId()
				)
			));

			log.debug("SQS 메시지 발행: {}", messageBody);

			sqsClient.sendMessage(SendMessageRequest.builder()
				.queueUrl(queueUrl)
				.messageBody(messageBody)
				.build());
		} catch (JsonProcessingException e) {
			log.error("SQS 메시지 직렬화 실패: type={}", payload.type(), e);
		}
	}
}
