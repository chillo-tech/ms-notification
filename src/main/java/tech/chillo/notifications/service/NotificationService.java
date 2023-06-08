package tech.chillo.notifications.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import tech.chillo.notifications.entity.Notification;
import tech.chillo.notifications.entity.NotificationStatus;
import tech.chillo.notifications.enums.Application;
import tech.chillo.notifications.enums.NotificationType;
import tech.chillo.notifications.repository.NotificationRepository;
import tech.chillo.notifications.repository.NotificationStatusRepository;
import tech.chillo.notifications.service.mail.MailService;
import tech.chillo.notifications.service.sms.TwilioSMSService;
import tech.chillo.notifications.service.sms.VonageSMSService;
import tech.chillo.notifications.service.whatsapp.WhatsappService;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
public class NotificationService {
    private MailService mailService;
    private WhatsappService whatsappService;
    private TwilioSMSService twilioSmsService;
    private VonageSMSService vonageSMSService;
    private NotificationRepository notificationRepository;
    private NotificationStatusRepository notificationStatusRepository;

    public NotificationService(MailService mailService, WhatsappService whatsappService, TwilioSMSService twilioSmsService, VonageSMSService vonageSMSService, NotificationRepository notificationRepository, NotificationStatusRepository notificationStatusRepository) {
        this.mailService = mailService;
        this.whatsappService = whatsappService;
        this.twilioSmsService = twilioSmsService;
        this.vonageSMSService = vonageSMSService;
        this.notificationRepository = notificationRepository;
        this.notificationStatusRepository = notificationStatusRepository;
    }

    public void send(final Application application, final Notification notification, final List<NotificationType> types) {
        types.forEach(type -> {
            try {

                final List<NotificationStatus> notificationStatusList = new ArrayList<>();
                switch (type) {
                    case MAIL:
                    case EMAIL:
                        log.info("Message de type {}", type);
                        final List<NotificationStatus> mailStatusList = this.mailService.send(notification);
                        notificationStatusList.addAll(mailStatusList);
                        break;
                    case WHATSAPP:
                        log.info("Message de type {}", type);
                        final List<NotificationStatus> whatsappStatusList = this.whatsappService.send(notification);
                        notificationStatusList.addAll(whatsappStatusList);
                        break;
                    case SMS:
                        log.info("Message de type {}", type);
                        final List<NotificationStatus> smsStatusList = this.twilioSmsService.send(notification);
                        notificationStatusList.addAll(smsStatusList);
                        break;
                    default:
                        log.info("type {} inconnu", type);
                }
                notification.setType(type);
                notification.setCreation(Instant.now());
                notification.setApplication(application);
                final Notification saved = this.notificationRepository.save(notification);
                notificationStatusList.parallelStream().forEach(notificationStatus -> notificationStatus.setLocalNotificationId(saved.getId()));

                this.notificationStatusRepository.saveAll(notificationStatusList);
            } catch (Exception e) {
                log.info("ERREUR LORS DE L'ENVOI d'un message");
                log.error("ERREUR LORS DE L'ENVOI d'un message", e);
            }
        });

    }

    public List<NotificationStatus> statistics(String id) {
        return this.notificationStatusRepository.findByEventId(id);
    }
}
