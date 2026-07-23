package com.wethinkcode.bloodbroker.router;

import com.wethinkcode.bloodbroker.domain.BloodInventoryStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Step 4 — EIP Routing (Scatter-Gather + Aggregator)
 *
 * Enterprise Integration Pattern — Scatter-Gather:
 *  1. SCATTER: Send the hospital request to ALL blood banks simultaneously
 *     (LegacyBankSoapAdapter AND ModernBankRestAdapter run in parallel).
 *  2. GATHER:  Collect all replies and aggregate them into a single result.
 *
 * This class implements the GATHER (aggregation) side.
 *
 * Aggregation rules:
 *  - Sum the unitsAvailable from every reply.
 *  - If the total is 0 → flag emergencyShortage = true (triggers SMS alert).
 *  - If the total > 0 → emergencyShortage = false (stock exists somewhere).
 */
@Component
public class BloodBankRouter {

    private static final Logger log = LoggerFactory.getLogger(BloodBankRouter.class);

    /**
     * Aggregates multiple {@link BloodInventoryStatus} replies from the scatter phase
     * into one consolidated status.
     *
     * @param replies the list of responses from all queried blood banks
     * @return a single aggregated {@link BloodInventoryStatus}
     */
    public BloodInventoryStatus aggregate(List<BloodInventoryStatus> replies) {
        log.info("[ROUTER] Aggregating responses from {} blood bank(s).", replies.size());

        // ── Sum all available units across every bank reply ───────────────────
        int totalUnits = replies.stream()
                                .mapToInt(BloodInventoryStatus::getUnitsAvailable)
                                .sum();

        // ── Determine emergency status: shortage only when total is zero ───────
        boolean isShortage = (totalUnits == 0);

        if (isShortage) {
            log.warn("[ROUTER] ⚠ EMERGENCY SHORTAGE: total units across all banks = 0.");
        } else {
            log.info("[ROUTER] Aggregated total units available: {}.", totalUnits);
        }

        // ── Build and return the consolidated result ───────────────────────────
        BloodInventoryStatus aggregated = new BloodInventoryStatus();
        aggregated.setBankName("Aggregated");
        aggregated.setUnitsAvailable(totalUnits);
        aggregated.setEmergencyShortage(isShortage);

        return aggregated;
    }
}
