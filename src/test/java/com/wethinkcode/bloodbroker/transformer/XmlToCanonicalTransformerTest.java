package com.wethinkcode.bloodbroker.transformer;

import com.wethinkcode.bloodbroker.domain.HospitalRequest;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class XmlToCanonicalTransformerTest {

    @Test
    void testTransform() {
        XmlToCanonicalTransformer transformer = new XmlToCanonicalTransformer();
        String xml = "<hospitalRequest><bloodType>A-Positive</bloodType><unitsRequired>3</unitsRequired><hospitalId>HOSP-456</hospitalId></hospitalRequest>";
        
        HospitalRequest request = transformer.transform(xml);
        
        assertNotNull(request);
        assertEquals("A-Positive", request.getBloodType());
        assertEquals(3, request.getUnitsRequired());
        assertEquals("HOSP-456", request.getHospitalId());
        assertNull(request.getTimestamp(), "Timestamp should be null initially before enrichment");
    }
}
