package com.flaviodutra.notification;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.Map;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.flaviodutra.notification.adapter.out.TwilioWhatsAppAdapter;
import com.flaviodutra.notification.domain.model.MessageToSend;
import com.twilio.http.HttpClient;
import com.twilio.http.Request;
import com.twilio.http.Response;
import com.twilio.http.TwilioRestClient;

class TwilioWhatsAppAdapterTest {
    @Test
    void sendsTemplateAndVariablesWithoutBody() throws Exception {
        var http = new CapturingHttpClient();
        var adapter = adapter(http);
        var message = new ObjectMapper().readValue("""
                {"to":"whatsapp:+5511999999999","contentSid":"HX123",
                 "contentVariables":{"1":"Jane \\\"JJ\\\"","2":"123"}}
                """, MessageToSend.class);
        adapter.send(message);
        var params = http.request.getPostParams();
        assertEquals("HX123", params.get("ContentSid").get(0));
        assertEquals("MG123", params.get("MessagingServiceSid").get(0));
        assertFalse(params.containsKey("Body"));
        assertEquals(Map.of("1", "Jane \"JJ\"", "2", "123"),
                new ObjectMapper().readValue(params.get("ContentVariables").get(0), Map.class));
    }

    @Test
    void sendsTextWithoutTemplateFields() throws Exception {
        var http = new CapturingHttpClient();
        var message = new ObjectMapper().readValue("{\"to\":\"whatsapp:+5511\",\"body\":\"Hello\"}",
                MessageToSend.class);
        adapter(http).send(message);
        var params = http.request.getPostParams();
        assertEquals("Hello", params.get("Body").get(0));
        assertFalse(params.containsKey("ContentSid"));
        assertFalse(params.containsKey("ContentVariables"));
    }

    @Test
    void sendsTemplateWithoutVariables() throws Exception {
        var http = new CapturingHttpClient();
        var message = new ObjectMapper().readValue("{\"to\":\"whatsapp:+5511\",\"contentSid\":\"HX123\"}",
                MessageToSend.class);
        adapter(http).send(message);
        assertFalse(http.request.getPostParams().containsKey("ContentVariables"));
        assertFalse(http.request.getPostParams().containsKey("Body"));
    }

    @Test
    void rejectsAmbiguousAndIncompleteMessages() {
        ObjectMapper mapper = new ObjectMapper();
        for (String json : new String[] {
                "{\"to\":\"whatsapp:+1\"}",
                "{\"to\":\"whatsapp:+1\",\"body\":\"text\",\"contentSid\":\"HX123\"}",
                "{\"to\":\"whatsapp:+1\",\"body\":\"text\",\"contentVariables\":{\"1\":\"Jane\"}}",
                "{\"to\":\"whatsapp:+1\",\"contentSid\":\"HX123\",\"contentVariables\":{\"1\":null}}" }) {
            assertThrows(com.fasterxml.jackson.databind.JsonMappingException.class,
                    () -> mapper.readValue(json, MessageToSend.class));
        }
    }

    private TwilioWhatsAppAdapter adapter(CapturingHttpClient http) {
        var client = new TwilioRestClient.Builder("AC00000000000000000000000000000000", "test").httpClient(http)
                .build();
        return new TwilioWhatsAppAdapter("MG123", client);
    }

    private static class CapturingHttpClient extends HttpClient {
        Request request;

        @Override
        public Response makeRequest(Request request) {
            this.request = request;
            return new Response("{\"sid\":\"SM123\",\"status\":\"accepted\"}", 201);
        }
    }
}
