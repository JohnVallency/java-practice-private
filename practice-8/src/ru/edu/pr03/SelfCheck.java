package ru.edu.pr03;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.Scanner;

public class SelfCheck {
    private static int passed;
    private static int total;

    public static void main(String[] args) {
        runUnitChecks();
        runTransitionMatrix();
        System.out.println("Unit: " + passed + "/" + total + " passed");
        boolean unitOk = passed == total;
        passed = 0;
        total = 0;
        runDataChecks();
        System.out.println("Data-driven: " + passed + "/" + total + " passed");
        if (!unitOk || passed != total) System.exit(1);
        runInteractiveMode();
    }

    private static void record(String name, boolean ok, String details) {
        total++;
        if (ok) passed++;
        System.out.println((ok ? "PASS " : "FAIL ") + name + (ok ? "" : ": " + details));
    }

    private static BusinessTrip newTrip() {
        LocalDate today = LocalDate.now();
        return new BusinessTripService().create("T-01", today.minusDays(2), today.minusDays(1));
    }

    private static void runUnitChecks() {
        for (int number = 1; number <= 40; number++) {
            try {
                boolean ok = runScenario(number);
                record("unit/" + number, ok && expectedError(number) == null, "неверный результат");
            } catch (RuntimeException e) {
                record("unit/" + number, e.getClass().getSimpleName().equals(expectedError(number))
                        && e.getMessage().contains(expectedMessage(number)), e.toString());
            }
        }
    }

    private static String expectedError(int number) {
        return switch (number) {
            case 2, 3, 4, 5, 10, 11, 19, 21, 25, 26, 27 -> "IllegalArgumentException";
            case 7, 9, 12, 14, 16, 17, 31, 32, 33, 34, 40 -> "IllegalStateException";
            default -> null;
        };
    }

    private static String expectedMessage(int number) {
        return switch (number) {
            case 2, 3 -> "id is null or blank";
            case 4, 5, 26 -> "start must be <= end";
            case 27 -> "id is null or blank";
            case 10, 11, 19, 25 -> "rejectionReason must be at least 15";
            case 21 -> "rejectionReason must contain code R-NNN";
            case 17 -> "complete возможен только после";
            default -> "недопустим";
        };
    }

    private static boolean runScenario(int number) {
        BusinessTrip trip = newTrip();
        if (number > 18) return extraScenario(number, trip);
        if (number <= 5) return constructorScenario(number, trip);
        if (number <= 12) return transitionScenario(number, trip);
        return completionScenario(number, trip);
    }

    private static boolean constructorScenario(int number, BusinessTrip trip) {
        LocalDate today = LocalDate.now();
        switch (number) {
            // Конструктор: корректные данные и начальный статус.
            case 1: return trip.getStatus() == TripStatus.DRAFT;
            // Конструктор: отсутствующий id.
            case 2: new BusinessTrip(null, today, today); break;
            // Конструктор: пустой id.
            case 3: new BusinessTrip("", today, today); break;
            // Конструктор: отсутствующая дата начала.
            case 4: new BusinessTrip("T-01", null, today); break;
            // Конструктор: обратный порядок дат.
            case 5: new BusinessTrip("T-01", today.plusDays(1), today); break;
            default: throw new IllegalArgumentException("unknown scenario");
        }
        return false;
    }

    private static boolean transitionScenario(int number, BusinessTrip trip) {
        switch (number) {
            // submit: разрешён из DRAFT.
            case 6: trip.submit(); return trip.getStatus() == TripStatus.SUBMITTED;
            // submit: повторная отправка запрещена.
            case 7: trip.submit(); trip.submit(); break;
            // approve: разрешён из SUBMITTED.
            case 8: trip.submit(); trip.approve(); return trip.getStatus() == TripStatus.APPROVED;
            // approve: запрещён из DRAFT.
            case 9: trip.approve(); break;
            // reject: базовая причина 10 символов теперь отклоняется вариантом 27.
            case 10:
                trip.submit(); trip.reject("0123456789");
                return trip.getStatus() == TripStatus.REJECTED
                        && "0123456789".equals(trip.getRejectionReason());
            // reject: короткая причина запрещена.
            case 11: trip.submit(); trip.reject("Нет"); break;
            // reject: неверный исходный статус проверяется первым.
            case 12: trip.reject("0123456789"); break;
            default: throw new IllegalArgumentException("unknown scenario");
        }
        return false;
    }

    private static boolean completionScenario(int number, BusinessTrip trip) {
        switch (number) {
            // cancel: отмена черновика.
            case 13: trip.cancel(); return trip.getStatus() == TripStatus.CANCELLED;
            // cancel: завершённая заявка терминальна.
            case 14: trip.submit(); trip.approve(); trip.complete(); trip.cancel(); break;
            // complete: APPROVED и дата окончания уже наступила.
            case 15:
                trip.submit(); trip.approve(); trip.complete();
                return trip.getStatus() == TripStatus.COMPLETED;
            // complete: из SUBMITTED запрещён.
            case 16: trip.submit(); trip.complete(); break;
            // complete: дата окончания в будущем.
            case 17:
                BusinessTrip future = new BusinessTrip("T-F", LocalDate.now(), LocalDate.now().plusDays(2));
                future.submit(); future.approve(); future.complete(); break;
            // getRejectionReason: сохранённая причина.
            case 18:
                trip.submit(); trip.reject("R-001: Недостаточно бюджета");
                return "R-001: Недостаточно бюджета".equals(trip.getRejectionReason());
            default: throw new IllegalArgumentException("unknown scenario");
        }
        return false;
    }

    private static void runDataChecks() {
        try {
            int number = 0;
            boolean headerSeen = false;
            for (String line : Files.readAllLines(Paths.get("data/sample.csv"), StandardCharsets.UTF_8)) {
                if (line.isEmpty() || line.startsWith("#")) continue;
                if (!headerSeen) {
                    record("data/header", line.equals("id;start;end;action;expected"), line);
                    headerSeen = true;
                } else {
                    checkDataLine(line, ++number);
                }
            }
            if (number == 0) record("data/empty", false, "нет проверок");
        } catch (IOException e) {
            record("data/file", false, e.getMessage());
        }
    }

    private static void checkDataLine(String line, int number) {
        String[] parts = line.split(";", -1);
        if (parts.length != 5) {
            record("data/" + number, false, "ожидалось 5 колонок");
            return;
        }
        BusinessTrip trip;
        try {
            validateFields(parts);
            trip = new BusinessTripService().create(parts[0], LocalDate.parse(parts[1]), LocalDate.parse(parts[2]));
        } catch (IllegalArgumentException | DateTimeParseException e) {
            record("data/" + number, parts[4].equals("SKIP:" + e.getMessage()), e.toString());
            return;
        }
        checkDataAction(trip, parts[3], parts[4], number);
    }

    private static void checkDataAction(BusinessTrip trip, String action, String expected, int number) {
        try {
            executeActions(trip, action);
            record("data/" + number, expected.equals("OK:status=" + trip.getStatus()),
                    "получено " + trip.getStatus());
        } catch (IllegalArgumentException | IllegalStateException e) {
            record("data/" + number, expected.equals("ERROR:" + e.getClass().getSimpleName()), e.toString());
        }
    }

    private static boolean extraScenario(int number, BusinessTrip trip) {
        if (number <= 25) return variantScenario(number, trip);
        if (number <= 34) return boundaryScenario(number, trip);
        return rejectScenario(number, trip);
    }

    private static boolean variantScenario(int number, BusinessTrip trip) {
        switch (number) {
            // Часть А, reject: 14 символов с кодом недостаточно.
            case 19: trip.submit(); trip.reject("R-001:12345678"); break;
            // Часть А, reject: 15 символов с кодом допустимы.
            case 20:
                trip.submit(); trip.reject("R-001:123456789");
                return trip.getStatus() == TripStatus.REJECTED
                        && "R-001:123456789".equals(trip.getRejectionReason());
            // Часть А, reject: длинная причина без кода запрещена.
            case 21: trip.submit(); trip.reject("Недостаточно бюджета"); break;
            // Часть Б, addRejectCode: код извлекается.
            case 22: return "R-001".equals(trip.addRejectCode("R-001: причина отклонения"));
            // Часть Б, addRejectCode: отсутствие кода.
            case 23: return trip.addRejectCode("Недостаточно бюджета") == null;
            // addRejectCode: null не содержит кода.
            case 24: return trip.addRejectCode(null) == null;
            // reject: null-причина даёт IAE, а не NPE.
            case 25: trip.submit(); trip.reject(null); break;
            default: throw new IllegalArgumentException("unknown scenario");
        }
        return false;
    }

    private static boolean boundaryScenario(int number, BusinessTrip trip) {
        LocalDate today = LocalDate.now();
        switch (number) {
            // Конструктор: отсутствующая дата окончания.
            case 26: new BusinessTrip("T-01", today, null); break;
            // Конструктор: id из пробелов.
            case 27: new BusinessTrip(" ", today, today); break;
            // complete: today == end и start == end разрешены.
            case 28:
                BusinessTrip oneDay = new BusinessTrip("T-DAY", today, today);
                oneDay.submit(); oneDay.approve(); oneDay.complete();
                return oneDay.getStatus() == TripStatus.COMPLETED;
            // cancel: разрешён из SUBMITTED.
            case 29: trip.submit(); trip.cancel(); return trip.getStatus() == TripStatus.CANCELLED;
            // cancel: разрешён из APPROVED.
            case 30:
                trip.submit(); trip.approve(); trip.cancel();
                return trip.getStatus() == TripStatus.CANCELLED;
            // cancel: REJECTED терминален.
            case 31: trip.submit(); trip.reject("R-001:123456789"); trip.cancel(); break;
            // cancel: повторная отмена запрещена.
            case 32: trip.cancel(); trip.cancel(); break;
            // submit: COMPLETED терминален.
            case 33: trip.submit(); trip.approve(); trip.complete(); trip.submit(); break;
            // approve: CANCELLED терминален.
            case 34: trip.cancel(); trip.approve(); break;
            default: throw new IllegalArgumentException("unknown scenario");
        }
        return false;
    }

    private static boolean rejectScenario(int number, BusinessTrip trip) {
        switch (number) {
            // reject: отказ не изменяет статус и причину.
            case 35:
                trip.submit();
                try { trip.reject("Причина без кода, но достаточной длины"); return false; }
                catch (IllegalArgumentException e) {
                    return trip.getStatus() == TripStatus.SUBMITTED && trip.getRejectionReason() == null;
                }
            // addRejectCode: неполный код не подходит.
            case 36: return trip.addRejectCode("R-12: причина отклонения") == null;
            // addRejectCode: четыре цифры не являются кодом из трёх цифр.
            case 37: return trip.addRejectCode("R-1234: причина отклонения") == null;
            // addRejectCode: код не обязан стоять в начале строки.
            case 38: return "R-002".equals(trip.addRejectCode("Причина R-002: отклонено"));
            // addRejectCode: регистр R важен.
            case 39: return trip.addRejectCode("r-001: причина отклонения") == null;
            // complete: повторное завершение запрещено.
            case 40: trip.submit(); trip.approve(); trip.complete(); trip.complete(); break;
            default: throw new IllegalArgumentException("unknown scenario");
        }
        return false;
    }

    private static void runTransitionMatrix() {
        String[] actions = {"SUBMIT", "APPROVE", "REJECT:R-001:123456789", "CANCEL", "COMPLETE"};
        TripStatus[] targets = {TripStatus.SUBMITTED, TripStatus.APPROVED, TripStatus.REJECTED,
                TripStatus.CANCELLED, TripStatus.COMPLETED};
        for (TripStatus status : TripStatus.values()) {
            for (int i = 0; i < actions.length; i++) {
                BusinessTrip trip = tripWithStatus(status);
                String reason = trip.getRejectionReason();
                boolean allowed = isAllowed(status, targets[i]);
                String name = "matrix/" + status + "/" + targets[i];
                try {
                    executeAction(trip, actions[i]);
                    record(name, allowed && trip.getStatus() == targets[i], "неверный переход");
                } catch (IllegalStateException e) {
                    record(name, !allowed && trip.getStatus() == status
                            && java.util.Objects.equals(reason, trip.getRejectionReason())
                            && e.getMessage().contains("недопустим"), e.toString());
                }
            }
        }
    }

    private static boolean isAllowed(TripStatus from, TripStatus to) {
        return switch (from) {
            case DRAFT -> to == TripStatus.SUBMITTED || to == TripStatus.CANCELLED;
            case SUBMITTED -> to == TripStatus.APPROVED || to == TripStatus.REJECTED || to == TripStatus.CANCELLED;
            case APPROVED -> to == TripStatus.COMPLETED || to == TripStatus.CANCELLED;
            default -> false;
        };
    }

    private static BusinessTrip tripWithStatus(TripStatus status) {
        BusinessTrip trip = newTrip();
        switch (status) {
            case DRAFT: break;
            case SUBMITTED: trip.submit(); break;
            case APPROVED: trip.submit(); trip.approve(); break;
            case REJECTED: trip.submit(); trip.reject("R-001:123456789"); break;
            case CANCELLED: trip.cancel(); break;
            case COMPLETED: trip.submit(); trip.approve(); trip.complete(); break;
            default: throw new IllegalArgumentException("unknown status");
        }
        return trip;
    }

    private static void validateFields(String[] parts) {
        for (int i = 0; i < parts.length; i++) {
            if (!parts[i].equals(parts[i].strip())) {
                throw new IllegalArgumentException("surrounding spaces are not allowed");
            }
        }
    }

    private static void executeActions(BusinessTrip trip, String actions) {
        for (String action : actions.split(">", -1)) {
            executeAction(trip, action);
        }
    }

    private static BusinessTrip readTrip(String data) {
        String[] parts = data.split(";", -1);
        if (parts.length != 3) throw new IllegalArgumentException("ожидалось id;start;end");
        validateFields(parts);
        return new BusinessTripService().create(parts[0], LocalDate.parse(parts[1]), LocalDate.parse(parts[2]));
    }

    private static void printTrip(BusinessTrip trip) {
        System.out.println("Заявка: " + trip.getId() + ", статус: " + trip.getStatus());
        if (trip.getRejectionReason() != null) {
            System.out.println("Причина: " + trip.getRejectionReason());
            System.out.println("Код: " + trip.addRejectCode(trip.getRejectionReason()));
        }
    }

    private static void runInteractiveMode() {
        BusinessTrip trip = newTrip();
        System.out.println("NEW id;start;end, SUBMIT, APPROVE, REJECT:<причина>, CANCEL, COMPLETE, STATUS, exit");
        printTrip(trip);
        Scanner scanner = new Scanner(System.in, StandardCharsets.UTF_8);
        while (scanner.hasNextLine()) {
            String line = scanner.nextLine();
            if (line.isEmpty() || line.equals("exit")) break;
            try {
                if (line.startsWith("NEW ")) trip = readTrip(line.substring(4));
                else if (!line.equals("STATUS")) executeActions(trip, line);
            } catch (IllegalArgumentException | IllegalStateException | DateTimeParseException e) {
                System.out.println("Ошибка: " + e.getMessage());
            }
            printTrip(trip);
        }
    }

    private static void executeAction(BusinessTrip trip, String action) {
        if (action.startsWith("REJECT:")) {
            trip.reject(action.substring("REJECT:".length()));
            return;
        }
        switch (action) {
            case "SUBMIT": trip.submit(); break;
            case "APPROVE": trip.approve(); break;
            case "REJECT": trip.reject(null); break;
            case "CANCEL": trip.cancel(); break;
            case "COMPLETE": trip.complete(); break;
            default: throw new IllegalArgumentException("неизвестное действие");
        }
    }
}
