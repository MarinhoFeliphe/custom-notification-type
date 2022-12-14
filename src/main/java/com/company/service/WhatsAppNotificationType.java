package com.company.service;

import com.company.service.util.RestTemplateUtil;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.twilio.Twilio;
import com.twilio.rest.api.v2010.account.Message;
import com.twilio.type.PhoneNumber;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

@RestController
public class WhatsAppNotificationType {

  @PostMapping(
      consumes = MediaType.APPLICATION_JSON_VALUE,
      produces = MediaType.APPLICATION_JSON_VALUE,
      value = "/whatsapp/send")
  public ResponseEntity<String> create(
      @AuthenticationPrincipal Jwt jwt,
      @RequestBody String json)
    throws JsonMappingException, JsonProcessingException {

    Twilio.init(_twilioAccountSID, _twilioAuthToken);

    Message message = Message.creator(
            _getCustomerPhoneNumber(json, jwt.getTokenValue()),
            new PhoneNumber("whatsapp:+" + _twilioPhoneNumber),
            _getBody(json, jwt.getTokenValue())
      ).create();

    return new ResponseEntity<>(message.toString(), HttpStatus.CREATED);
  }

  private String _getBody(String json, String token) throws JsonProcessingException {
    JsonNode notificationContextJsonNode = new ObjectMapper().readTree(json);
    JsonNode termValuesJsonNode = notificationContextJsonNode.get("termValues");
    JsonNode orderJsonNode = termValuesJsonNode.get("commerceOrder");

    String body = notificationContextJsonNode.get(
        "notificationTemplate"
    ).get(
        "bodyCurrentValue"
    ).asText();

    JsonNode order = RestTemplateUtil.get(
            "headless-commerce-admin-order/v1.0/orders/" + orderJsonNode.get("id").asText(), token);
    JsonNode orderStatusInfoJsonNode = order.get("orderStatusInfo");

    return body
            .replaceAll("__customerName__", "Feliphe Marinho")
            .replaceAll("__id__", orderJsonNode.get("id").asText())
            .replaceAll("__status__", orderStatusInfoJsonNode.get("label_i18n").asText());
  }

  private PhoneNumber _getCustomerPhoneNumber(String json, String token) throws JsonProcessingException {
    JsonNode notificationContextJsonNode = new ObjectMapper().readTree(json);
    JsonNode termValuesJsonNode = notificationContextJsonNode.get("termValues");
    JsonNode orderJsonNode = termValuesJsonNode.get("commerceOrder");

    JsonNode billingAddressJsonNode = RestTemplateUtil.get(
            "headless-commerce-admin-order/v1.0/orders/" + orderJsonNode.get("id").asText() + "/billingAddress", token);

    return new PhoneNumber("whatsapp:+" + billingAddressJsonNode.get("phoneNumber").asText());
  }

  @Value("${twilio.account.sid}")
  private String _twilioAccountSID;

  @Value("${twilio.auth.token}")
  private String _twilioAuthToken;

  @Value("${twilio.phone.number}")
  private String _twilioPhoneNumber;

}
