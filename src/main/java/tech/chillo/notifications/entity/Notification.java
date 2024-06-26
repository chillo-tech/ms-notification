package tech.chillo.notifications.entity;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import tech.chillo.notifications.enums.NotificationType;

import java.time.Instant;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Getter
@Setter
@AllArgsConstructor
@Document(collection = "NOTIFICATION")
public class Notification {
    @Id
    private String id;
    private String message;
    private String subject;
    private String eventId;
    private String applicationMessageId;
    private String application;
    private Set<NotificationType> channels;
    private String template;
    private Sender from;
    private Set<Recipient> contacts;
    private NotificationType type;
    private Set<Recipient> cc;
    private Set<Recipient> cci;
    private Map<String, List<Object>> params;
    @JsonFormat(without = JsonFormat.Feature.WRITE_DATE_TIMESTAMPS_AS_NANOSECONDS)
    private Instant creation;

    public Notification() {
        this.cc = new HashSet<>();
        this.cci = new HashSet<>();
    }

    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    public Instant getCreation() {
        return this.creation != null ? this.creation : null;
    }
}
