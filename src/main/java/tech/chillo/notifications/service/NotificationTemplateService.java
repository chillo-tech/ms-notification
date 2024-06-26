package tech.chillo.notifications.service;

import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import tech.chillo.notifications.entity.NotificationTemplate;
import tech.chillo.notifications.repository.NotificationTemplateRepository;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import static java.lang.String.format;

@Service
@AllArgsConstructor
public class NotificationTemplateService {
    private NotificationTemplateRepository notificationTemplateRepository;

    private void checkTemplates(final String application, final Set<NotificationTemplate> templates) {
        templates.forEach(notificationTemplate -> {
            final Optional<NotificationTemplate> template = this.notificationTemplateRepository
                    .findByApplicationAndNameAndVersionAndType(
                            application,
                            notificationTemplate.getName(),
                            notificationTemplate.getVersion(),
                            notificationTemplate.getType()

                    );
            if (template.isPresent()) {
                throw new IllegalArgumentException(
                        format("un template existe déjà avec les paramètres application %s name %s version %s type %s", application,
                                notificationTemplate.getName(),
                                notificationTemplate.getVersion(),
                                notificationTemplate.getType()));
            }
        });
    }

    public void create(final String application, final Set<NotificationTemplate> templates) {
        this.checkTemplates(application, templates);
        templates.forEach(template -> template.setApplication(application));
        this.notificationTemplateRepository.saveAll(templates);
    }

    public NotificationTemplate update(final String id, final NotificationTemplate notificationTemplate) {
        final NotificationTemplate templateInBDD = this.notificationTemplateRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Aucun template n'existe avec les paramètres transmis"));

        templateInBDD.setContent(notificationTemplate.getContent());
        templateInBDD.setApplication(notificationTemplate.getApplication());
        templateInBDD.setName(notificationTemplate.getName());
        this.notificationTemplateRepository.deleteById(id);
        return this.notificationTemplateRepository.save(templateInBDD);
    }

    public List<NotificationTemplate> search() {
        return this.notificationTemplateRepository.findAll();
    }

    public void delete(final String id) {
        this.notificationTemplateRepository.deleteById(id);
    }
}
