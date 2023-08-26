package tech.chillo.notifications.web;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import tech.chillo.notifications.records.whatsapp.WhatsappNotification;
import tech.chillo.notifications.service.hooks.HooksService;

import java.util.Map;

import static org.springframework.http.MediaType.APPLICATION_FORM_URLENCODED_VALUE;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

@Slf4j
@AllArgsConstructor
@RestController
@RequestMapping(path = "hooks", produces = APPLICATION_JSON_VALUE)
public class HooksController {

    private HooksService hooksService;

    @PostMapping(path = "vonage", consumes = APPLICATION_JSON_VALUE)
    public void vonage(@RequestBody Map<String, Object> params) {
        this.hooksService.vonage(params);
    }

    @PostMapping(path = "twilio", consumes = APPLICATION_FORM_URLENCODED_VALUE)
    public void twilio(@RequestBody MultiValueMap<String, Object> params) {
        this.hooksService.twilio(params);
    }

    @PostMapping(path = "whatsapp", consumes = APPLICATION_JSON_VALUE)
    public void whatsapp(@RequestBody WhatsappNotification notification) {
        this.hooksService.whatsapp(notification);
    }

    @PostMapping(path = "brevo", consumes = APPLICATION_JSON_VALUE)
    public void sendingblue(@RequestBody Map<String, Object> params) {
        this.hooksService.brevo(params);
    }

    @GetMapping(path = "whatsapp")
    public String whatsapp(
            @RequestParam(required = false, name = "hub.verify_token") String token,
            @RequestParam(required = false, name = "hub.challenge") String challenge,
            @RequestParam(required = false, name = "hub.mode") String mode) {
        log.info("WA TOKEN {}", challenge);
        return challenge;
    }
}
