package ru.edu.pr03;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Scanner;

public class SelfCheck {
    private static int passed;
    private static int total;

    public static void main(String[] args) {
        runUnitChecks();
        runVariantChecks();
        runInputChecks();
        System.out.println("Unit: " + passed + "/" + total + " passed");
        boolean unitOk = passed == total;
        passed = 0;
        total = 0;
        runDataChecks();
        System.out.println("Data-driven: " + passed + "/" + total + " passed");
        if (!unitOk || passed != total) System.exit(1);
        runInteractiveMode();
    }

    private static void runUnitChecks() {
        // route: персональные данные важнее срочности.
        check("personal > urgent", 10000, true, RiskLevel.LOW, true, ApprovalRoute.SECURITY_REVIEW);
        // route: HIGH важнее срочности.
        check("HIGH > urgent", 10000, true, RiskLevel.HIGH, false, ApprovalRoute.DIRECTOR_REVIEW);
        // route: персональные данные важнее HIGH.
        check("personal > HIGH", 10000, false, RiskLevel.HIGH, true, ApprovalRoute.SECURITY_REVIEW);
        // route: финансовый контроль важнее обычной срочности.
        check("finance > urgent", 600000, true, RiskLevel.LOW, false, ApprovalRoute.FINANCE_REVIEW);
        // route: ускоренный маршрут.
        check("FAST_TRACK", 10000, true, RiskLevel.LOW, false, ApprovalRoute.FAST_TRACK);
        // route: точная граница финансового контроля.
        check("500000", 500000, false, RiskLevel.LOW, false, ApprovalRoute.FINANCE_REVIEW);
        // route: стандартный маршрут.
        check("STANDARD", 10000, false, RiskLevel.LOW, false, ApprovalRoute.STANDARD);
        // route: MEDIUM не активирует директорский контроль.
        check("MEDIUM", 10000, false, RiskLevel.MEDIUM, false, ApprovalRoute.STANDARD);
        // route: персональные данные без пересечений.
        check("SECURITY_REVIEW", 10000, false, RiskLevel.LOW, true, ApprovalRoute.SECURITY_REVIEW);
        // route: значение непосредственно перед границей.
        check("499999", 499999, true, RiskLevel.LOW, false, ApprovalRoute.FAST_TRACK);
        // route: значение непосредственно после границы.
        check("500001", 500001, false, RiskLevel.LOW, false, ApprovalRoute.FINANCE_REVIEW);
        // route: отрицательная сумма.
        checkError("negative amount", -1, RiskLevel.LOW, "Ошибка: amount must be >= 0, got -1");
        // route: отсутствующий риск.
        checkError("null risk", 10000, null, "Ошибка: risk is null");
    }

    private static void check(String name, long amount, boolean urgent, RiskLevel risk,
                              boolean personal, ApprovalRoute expected) {
        checkRoute(name, amount, urgent, risk, personal, null, expected);
    }

    private static void checkRoute(String name, long amount, boolean urgent, RiskLevel risk,
                                   boolean personal, String department, ApprovalRoute expected) {
        try {
            ApprovalRoute actual = new ApprovalRouter().route(amount, urgent, risk, personal, department);
            record(name, actual == expected, "ожидалось " + expected + ", получено " + actual);
        } catch (IllegalArgumentException e) {
            record(name, false, e.getMessage());
        }
    }

    private static void checkError(String name, long amount, RiskLevel risk, String expected) {
        try {
            new ApprovalRouter().route(amount, true, risk, false, null);
            record(name, false, "исключение не выброшено");
        } catch (IllegalArgumentException e) {
            record(name, expected.equals(e.getMessage()), e.getMessage());
        }
    }

    private static void record(String name, boolean ok, String details) {
        total++;
        if (ok) passed++;
        System.out.println((ok ? "PASS " : "FAIL ") + name + (ok ? "" : ": " + details));
    }

    private static void runDataChecks() {
        try {
            int number = 0;
            boolean headerSeen = false;
            for (String line : Files.readAllLines(Paths.get("data/sample.csv"), StandardCharsets.UTF_8)) {
                if (line.isEmpty() || line.startsWith("#")) continue;
                if (!headerSeen) {
                    if (!line.equals("amount;urgent;risk;personal;department;expected")) {
                        record("data/header", false, "неверный заголовок");
                        return;
                    }
                    headerSeen = true;
                    continue;
                }
                checkDataLine(line, ++number);
            }
            if (number == 0) record("data/empty", false, "нет проверок");
        } catch (IOException e) {
            record("data/file", false, e.getMessage());
        }
    }

    private static void runVariantChecks() {
        // route, часть А: IT-срочность на финансовой границе.
        checkRoute("IT/500000", 500000, true, RiskLevel.LOW, false, "IT", ApprovalRoute.FAST_TRACK);
        // route, часть А: IT-срочность выше финансового порога.
        checkRoute("IT/600000", 600000, true, RiskLevel.MEDIUM, false, "IT", ApprovalRoute.FAST_TRACK);
        // route: R1 сохраняет приоритет над IT-срочностью.
        checkRoute("IT/personal", 600000, true, RiskLevel.LOW, true, "IT", ApprovalRoute.SECURITY_REVIEW);
        // route: R2 сохраняет приоритет над IT-срочностью.
        checkRoute("IT/HIGH", 600000, true, RiskLevel.HIGH, false, "IT", ApprovalRoute.DIRECTOR_REVIEW);
        // route: несрочная IT-заявка остаётся финансовой.
        checkRoute("IT/not urgent", 600000, false, RiskLevel.LOW, false, "IT", ApprovalRoute.FINANCE_REVIEW);
        // route: другой отдел не обходит финансовую проверку.
        checkRoute("HR/urgent", 600000, true, RiskLevel.LOW, false, "HR", ApprovalRoute.FINANCE_REVIEW);
        // route: нулевая сумма допустима.
        checkRoute("zero", 0, false, RiskLevel.LOW, false, null, ApprovalRoute.STANDARD);
        // route: HIGH выше финансовой проверки.
        checkRoute("HIGH/finance", 600000, false, RiskLevel.HIGH, false, null, ApprovalRoute.DIRECTOR_REVIEW);
        // addDepartmentPriority, часть Б: срочная IT-заявка.
        checkPriority("priority/IT urgent", "IT", true, 1);
        // addDepartmentPriority, часть Б: несрочная IT-заявка.
        checkPriority("priority/IT ordinary", "IT", false, 0);
        // addDepartmentPriority: отсутствие отдела допустимо.
        checkPriority("priority/null", null, true, 0);
        // addDepartmentPriority: другой отдел.
        checkPriority("priority/HR", "HR", true, 0);
        // addDepartmentPriority: регистр IT важен.
        checkPriority("priority/it", "it", true, 0);
    }

    private static void checkPriority(String name, String department, boolean urgent, int expected) {
        try {
            int actual = new ApprovalRouter().addDepartmentPriority(department, urgent);
            record(name, actual == expected, "ожидалось " + expected + ", получено " + actual);
        } catch (IllegalArgumentException e) {
            record(name, false, e.getMessage());
        }
    }

    private static ApprovalRoute routeFields(String[] parts) {
        for (int i = 0; i < parts.length; i++) {
            if (parts[i].isEmpty() || !parts[i].equals(parts[i].strip())) {
                throw new IllegalArgumentException("Ошибка: пустое поле или пробелы, позиция " + (i + 1));
            }
        }
        try {
            long amount = Long.parseLong(parts[0]);
            boolean urgent = parseBoolean(parts[1]);
            RiskLevel risk = parts[2].equals("null") ? null : RiskLevel.valueOf(parts[2]);
            boolean personal = parseBoolean(parts[3]);
            String department = parts[4].equals("null") ? null : parts[4];
            return new ApprovalRouter().route(amount, urgent, risk, personal, department);
        } catch (IllegalArgumentException e) {
            if (e.getMessage() != null && e.getMessage().startsWith("Ошибка:")) throw e;
            throw new IllegalArgumentException("Ошибка: неверный формат суммы или уровня риска");
        }
    }

    private static boolean parseBoolean(String value) {
        if (!value.equals("true") && !value.equals("false")) {
            throw new IllegalArgumentException("Ошибка: ожидалось true или false");
        }
        return Boolean.parseBoolean(value);
    }

    private static void runInputChecks() {
        // Разбор: лишний завершающий разделитель.
        checkInputError("input/trailing", "10000;true;LOW;false;IT;", "Ошибка: ожидалось 5 полей");
        // Разбор: неполная строка.
        checkInputError("input/missing", "10000;true;LOW;false", "Ошибка: ожидалось 5 полей");
        // Разбор: пробелы запрещены.
        checkInputError("input/spaces", "10000; true;LOW;false;IT",
                "Ошибка: пустое поле или пробелы, позиция 2");
        // Разбор: неверное логическое значение.
        checkInputError("input/boolean", "10000;yes;LOW;false;IT", "Ошибка: ожидалось true или false");
        // Разбор: регистр enum важен.
        checkInputError("input/risk", "10000;true;low;false;IT",
                "Ошибка: неверный формат суммы или уровня риска");
        // Разбор: сумма должна быть числом.
        checkInputError("input/amount", "abc;true;LOW;false;IT",
                "Ошибка: неверный формат суммы или уровня риска");
        // Разбор: long не должен переполняться.
        checkInputError("input/overflow", "9223372036854775808;true;LOW;false;IT",
                "Ошибка: неверный формат суммы или уровня риска");
    }

    private static ApprovalRoute routeLine(String line) {
        String[] parts = line.split(";", -1);
        if (parts.length != 5) throw new IllegalArgumentException("Ошибка: ожидалось 5 полей");
        return routeFields(parts);
    }

    private static void checkInputError(String name, String line, String expected) {
        try {
            routeLine(line);
            record(name, false, "исключение не выброшено");
        } catch (IllegalArgumentException e) {
            record(name, expected.equals(e.getMessage()), e.getMessage());
        }
    }

    private static void runInteractiveMode() {
        System.out.println("Введите amount;urgent;risk;personal;department (exit или пустая строка — выход):");
        Scanner scanner = new Scanner(System.in, StandardCharsets.UTF_8);
        while (scanner.hasNextLine()) {
            String line = scanner.nextLine();
            if (line.isEmpty() || line.equals("exit")) break;
            try {
                System.out.println("Маршрут: " + routeLine(line));
            } catch (IllegalArgumentException e) {
                System.out.println(e.getMessage());
            }
        }
    }

    private static void checkDataLine(String line, int number) {
        String[] parts = line.split(";", -1);
        String expected = parts.length == 6 ? parts[5] : "";
        try {
            if (parts.length != 6) throw new IllegalArgumentException("Ошибка: ожидалось 6 полей CSV");
            ApprovalRoute actual = routeFields(parts);
            record("data/" + number, actual.name().equals(expected), "получено " + actual);
        } catch (IllegalArgumentException e) {
            record("data/" + number, expected.equals("ERROR"), e.getMessage());
        }
    }
}
