package com.wethinkcode.bloodbroker.transformer;

import com.wethinkcode.bloodbroker.domain.HospitalRequest;
import org.springframework.stereotype.Component;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;

/**
 * Step 2a — Data Translation (Transformer)
 *
 * Responsibility: Convert a legacy XML string from the hospital system into a
 * canonical {@link HospitalRequest} domain object.
 *
 * NOTE: The transformer does NOT set a timestamp — that is the job of
 * {@link PayloadEnricher} (the next step in the pipeline). This keeps each
 * component focused on a single responsibility (SRP).
 *
 * Example input:
 *   <hospitalRequest>
 *       <bloodType>O-Negative</bloodType>
 *       <unitsRequired>3</unitsRequired>
 *       <hospitalId>HOSP-007</hospitalId>
 *   </hospitalRequest>
 */
@Component
public class XmlToCanonicalTransformer {

    /**
     * Parses the incoming legacy XML string and maps its fields onto a
     * {@link HospitalRequest} object.
     *
     * @param legacyXml the raw XML string received from the legacy hospital system
     * @return a populated {@link HospitalRequest} (timestamp left null for the enricher)
     * @throws RuntimeException if the XML is malformed or a required tag is missing
     */
    public HospitalRequest transform(String legacyXml) {

        // 1. Parse the raw XML string into a DOM Document
        Document document = parseXml(legacyXml);

        // 2. Get the root element and normalise whitespace text nodes
        Element root = document.getDocumentElement();
        root.normalize();

        // 3. Extract each required field from its child element
        String bloodType        = getTagValue("bloodType",      root);
        String unitsRequiredStr = getTagValue("unitsRequired",  root);
        String hospitalId       = getTagValue("hospitalId",     root);

        // 4. Type-convert unitsRequired (XML is always text, domain wants int)
        int unitsRequired;
        try {
            unitsRequired = Integer.parseInt(unitsRequiredStr.trim());
        } catch (NumberFormatException e) {
            throw new RuntimeException(
                "Invalid <unitsRequired> value — expected an integer, got: '"
                + unitsRequiredStr + "'", e);
        }

        // 5. Build and return the canonical domain object.
        //    Timestamp is intentionally NOT set here — PayloadEnricher handles that.
        HospitalRequest request = new HospitalRequest();
        request.setBloodType(bloodType);
        request.setUnitsRequired(unitsRequired);
        request.setHospitalId(hospitalId);

        return request;
    }

    // ── Private helpers ───────────────────────────────────────────────────────

    /**
     * Converts an XML string into an in-memory DOM {@link Document}.
     * Uses Java's built-in parser — no extra library needed.
     * The DTD feature is disabled to prevent XXE (XML External Entity) attacks.
     */
    private Document parseXml(String xml) {
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            // Security: block DOCTYPE declarations to prevent XXE attacks
            factory.setFeature(
                "http://apache.org/xml/features/disallow-doctype-decl", true);

            DocumentBuilder builder = factory.newDocumentBuilder();
            ByteArrayInputStream inputStream =
                new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8));

            return builder.parse(inputStream);

        } catch (Exception e) {
            throw new RuntimeException(
                "Failed to parse legacy XML payload: " + e.getMessage(), e);
        }
    }

    /**
     * Reads the text content of the first element matching {@code tagName}
     * inside {@code parent}. Throws a clear exception if the tag is absent.
     */
    private String getTagValue(String tagName, Element parent) {
        var nodeList = parent.getElementsByTagName(tagName);
        if (nodeList.getLength() == 0) {
            throw new RuntimeException(
                "Required XML tag <" + tagName + "> not found in payload.");
        }
        return nodeList.item(0).getTextContent().trim();
    }
}
