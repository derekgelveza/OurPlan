package com.derekgelvez.calendar.service;

import com.derekgelvez.calendar.dto.CalendarResponseDTO;
import com.derekgelvez.calendar.exception.CalendarAlreadyExistsException;
import com.derekgelvez.calendar.model.Calendar;
import com.derekgelvez.calendar.model.CalendarPermission;
import com.derekgelvez.calendar.repository.CalendarPermissionRepository;
import com.derekgelvez.calendar.repository.CalendarRepository;
import com.derekgelvez.user.model.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

/** Calendar creation and listing. Each user has one calendar. */
@Service
@RequiredArgsConstructor
public class CalendarService {

    private final CalendarRepository calendarRepository;
    private final CalendarPermissionRepository calendarPermissionRepository;
    private final CategoryService categoryService;
    private final UserLookup userLookup;

    /** Creates the user's calendar and their "Uncategorized" category. */
    @Transactional
    public CalendarResponseDTO createCalendar(Long userId) {
        if (calendarRepository.existsByOwnerId(userId)) {
            throw new CalendarAlreadyExistsException("You already have a calendar");
        }
        User owner = userLookup.getUser(userId);
        Calendar calendar = calendarRepository.save(new Calendar(owner));
        categoryService.getOrCreateDefaultCategory(owner);
        return new CalendarResponseDTO(calendar.getId(), userLookup.displayName(owner), true, null);
    }

    /** The user's own calendar (if created) followed by calendars shared with them. */
    @Transactional(readOnly = true)
    public List<CalendarResponseDTO> getCalendars(Long userId) {
        List<CalendarResponseDTO> calendars = new ArrayList<>();
        calendarRepository.findByOwnerId(userId).ifPresent(own -> calendars.add(
                new CalendarResponseDTO(own.getId(), userLookup.displayName(own.getOwner()), true, null)));
        for (CalendarPermission permission : calendarPermissionRepository.findByUserId(userId)) {
            Calendar shared = permission.getCalendar();
            calendars.add(new CalendarResponseDTO(shared.getId(), userLookup.displayName(shared.getOwner()),
                    false, permission.getAccessLevel()));
        }
        return calendars;
    }
}
