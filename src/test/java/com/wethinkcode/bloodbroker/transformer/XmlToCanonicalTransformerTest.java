package com.wethinkcode.bloodbroker.transformer;

import com.wethinkcode.bloodbroker.domain.HospitalRequest;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class XmlToCanonicalTransformerTest {

    private final XmlToCanonicalTransformer transformer = new XmlToCanonicalTransformer();

    // ── Happy path ────────────────────────────────────────────────────────────

    @Test
    void testTransformValidXml() {
        String xml = "<hospitalRequest>" +
                "<bloodType>O-Negative</bloodType>" +
                "<unitsRequired>5</unitsRequired>" +
                "<hospitalId>HOSP-001</hospitalId>" +
                "</hospitalRequest>";

        HospitalRequest request = transformer.transform(xml);

        assertEquals("O-Negative", request.getBloodType());
        assertEquals(5, request.getUnitsRequired());
        assertEquals("HOSP-001", request.getHospitalId());
        assertNull(request.getTimestamp(), "Transformer must NOT set timestamp — that is the enricher's job");
    }

    @Test
    void testTransformTrimsWhitespace() {
        // Values padded with spaces / newlines — getTagValue must trim them
        String xml = "<hospitalRequest>" +
                "<bloodType>  A-Positive  </bloodType>" +
                "<unitsRequired>  3  </unitsRequired>" +
                "<hospitalId>  HOSP-999  </hospitalId>" +
                "</hospitalRequest>";

        HospitalRequest request = transformer.transform(xml);

        assertEquals("A-Positive", request.getBloodType());
        assertEquals(3, request.getUnitsRequired());
        assertEquals("HOSP-999", request.getHospitalId());
    }

    // ── Error paths (cover the two catch/throw branches) ─────────────────────

    @Test
    void testTransformThrowsOnMalformedXml() {
        // Completely broken XML — parseXml catch block
        String badXml = "this is not xml at all";

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> transformer.transform(badXml));

        assertTrue(ex.getMessage().contains("Failed to parse legacy XML payload"),
                "Expected parse-failure message, got: " + ex.getMessage());
    }

    @Test
    void testTransformThrowsOnMissingTag() {
        // Valid XML but <unitsRequired> tag is absent — getTagValue throw
        String xml = "<hospitalRequest>" +
                "<bloodType>B-Positive</bloodType>" +
                "<hospitalId>HOSP-002</hospitalId>" +
                "</hospitalRequest>";

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> transformer.transform(xml));

        assertTrue(ex.getMessage().contains("unitsRequired"),
                "Expected missing-tag message, got: " + ex.getMessage());
    }

    @Test
    void testTransformThrowsOnNonIntegerUnits() {
        // Tag present but value is not parseable as int — NumberFormatException branch
        String xml = "<hospitalRequest>" +
                "<bloodType>AB-Positive</bloodType>" +
                "<unitsRequired>not-a-number</unitsRequired>" +
                "<hospitalId>HOSP-003</hospitalId>" +
                "</hospitalRequest>";

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> transformer.transform(xml));

        assertTrue(ex.getMessage().contains("Invalid <unitsRequired>"),
                "Expected integer-parse-failure message, got: " + ex.getMessage());
    }
}
