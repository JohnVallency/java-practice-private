package ru.edu.pr03;

import java.time.LocalDate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class BusinessTrip {
    private static final int MIN_REASON_LENGTH = 15;

    private final String id;
    private final LocalDate start;
    private final LocalDate end;
    private TripStatus status = TripStatus.DRAFT;
    private String rejectionReason;

    public BusinessTrip(String id, LocalDate start, LocalDate end) {
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("id is null or blank");
        }
        if (start == null || end == null || start.isAfter(end)) {
            throw new IllegalArgumentException("start must be <= end");
        }
        this.id = id;
        this.start = start;
        this.end = end;
    }

    public void submit() {
        requireStatus(TripStatus.DRAFT, TripStatus.SUBMITTED);
        status = TripStatus.SUBMITTED;
    }

    public void approve() {
        requireStatus(TripStatus.SUBMITTED, TripStatus.APPROVED);
        status = TripStatus.APPROVED;
    }

    public void reject(String reason) {
        requireStatus(TripStatus.SUBMITTED, TripStatus.REJECTED);
        if (reason == null || reason.length() < MIN_REASON_LENGTH) {
            throw new IllegalArgumentException("rejectionReason must be at least "
                    + MIN_REASON_LENGTH + " characters, got " + (reason == null ? 0 : reason.length()));
        }
        if (addRejectCode(reason) == null) {
            throw new IllegalArgumentException("rejectionReason must contain code R-NNN");
        }
        rejectionReason = reason;
        status = TripStatus.REJECTED;
    }

    public void cancel() {
        if (status != TripStatus.DRAFT && status != TripStatus.SUBMITTED && status != TripStatus.APPROVED) {
            throw transitionError(TripStatus.CANCELLED);
        }
        status = TripStatus.CANCELLED;
    }

    public void complete() {
        requireStatus(TripStatus.APPROVED, TripStatus.COMPLETED);
        if (LocalDate.now().isBefore(end)) {
            throw new IllegalStateException("complete возможен только после " + end);
        }
        status = TripStatus.COMPLETED;
    }

    private void requireStatus(TripStatus required, TripStatus target) {
        if (status != required) throw transitionError(target);
    }

    private IllegalStateException transitionError(TripStatus target) {
        return new IllegalStateException("Ошибка перехода: " + status + " → " + target + " недопустим");
    }

    public String getId() {
        return id;
    }

    public LocalDate getStart() {
        return start;
    }

    public LocalDate getEnd() {
        return end;
    }

    public TripStatus getStatus() {
        return status;
    }

    public String getRejectionReason() {
        return rejectionReason;
    }

    public String addRejectCode(String reason) {
        if (reason == null) return null;
        Matcher matcher = Pattern.compile("\\bR-[0-9]{3}\\b").matcher(reason);
        return matcher.find() ? matcher.group() : null;
    }
}
