package tech.chillo.notifications.records.whatsapp;

public record WhatsappChangeValueStatus(
        String id,
        String status,
        String timestamp,
        String recipient_id
) {
}
