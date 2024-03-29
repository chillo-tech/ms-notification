package tech.chillo.notifications.service.mail;

import lombok.extern.slf4j.Slf4j;
import org.commonmark.node.Node;
import org.commonmark.parser.Parser;
import org.commonmark.renderer.html.HtmlRenderer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import tech.chillo.notifications.entity.Notification;
import tech.chillo.notifications.entity.NotificationStatus;
import tech.chillo.notifications.entity.Recipient;
import tech.chillo.notifications.records.brevo.Contact;
import tech.chillo.notifications.records.brevo.Message;
import tech.chillo.notifications.repository.NotificationTemplateRepository;
import tech.chillo.notifications.service.NotificationMapper;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static java.lang.String.format;
import static tech.chillo.notifications.data.ApplicationData.FOOTER_TEXT;
import static tech.chillo.notifications.enums.NotificationType.EMAIL;
import static tech.chillo.notifications.enums.NotificationType.MAIL;

@Slf4j
@Service
public class MailService extends NotificationMapper {
    private final String recipient;
    private final SendinblueMessageService brevoMessageService;

    public MailService(
            final NotificationTemplateRepository notificationTemplateRepository,
            final SendinblueMessageService brevoMessageService,
            @Value("${application.recipient.email:#{null}}") final String recipient) {
        super(notificationTemplateRepository);
        this.brevoMessageService = brevoMessageService;
        this.recipient = recipient;
    }

    @Async
    public List<NotificationStatus> send(final Notification notification) {
        return notification.getContacts().stream().map((Recipient to) -> {
            final NotificationStatus notificationStatus = this.getNotificationStatus(
                    notification,
                    to.getId(),
                    MAIL,
                    "", //result.get("messageId").toString(), // TODO decommenter
                    "INITIAL"
            );
            notificationStatus.setProvider("BREVO");
            try {
                final String messageToSend = String.valueOf(this.map(notification, to, EMAIL).get("message"));
                final Map<String, Object> result = this.sendMessageUsingSendinBlueAPI(notification, messageToSend, to);
                notificationStatus.setProviderNotificationId(result.get("messageId").toString());
                return notificationStatus;
            } catch (final Exception e) {
                //e.printStackTrace();
                log.error("ERROR {}", e.getMessage());
                notificationStatus.setStatus("ERROR");
            }
            return notificationStatus;
        }).collect(Collectors.toList());
    }


    private Map<String, Object> sendMessageUsingSendinBlueAPI(final Notification notification, final String messageToSend, final Recipient to) {
        final Parser parser = Parser.builder().build();
        final Node document = parser.parse(String.format("%s<p>%s</p>", messageToSend.replaceAll("\\n", "<br />"), FOOTER_TEXT));
        final HtmlRenderer renderer = HtmlRenderer.builder().build();

        String lastName = notification.getFrom().getLastName();
        if (lastName != null) {
            lastName = lastName.toUpperCase();
        }

        String firstName = notification.getFrom().getFirstName();
        if (firstName != null) {
            firstName = format("%s%s", firstName.substring(0, 1).toUpperCase(), firstName.substring(1).toLowerCase());
        }

        final Message message = new Message(
                notification.getSubject(),
                renderer.render(document),
                new Contact(format("%s %s VIA ZEEVEN", firstName, lastName), notification.getFrom().getEmail()),
                this.mappedContacts(Set.of(to))
        );
        return this.brevoMessageService.message(message);
    }

    private Set<Contact> mappedContacts(final Set<Recipient> recipients) {

        return recipients.stream().map(
                        (Recipient to) -> {
                            String email = this.recipient;
                            if (this.recipient == null) {
                                email = to.getEmail();
                            }
                            return new Contact(format("%s %s", to.getFirstName(), to.getLastName()), email);
                        })
                .collect(Collectors.toSet());
    }

}
