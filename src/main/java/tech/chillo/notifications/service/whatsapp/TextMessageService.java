package tech.chillo.notifications.service.whatsapp;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestPart;
import tech.chillo.notifications.service.whatsapp.dto.TextMessage;
import tech.chillo.notifications.service.whatsapp.dto.WhatsAppResponse;

import java.util.Map;

@FeignClient(name = "whatsappmessages", url = "${providers.whatsapp.host}")
public interface TextMessageService {
    @PostMapping("/messages")
    WhatsAppResponse message(@RequestBody TextMessage textMessage);

    @PostMapping("/messages")
    void mapMessage(@RequestBody String textMessage);

    @PostMapping(value = "/media", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    Map<String, String> image(@RequestPart String file, @RequestPart String type, @RequestPart String messaging_product);

    /*
    @PostMapping(value = "/media", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    Map<String, String> image(@RequestPart Map<String, String> form);
     */
}
