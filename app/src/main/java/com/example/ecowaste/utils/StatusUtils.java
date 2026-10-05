package com.example.ecowaste.utils;

public class StatusUtils {
    public static String getDisplayStatus(String rawStatus) {
        if (rawStatus == null) return "Pending";
        
        switch (rawStatus) {
            case "PENDING_REVIEW":
                return "Waiting for Member";
            case "OFFER_SENT":
                return "Rate Offered by Member";
            case "USER_ACCEPTED":
                return "Offer Accepted by You";
            case "USER_REJECTED":
                return "Offer Rejected";
            case "PICKUP_SCHEDULED":
                return "Pickup Scheduled";
            case "COLLECTED":
                return "Collected & Paid";
            case "BULK_REQUESTED":
                return "Bulk Sale Pending";
            case "ACCEPTED_BY_COMPANY":
                return "Company Offer Received";
            case "COMPLETED":
                return "Completed";
            case "REJECTED":
                return "Rejected";
            case "CANCELLED":
                return "Cancelled";
            default:
                return rawStatus;
        }
    }
}
