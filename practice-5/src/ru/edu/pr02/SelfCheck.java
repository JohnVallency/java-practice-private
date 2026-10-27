package ru.edu.pr02;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Scanner;

public class SelfCheck {
    private static int unitPassed;
    private static int unitTotal;

    public static void main(String[] args) {
        runUnitChecks();
        int[] dataResult = runDataChecks();

        System.out.println("Unit: " + unitPassed + "/" + unitTotal + " passed");
        System.out.println("Data-driven: " + dataResult[0] + "/" + dataResult[1] + " passed");
        if (unitPassed != unitTotal || dataResult[0] != dataResult[1]) {
            System.exit(1);
        }

        runInteractiveMode();
    }

    private static void runUnitChecks() {
        // parseHours: корректная строка
        checkParse("parseHours/корректная строка", "8;9;10;8;8", new int[]{8, 9, 10, 8, 8});
        // parseHours: пустая строка
        checkParseError("parseHours/пустая строка", "", "структура");
        // parseHours: значение выше границы
        checkParseError("parseHours/значение 17", "8;8;17;8;8", "позиция 3");
        // parseHours: нечисловое значение
        checkParseError("parseHours/не число", "8;abc;8;8;8", "позиция 2");
        // parseHours: четыре значения
        checkParseError("parseHours/4 значения", "8;8;8;8", "структура");
        // parseHours: завершающий разделитель
        checkParseError("parseHours/trailing ;", "8;8;8;8;8;", "структура");
        // parseHours: ведущий ноль
        checkParseError("parseHours/ведущий ноль", "08;8;8;8;8", "позиция 1");

        // total: типовой массив
        checkInt("total/типовое", "total", new int[]{8, 9, 10, 8, 8}, 43);
        // total: нижняя граница
        checkInt("total/нижняя граница", "total", new int[]{0, 0, 0, 0, 0}, 0);
        // total: верхняя граница
        checkInt("total/верхняя граница", "total", new int[]{16, 16, 16, 16, 16}, 80);

        // average: типовой массив
        checkDouble("average/типовое", new int[]{8, 9, 10, 8, 8}, 8.6);
        // average: целое среднее
        checkDouble("average/целое", new int[]{8, 8, 8, 8, 8}, 8.0);
        // average: дробное среднее
        checkDouble("average/дробное", new int[]{7, 8, 9, 8, 7}, 7.8);

        // overtimeDays: нет переработки
        checkInt("overtimeDays/нет переработки", "od", new int[]{8, 8, 8, 8, 8}, 0);
        // overtimeDays: два дня
        checkInt("overtimeDays/два дня", "od", new int[]{9, 8, 7, 10, 8}, 2);
        // overtimeDays: граница 8
        checkInt("overtimeDays/граница 8", "od", new int[]{8, 9, 8, 8, 8}, 1);

        // weeklyOvertime: сумма 40
        checkInt("weeklyOvertime/сумма 40", "wo", new int[]{8, 8, 8, 8, 8}, 0);
        // weeklyOvertime: сумма 50
        checkInt("weeklyOvertime/сумма 50", "wo", new int[]{10, 10, 10, 10, 10}, 10);
        // weeklyOvertime: сумма меньше 40
        checkInt("weeklyOvertime/сумма < 40", "wo", new int[]{6, 6, 6, 6, 6}, 0);

        // Вариант 27, часть А: parseHours принимает отгул
        checkParse("variant/отгул допустим", "8;-1;8;8;8", new int[]{8, -1, 8, 8, 8});
        // Вариант 27, часть А: total не считает отгул часом
        checkInt("variant/total без отгула", "total", new int[]{8, -1, 8, 8, 8}, 32);
        // Вариант 27, часть А: average делит на неотгульные дни
        checkDouble("variant/average без отгула", new int[]{8, -1, 10, 8, 6}, 8.0);
        // Вариант 27, часть А: среднее при пяти отгулах
        checkDouble("variant/пять отгулов", new int[]{-1, -1, -1, -1, -1}, 0.0);
        // Вариант 27, часть Б: отгулов нет
        checkInt("addDayOffCount/нет отгулов", "dayOff", new int[]{8, 8, 8, 8, 8}, 0);
        // Вариант 27, часть Б: несколько отгулов
        checkInt("addDayOffCount/два отгула", "dayOff", new int[]{-1, 8, -1, 8, 8}, 2);
    }

    private static void checkParse(String name, String input, int[] expected) {
        unitTotal++;
        try {
            int[] actual = TimesheetAnalyzer.parseHours(input);
            recordUnit(name, Arrays.equals(actual, expected),
                    "ожидалось " + Arrays.toString(expected) + ", получено " + Arrays.toString(actual));
        } catch (Exception e) {
            recordUnit(name, false, e.getMessage());
        }
    }

    private static void checkParseError(String name, String input, String messagePart) {
        unitTotal++;
        try {
            TimesheetAnalyzer.parseHours(input);
            recordUnit(name, false, "исключение не выброшено");
        } catch (IllegalArgumentException e) {
            recordUnit(name, e.getMessage().contains(messagePart), e.getMessage());
        }
    }

    private static void checkInt(String name, String method, int[] hours, int expected) {
        unitTotal++;
        try {
            int actual;
            if (method.equals("total")) {
                actual = TimesheetAnalyzer.total(hours);
            } else if (method.equals("od")) {
                actual = TimesheetAnalyzer.overtimeDays(hours);
            } else if (method.equals("wo")) {
                actual = TimesheetAnalyzer.weeklyOvertime(hours);
            } else {
                actual = TimesheetAnalyzer.addDayOffCount(hours);
            }
            recordUnit(name, actual == expected,
                    "ожидалось " + expected + ", получено " + actual);
        } catch (Exception e) {
            recordUnit(name, false, e.getMessage());
        }
    }

    private static void checkDouble(String name, int[] hours, double expected) {
        unitTotal++;
        try {
            double actual = TimesheetAnalyzer.average(hours);
            recordUnit(name, Double.compare(actual, expected) == 0,
                    "ожидалось " + expected + ", получено " + actual);
        } catch (Exception e) {
            recordUnit(name, false, e.getMessage());
        }
    }

    private static void recordUnit(String name, boolean passed, String details) {
        if (passed) {
            unitPassed++;
            System.out.println("PASS " + name);
        } else {
            System.out.println("FAIL " + name + ": " + details);
        }
    }

    private static int[] runDataChecks() {
        int passed = 0;
        int total = 0;
        try {
            List<String> lines = Files.readAllLines(Paths.get("data/sample.csv"));
            for (String line : lines) {
                if (line.isEmpty() || line.startsWith("#")) {
                    continue;
                }
                total++;
                if (checkDataLine(line, total)) {
                    passed++;
                }
            }
        } catch (IOException e) {
            System.out.println("FAIL data: " + e.getMessage());
            total++;
        }
        return new int[]{passed, total};
    }

    private static boolean checkDataLine(String line, int number) {
        String[] fields = line.split("\\|", -1);
        if (fields.length != 3) {
            System.out.println("FAIL data/" + number + ": ожидалось 3 поля");
            return false;
        }
        String input = fields[0].equals("EMPTY") ? "" : fields[0];
        String expected = fields[1];
        try {
            int[] hours = TimesheetAnalyzer.parseHours(input);
            if (!expected.startsWith("OK:")) {
                System.out.println("FAIL data/" + number + ": ожидалась ошибка");
                return false;
            }
            boolean passed = checkExpectedMetrics(hours, expected.substring(3));
            System.out.println((passed ? "PASS " : "FAIL ") + "data/" + number
                    + " " + fields[2]);
            return passed;
        } catch (IllegalArgumentException e) {
            boolean passed = expected.startsWith("ERROR:")
                    && e.getMessage().contains(expected.substring(6));
            System.out.println((passed ? "PASS " : "FAIL ") + "data/" + number
                    + " " + fields[2]);
            return passed;
        }
    }

    private static boolean checkExpectedMetrics(int[] hours, String expected) {
        String[] metrics = expected.split(",");
        if (metrics.length != 4) {
            return false;
        }
        int sum = Integer.parseInt(metrics[0].substring("sum=".length()));
        double avg = Double.parseDouble(metrics[1].substring("avg=".length()));
        int od = Integer.parseInt(metrics[2].substring("od=".length()));
        int wo = Integer.parseInt(metrics[3].substring("wo=".length()));
        return TimesheetAnalyzer.total(hours) == sum
                && Double.compare(TimesheetAnalyzer.average(hours), avg) == 0
                && TimesheetAnalyzer.overtimeDays(hours) == od
                && TimesheetAnalyzer.weeklyOvertime(hours) == wo;
    }

    private static void runInteractiveMode() {
        System.out.println("Интерактивный режим: введите табель, exit или пустую строку для выхода.");
        Scanner scanner = new Scanner(System.in);
        while (scanner.hasNextLine()) {
            String line = scanner.nextLine();
            if (line.isEmpty() || line.equals("exit")) {
                break;
            }
            try {
                int[] hours = TimesheetAnalyzer.parseHours(line);
                System.out.println("Сумма: " + TimesheetAnalyzer.total(hours));
                System.out.printf(Locale.US, "Среднее: %.2f%n", TimesheetAnalyzer.average(hours));
                System.out.println("Дней с переработкой: " + TimesheetAnalyzer.overtimeDays(hours));
                System.out.println("Недельная переработка: " + TimesheetAnalyzer.weeklyOvertime(hours));
                System.out.println("Отгулов: " + TimesheetAnalyzer.addDayOffCount(hours));
            } catch (IllegalArgumentException e) {
                System.out.println(e.getMessage());
            }
        }
    }
}
