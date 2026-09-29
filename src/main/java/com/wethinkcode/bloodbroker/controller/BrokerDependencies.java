package com.wethinkcode.bloodbroker.controller;

import com.wethinkcode.bloodbroker.adapter.LegacyBankSoapAdapter;
import com.wethinkcode.bloodbroker.adapter.ModernBankRestAdapter;
import com.wethinkcode.bloodbroker.adapter.TwilioNotificationAdapter;
import com.wethinkcode.bloodbroker.router.BloodBankRouter;
import com.wethinkcode.bloodbroker.transformer.PayloadEnricher;
import com.wethinkcode.bloodbroker.transformer.XmlToCanonicalTransformer;
import org.springframework.stereotype.Component;

@Component
public class BrokerDependencies {
    
    private final XmlToCanonicalTransformer transformer;
    private final PayloadEnricher enricher;
    private final LegacyBankSoapAdapter soapAdapter;
    private final ModernBankRestAdapter restAdapter;
    private final BloodBankRouter router;
    private final TwilioNotificationAdapter notificationAdapter;

    public BrokerDependencies(XmlToCanonicalTransformer transformer,
                              PayloadEnricher enricher,
                              LegacyBankSoapAdapter soapAdapter,
                              ModernBankRestAdapter restAdapter,
                              BloodBankRouter router,
                              TwilioNotificationAdapter notificationAdapter) {
        this.transformer = transformer;
        this.enricher = enricher;
        this.soapAdapter = soapAdapter;
        this.restAdapter = restAdapter;
        this.router = router;
        this.notificationAdapter = notificationAdapter;
    }

    public XmlToCanonicalTransformer getTransformer() {
        return transformer;
    }

    public PayloadEnricher getEnricher() {
        return enricher;
    }

    public LegacyBankSoapAdapter getSoapAdapter() {
        return soapAdapter;
    }

    public ModernBankRestAdapter getRestAdapter() {
        return restAdapter;
    }

    public BloodBankRouter getRouter() {
        return router;
    }

    public TwilioNotificationAdapter getNotificationAdapter() {
        return notificationAdapter;
    }
}
