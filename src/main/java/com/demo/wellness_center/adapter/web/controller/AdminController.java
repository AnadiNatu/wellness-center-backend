package com.demo.wellness_center.adapter.web.controller;

import com.demo.wellness_center.adapter.web.dto.AppointmentDto;
import com.demo.wellness_center.adapter.web.dto.AuthDto;
import com.demo.wellness_center.adapter.web.dto.WellnessDto;
import com.demo.wellness_center.adapter.web.service.AppointmentService;
import com.demo.wellness_center.adapter.web.service.AuthService;
import com.demo.wellness_center.adapter.web.service.UserService;
import com.demo.wellness_center.adapter.web.service.WellnessServiceService;
import com.demo.wellness_center.domain.enums.AppointmentStatus;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminController {
    private final UserService userService;
    private final WellnessServiceService wellnessService;
    private final AppointmentService appointments;

    @GetMapping("/users")
    public List<AuthDto.UserResponse> users(@RequestParam(defaultValue = "0") int page,
                                            @RequestParam(defaultValue = "20") int size) {
        return userService.listUsers(page, size);
    }

    @GetMapping("/users/{id}")
    public AuthDto.UserResponse user(@PathVariable UUID id) {
        return userService.getProfile(id);
    }

    @PatchMapping("/users/{id}/permissions")
    public AuthDto.UserResponse permissions(@PathVariable UUID id, @RequestBody AuthDto.PermissionsRequest r) {
        return userService.updatePermissions(id, r);
    }

    @GetMapping("/services")
    public List<WellnessDto.ServiceResponse> listServices(@RequestParam(defaultValue = "0") int page,
                                                          @RequestParam(defaultValue = "20") int size) {
        return wellnessService.listAdmin(page, size);
    }

    @GetMapping("/services/{id}")
    public WellnessDto.ServiceResponse getService(@PathVariable UUID id) {
        return wellnessService.getAdmin(id);
    }

    @PostMapping("/services")
    @ResponseStatus(HttpStatus.CREATED)
    public WellnessDto.ServiceResponse createService(Authentication a, @Valid @RequestBody WellnessDto.ServiceRequest r) {
        return wellnessService.create(r, UUID.fromString(a.getName()));
    }

    @PutMapping("/services/{id}")
    public WellnessDto.ServiceResponse updateService(@PathVariable UUID id, @Valid @RequestBody WellnessDto.ServiceRequest r) {
        return wellnessService.update(id, r);
    }

    @PatchMapping("/services/{id}/activate")
    public WellnessDto.ServiceResponse activate(@PathVariable UUID id) {
        return wellnessService.setActive(id, true);
    }

    @PatchMapping("/services/{id}/deactivate")
    public WellnessDto.ServiceResponse deactivate(@PathVariable UUID id) {
        return wellnessService.setActive(id, false);
    }

    @PostMapping("/services/{id}/image")
    public WellnessDto.ServiceResponse serviceImage(@PathVariable UUID id, @RequestParam("file") MultipartFile file) {
        return wellnessService.updateImage(id, file);
    }

    @DeleteMapping("/services/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteService(@PathVariable UUID id) {
        wellnessService.delete(id);
    }

    // ---------- appointments ----------

    @GetMapping("/appointments")
    public List<AppointmentDto.AppointmentResponse> listAppointments(
            @RequestParam(required = false) AppointmentStatus status,
            @RequestParam(required = false) UUID serviceId,
            @RequestParam(required = false) UUID userId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam(required = false) String ref,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return appointments.listAdmin(status, serviceId, userId, date, ref, page, size);
    }

    @GetMapping("/appointments/{id}")
    public AppointmentDto.AppointmentResponse getAppointment(Authentication a, @PathVariable UUID id) {
        return appointments.getOne(id, adminId(a), true);
    }

    @PatchMapping("/appointments/{id}/approve")
    public AppointmentDto.AppointmentResponse approve(Authentication a, @PathVariable UUID id,
                                                      @RequestBody(required = false) AppointmentDto.NotesRequest r) {
        return appointments.approve(id, adminId(a), r == null ? null : r.notes());
    }

    @PatchMapping("/appointments/{id}/reject")
    public AppointmentDto.AppointmentResponse reject(@PathVariable UUID id, @RequestBody(required = false) AppointmentDto.RejectRequest r) {
        return appointments.reject(id, r == null ? null : r.reason());
    }

    @PatchMapping("/appointments/{id}/reschedule")
    public AppointmentDto.AppointmentResponse reschedule(@PathVariable UUID id, @Valid @RequestBody AppointmentDto.RescheduleRequest r) {
        return appointments.reschedule(id, r.appointmentDate(), r.startTime());
    }

    @PatchMapping("/appointments/{id}/cancel")
    public AppointmentDto.AppointmentResponse cancel(@PathVariable UUID id, @RequestBody(required = false) AppointmentDto.CancelRequest r) {
        return appointments.cancelAsAdmin(id, r == null ? null : r.reason());
    }

    @PatchMapping("/appointments/{id}/complete")
    public AppointmentDto.AppointmentResponse complete(@PathVariable UUID id) {
        return appointments.complete(id);
    }

    @PutMapping("/appointments/{id}/meeting-link")
    public AppointmentDto.AppointmentResponse meetingLink(@PathVariable UUID id, @RequestBody AppointmentDto.MeetingLinkRequest r) {
        return appointments.updateMeetingLink(id, r);
    }

    @PatchMapping("/appointments/{id}/notes")
    public AppointmentDto.AppointmentResponse notes(@PathVariable UUID id, @Valid @RequestBody AppointmentDto.NotesRequest r) {
        return appointments.updateNotes(id, r.notes());
    }

    private UUID adminId(Authentication a) {
        return UUID.fromString(a.getName());
    }

}