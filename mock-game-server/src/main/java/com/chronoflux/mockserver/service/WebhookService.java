package com.chronoflux.mockserver.service;

import com.chronoflux.mockserver.dto.WebhookPayloadDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class WebhookService {
    private static final Logger logger = LoggerFactory.getLogger(WebhookService.class);

    public void processWebhook(WebhookPayloadDto payload) {
        logger.info("\n=======================================================" +
                    "\n[MOCK GAME SERVER] Processed Webhook via Service Layer:" +
                    "\n=======================================================" +
                    "\nPayload: {}" +
                    "\n=======================================================", payload);
    }
}
