package com.evcharge.libsdk.wechat;

import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.Map;

public class HttpClientForWechat {

    private final RestTemplate restTemplate;

    public HttpClientForWechat(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }


    public String sendGet(String url){
        return sendGet(url,null);
    }

    public String sendGet(String url, Map<String, String> params) {
        UriComponentsBuilder builder = UriComponentsBuilder.fromHttpUrl(url);
        if(params!=null){
            params.forEach(builder::queryParam);
        }

        ResponseEntity<String> response = restTemplate.exchange(
                builder.toUriString(),
                HttpMethod.GET,
                null,
                String.class
        );

        return response.getBody();
    }

    public String sendPost(String url, Object body, MediaType contentType) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(contentType);
        HttpEntity<Object> entity = new HttpEntity<>(body, headers);

        ResponseEntity<String> response = restTemplate.exchange(
                url,
                HttpMethod.POST,
                entity,
                String.class
        );

        return response.getBody();
    }


}
