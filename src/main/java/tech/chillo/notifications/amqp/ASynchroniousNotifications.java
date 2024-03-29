package tech.chillo.notifications.amqp;

import com.google.gson.Gson;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import tech.chillo.notifications.entity.NotificationStatus;


@Service
@Slf4j
public class ASynchroniousNotifications {
    private final RabbitTemplate rabbitTemplate;
    private final String applicationMessagesStatusExchange;

    public ASynchroniousNotifications(
            final RabbitTemplate rabbitTemplate,
            @Value("${application.messages.status.exchange}") final String applicationMessagesStatusExchange
    ) {
        this.rabbitTemplate = rabbitTemplate;
        this.applicationMessagesStatusExchange = applicationMessagesStatusExchange;
    }

    public void sendMessageStatus(final NotificationStatus notificationStatus) {
        final MessageProperties messageProperties = new MessageProperties();
        messageProperties.getHeaders().put("object", "status");
        messageProperties.getHeaders().put("type", "message");
        final String jsonString = (new Gson()).toJson(notificationStatus);
        this.rabbitTemplate.setExchange(this.applicationMessagesStatusExchange);
        this.rabbitTemplate.convertAndSend(new Message(jsonString.getBytes(), messageProperties));
    }
}
