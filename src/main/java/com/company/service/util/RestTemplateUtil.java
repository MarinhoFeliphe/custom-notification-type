package com.company.service.util;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

public class RestTemplateUtil {

    public static JsonNode get(String uri, String token) throws JsonProcessingException {
        HttpHeaders headers = new HttpHeaders();
        headers.set(HttpHeaders.AUTHORIZATION, "Bearer " + token);

        ResponseEntity<String> response = new RestTemplate().exchange(
                "https://dxp.lfr.dev/o/" + uri, HttpMethod.GET,
                new HttpEntity<String>(headers), String.class);

        return new ObjectMapper().readTree(response.getBody());
    }
}
