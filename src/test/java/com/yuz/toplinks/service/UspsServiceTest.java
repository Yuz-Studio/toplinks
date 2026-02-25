package com.yuz.toplinks.service;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

import com.yuz.toplinks.dto.UspsRateRequest;
import com.yuz.toplinks.dto.UspsRateResponse;

/**
 * Unit tests for {@link UspsService}.
 * These tests verify request building, response parsing, and disabled-state
 * behavior without making real HTTP calls.
 */
class UspsServiceTest {

    @Test
    void calculateRate_whenNotConfigured_returnsError() {
        UspsService service = new UspsService("https://api.usps.com", "", "");
        assertFalse(service.isEnabled());

        UspsRateRequest request = new UspsRateRequest();
        request.setOriginZIPCode("20260");
        request.setDestinationZIPCode("10001");
        request.setWeight(2.5);

        UspsRateResponse response = service.calculateRate(request);
        assertFalse(response.isSuccess());
        assertNotNull(response.getMessage());
        assertTrue(response.getMessage().contains("not configured"));
    }

    @Test
    void isEnabled_whenCredentialsProvided_returnsTrue() {
        UspsService service = new UspsService("https://api.usps.com", "key", "secret");
        assertTrue(service.isEnabled());
    }

    @Test
    void isEnabled_whenCredentialsEmpty_returnsFalse() {
        UspsService service = new UspsService("https://api.usps.com", "", "");
        assertFalse(service.isEnabled());
    }

    @Test
    void buildRateRequestBody_containsRequiredFields() throws Exception {
        UspsService service = new UspsService("https://api.usps.com", "key", "secret");

        UspsRateRequest request = new UspsRateRequest();
        request.setOriginZIPCode("20260");
        request.setDestinationZIPCode("10001");
        request.setWeight(3.0);
        request.setMailClass("PRIORITY_MAIL");
        request.setLength(10);
        request.setWidth(8);
        request.setHeight(4);

        String body = service.buildRateRequestBody(request);
        assertTrue(body.contains("\"originZIPCode\":\"20260\""));
        assertTrue(body.contains("\"destinationZIPCode\":\"10001\""));
        assertTrue(body.contains("\"weight\":3.0"));
        assertTrue(body.contains("\"mailClass\":\"PRIORITY_MAIL\""));
        assertTrue(body.contains("\"length\":10.0"));
        assertTrue(body.contains("\"width\":8.0"));
        assertTrue(body.contains("\"height\":4.0"));
    }

    @Test
    void buildRateRequestBody_usesDefaultsWhenFieldsBlank() throws Exception {
        UspsService service = new UspsService("https://api.usps.com", "key", "secret");

        UspsRateRequest request = new UspsRateRequest();
        request.setOriginZIPCode("90210");
        request.setDestinationZIPCode("30301");
        request.setWeight(1.0);

        String body = service.buildRateRequestBody(request);
        assertTrue(body.contains("\"mailClass\":\"ALL\""));
        assertTrue(body.contains("\"processingCategory\":\"PARCELS\""));
        assertTrue(body.contains("\"rateIndicator\":\"SP\""));
        assertTrue(body.contains("\"priceType\":\"RETAIL\""));
    }

    @Test
    void parseRateResponse_parsesMultipleRates() throws Exception {
        UspsService service = new UspsService("https://api.usps.com", "key", "secret");

        String json = """
                {
                  "rates": [
                    {
                      "mailClass": "PRIORITY_MAIL",
                      "productName": "Priority Mail",
                      "totalPrice": 8.95,
                      "deliveryDays": "2",
                      "zone": "04"
                    },
                    {
                      "mailClass": "USPS_GROUND_ADVANTAGE",
                      "productName": "USPS Ground Advantage",
                      "totalPrice": 5.50,
                      "deliveryDays": "5",
                      "zone": "04"
                    }
                  ]
                }
                """;

        UspsRateResponse response = service.parseRateResponse(json);
        assertTrue(response.isSuccess());
        assertEquals(2, response.getRates().size());
        assertEquals("PRIORITY_MAIL", response.getRates().get(0).getMailClass());
        assertEquals(8.95, response.getRates().get(0).getTotalPrice());
        assertEquals("USPS_GROUND_ADVANTAGE", response.getRates().get(1).getMailClass());
        assertEquals(5.50, response.getRates().get(1).getTotalPrice());
    }

    @Test
    void parseRateResponse_parsesSingleRate() throws Exception {
        UspsService service = new UspsService("https://api.usps.com", "key", "secret");

        String json = """
                {
                  "mailClass": "PRIORITY_MAIL",
                  "productName": "Priority Mail",
                  "totalPrice": 12.30,
                  "deliveryDays": "2",
                  "zone": "06"
                }
                """;

        UspsRateResponse response = service.parseRateResponse(json);
        assertTrue(response.isSuccess());
        assertEquals(1, response.getRates().size());
        assertEquals(12.30, response.getRates().get(0).getTotalPrice());
    }
}
