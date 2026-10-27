package ru.edu.pr03;

import java.time.LocalDate;

public class BusinessTripService {
    public BusinessTrip create(String id, LocalDate start, LocalDate end) {
        return new BusinessTrip(id, start, end);
    }
}
