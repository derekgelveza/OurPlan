package com.derekgelvez.calendar.model;

/** Lifecycle of an invite link. Only PENDING invites can be accepted or declined. */
public enum InviteStatus {
    PENDING,
    ACCEPTED,
    DECLINED,
    /** Set lazily, when an expired invite is next looked at; no background job runs. */
    EXPIRED
}
