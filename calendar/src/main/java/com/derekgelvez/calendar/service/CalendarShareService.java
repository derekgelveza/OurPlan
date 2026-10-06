package com.derekgelvez.calendar.service;

import com.derekgelvez.calendar.dto.CalendarInviteResponseDTO;
import com.derekgelvez.calendar.dto.CalendarPermissionResponseDTO;
import com.derekgelvez.calendar.dto.CalendarResponseDTO;
import com.derekgelvez.calendar.dto.ShareCalendarRequestDTO;
import com.derekgelvez.calendar.dto.ShareCalendarResponseDTO;
import com.derekgelvez.calendar.exception.CalendarAccessDeniedException;
import com.derekgelvez.calendar.exception.InviteNotValidException;
import com.derekgelvez.calendar.exception.ResourceNotFoundException;
import com.derekgelvez.calendar.model.Calendar;
import com.derekgelvez.calendar.model.CalendarInvite;
import com.derekgelvez.calendar.model.CalendarPermission;
import com.derekgelvez.calendar.model.InviteStatus;
import com.derekgelvez.calendar.repository.CalendarInviteRepository;
import com.derekgelvez.calendar.repository.CalendarPermissionRepository;
import com.derekgelvez.user.model.User;
import com.derekgelvez.user.repository.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.List;
import java.util.Locale;

/**
 * Calendar sharing. An invite can be sent to someone who does not have an OurPlan account
 * yet, but they must have one to accept it. Acceptance is bound to the logged-in invitee
 * (their email or phone number must match the invite) and creates the CalendarPermission in
 * the same transaction. Expiry needs no scheduled job: it is checked when someone responds.
 */
@Service
public class CalendarShareService {

    private static final SecureRandom RANDOM = new SecureRandom();

    private final CalendarInviteRepository calendarInviteRepository;
    private final CalendarPermissionRepository calendarPermissionRepository;
    private final CalendarAccessService calendarAccessService;
    private final UserRepository userRepository;
    private final UserLookup userLookup;
    private final String inviteBaseUrl;
    private final int inviteExpiryDays;

    public CalendarShareService(CalendarInviteRepository calendarInviteRepository,
                                CalendarPermissionRepository calendarPermissionRepository,
                                CalendarAccessService calendarAccessService,
                                UserRepository userRepository,
                                UserLookup userLookup,
                                @Value("${ourplan.invite.base-url}") String inviteBaseUrl,
                                @Value("${ourplan.invite.expiry-days:7}") int inviteExpiryDays) {
        this.calendarInviteRepository = calendarInviteRepository;
        this.calendarPermissionRepository = calendarPermissionRepository;
        this.calendarAccessService = calendarAccessService;
        this.userRepository = userRepository;
        this.userLookup = userLookup;
        this.inviteBaseUrl = inviteBaseUrl.endsWith("/") ? inviteBaseUrl.substring(0, inviteBaseUrl.length() - 1)
                : inviteBaseUrl;
        this.inviteExpiryDays = inviteExpiryDays;
    }

    /** The owner invites someone by email or phone number with READ_ONLY or READ_WRITE access. */
    @Transactional
    public ShareCalendarResponseDTO shareCalendar(Long userId, Long calendarId, ShareCalendarRequestDTO dto) {
        Calendar calendar = calendarAccessService.assertIsOwner(userId, calendarId);
        User owner = calendar.getOwner();
        String contact = normalizeContact(dto.inviteeContact());

        if (contact.equals(owner.getEmail()) || contact.equals(normalizePhone(owner.getPhoneNumber()))) {
            throw new InviteNotValidException("You cannot invite yourself");
        }
        if (isEmail(contact)) {
            userRepository.findByEmail(contact)
                    .filter(invitee -> calendarPermissionRepository.existsByCalendarIdAndUserId(calendarId, invitee.getId()))
                    .ifPresent(invitee -> {
                        throw new InviteNotValidException("That person already has access to this calendar");
                    });
        }

        CalendarInvite invite = new CalendarInvite();
        invite.setCalendar(calendar);
        invite.setInviter(owner);
        invite.setInviteeContact(contact);
        invite.setToken(newToken());
        invite.setAccessLevel(dto.accessLevel());
        invite.setStatus(InviteStatus.PENDING);
        invite.setExpiresAt(Instant.now().plus(inviteExpiryDays, ChronoUnit.DAYS));
        invite = calendarInviteRepository.save(invite);
        return new ShareCalendarResponseDTO(inviteLink(invite), invite.getExpiresAt());
    }

    /** The calendar's PENDING invites, for the owner. Invites found to be past expiry are marked EXPIRED. */
    @Transactional
    public List<CalendarInviteResponseDTO> getPendingInvites(Long userId, Long calendarId) {
        calendarAccessService.assertIsOwner(userId, calendarId);
        Instant now = Instant.now();
        return calendarInviteRepository.findByCalendarIdAndStatus(calendarId, InviteStatus.PENDING).stream()
                .filter(invite -> {
                    if (invite.getExpiresAt().isAfter(now)) {
                        return true;
                    }
                    invite.setStatus(InviteStatus.EXPIRED);
                    return false;
                })
                .map(invite -> new CalendarInviteResponseDTO(invite.getId(), invite.getInviteeContact(),
                        invite.getAccessLevel(), invite.getStatus(), inviteLink(invite), invite.getExpiresAt()))
                .toList();
    }

    /**
     * Accepts an invite: it must be PENDING, not expired, and addressed to the logged-in user.
     * Creates the CalendarPermission and marks the invite ACCEPTED in one transaction.
     */
    @Transactional(noRollbackFor = InviteNotValidException.class)
    public CalendarResponseDTO acceptInvite(Long userId, String token) {
        CalendarInvite invite = getRespondableInvite(userId, token);
        Calendar calendar = invite.getCalendar();
        User invitee = userLookup.getUser(userId);
        if (calendar.getOwner().getId().equals(userId)) {
            throw new InviteNotValidException("You cannot accept an invite to your own calendar");
        }

        CalendarPermission permission = calendarPermissionRepository.findByCalendarIdAndUserId(calendar.getId(), userId)
                .orElseGet(() -> new CalendarPermission(calendar, invitee, invite.getAccessLevel()));
        permission.setAccessLevel(invite.getAccessLevel());
        calendarPermissionRepository.save(permission);
        invite.setStatus(InviteStatus.ACCEPTED);
        calendarInviteRepository.save(invite);
        return new CalendarResponseDTO(calendar.getId(), userLookup.displayName(calendar.getOwner()), false,
                permission.getAccessLevel());
    }

    /** Declines an invite. No permission is created. */
    @Transactional(noRollbackFor = InviteNotValidException.class)
    public void declineInvite(Long userId, String token) {
        CalendarInvite invite = getRespondableInvite(userId, token);
        invite.setStatus(InviteStatus.DECLINED);
        calendarInviteRepository.save(invite);
    }

    /** Guests of the calendar, for the owner. */
    @Transactional(readOnly = true)
    public List<CalendarPermissionResponseDTO> getPermissions(Long userId, Long calendarId) {
        calendarAccessService.assertIsOwner(userId, calendarId);
        return calendarPermissionRepository.findByCalendarId(calendarId).stream()
                .map(permission -> new CalendarPermissionResponseDTO(permission.getUser().getId(),
                        userLookup.displayName(permission.getUser()), permission.getUser().getEmail(),
                        permission.getAccessLevel()))
                .toList();
    }

    /** The owner revokes a guest's access, or a guest removes themselves (guestUserId = their own id). */
    @Transactional
    public void removePermission(Long userId, Long calendarId, Long guestUserId) {
        Calendar calendar = calendarAccessService.getCalendar(calendarId);
        boolean isOwner = calendar.getOwner().getId().equals(userId);
        if (!isOwner && !userId.equals(guestUserId)) {
            throw new CalendarAccessDeniedException("Only the owner can remove other guests");
        }
        CalendarPermission permission = calendarPermissionRepository.findByCalendarIdAndUserId(calendarId, guestUserId)
                .orElseThrow(() -> new ResourceNotFoundException("That user does not have access to this calendar"));
        calendarPermissionRepository.delete(permission);
    }

    // ---------------------------------------------------------------- helpers

    /** Finds a PENDING, unexpired invite addressed to the logged-in user; marks it EXPIRED if past expiry. */
    private CalendarInvite getRespondableInvite(Long userId, String token) {
        CalendarInvite invite = calendarInviteRepository.findByToken(token)
                .orElseThrow(() -> new InviteNotValidException("Invite not found"));
        if (invite.getStatus() != InviteStatus.PENDING) {
            throw new InviteNotValidException("Invite has already been " + invite.getStatus().name().toLowerCase(Locale.ROOT));
        }
        if (!invite.getExpiresAt().isAfter(Instant.now())) {
            invite.setStatus(InviteStatus.EXPIRED);
            calendarInviteRepository.save(invite);
            throw new InviteNotValidException("Invite has expired");
        }
        User user = userLookup.getUser(userId);
        String contact = invite.getInviteeContact();
        boolean addressedToUser = contact.equals(user.getEmail()) || contact.equals(normalizePhone(user.getPhoneNumber()));
        if (!addressedToUser) {
            throw new InviteNotValidException("This invite was sent to someone else");
        }
        return invite;
    }

    private String inviteLink(CalendarInvite invite) {
        return inviteBaseUrl + "/invites/" + invite.getToken();
    }

    private static String newToken() {
        byte[] bytes = new byte[32];
        RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private static boolean isEmail(String contact) {
        return contact.contains("@");
    }

    /** Emails are lower-cased; phone numbers keep only digits and a leading +. */
    static String normalizeContact(String contact) {
        String trimmed = contact.trim();
        return isEmail(trimmed) ? trimmed.toLowerCase(Locale.ROOT) : normalizePhone(trimmed);
    }

    static String normalizePhone(String phone) {
        if (phone == null || phone.isBlank()) {
            return null;
        }
        String digits = phone.replaceAll("[^0-9]", "");
        return phone.trim().startsWith("+") ? "+" + digits : digits;
    }
}
