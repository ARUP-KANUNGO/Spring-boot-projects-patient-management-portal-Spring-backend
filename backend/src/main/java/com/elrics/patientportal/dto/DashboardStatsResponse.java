package com.elrics.patientportal.dto;

import java.time.Instant;
import java.util.List;

public record DashboardStatsResponse(long totalPatients, long todaysAdmissions, long pendingReviews,
                                     long dischargedToday, long currentlyAdmitted,
                                     List<RecentActivity> recentActivity) {

    /** event is "registered" (newly created record) or "updated". */
    public record RecentActivity(String patientId, String patientName, String status, String event, Instant timestamp) {
    }
}
