package ru.edu.pr02;

public class TimesheetAnalyzer {
    private static final int DAYS_IN_WEEK = 5;
    private static final int DAY_OFF = -1;
    private static final int MAX_HOURS = 16;
    private static final int DAILY_NORMAL_HOURS = 8;
    private static final int WEEKLY_NORMAL_HOURS = 40;

    private TimesheetAnalyzer() {
    }

    public static int[] parseHours(String line) {
        if (line == null) {
            throw new IllegalArgumentException("Ошибка: структура – строка отсутствует");
        }
        String[] parts = line.split(";", -1);
        if (parts.length != DAYS_IN_WEEK) {
            throw new IllegalArgumentException(
                    "Ошибка: структура – ожидалось 5 значений, получено " + parts.length);
        }

        int[] result = new int[DAYS_IN_WEEK];
        for (int i = 0; i < parts.length; i++) {
            result[i] = parseValue(parts[i], i + 1);
        }
        return result;
    }

    private static int parseValue(String part, int position) {
        if (!part.matches("^(-1|0|[1-9][0-9]*)$")) {
            throw formatError(part, position);
        }
        int value;
        try {
            value = Integer.parseInt(part);
        } catch (NumberFormatException e) {
            throw formatError(part, position);
        }
        if (value < DAY_OFF || value > MAX_HOURS) {
            throw new IllegalArgumentException("Ошибка: позиция " + position
                    + " – значение " + value + " вне диапазона [-1, 16]");
        }
        return value;
    }

    private static IllegalArgumentException formatError(String value, int position) {
        return new IllegalArgumentException("Ошибка: позиция " + position
                + " – значение \"" + value + "\" не является целым числом");
    }

    public static int total(int[] hours) {
        validateHours(hours);
        int sum = 0;
        for (int value : hours) {
            if (value != DAY_OFF) {
                sum += value;
            }
        }
        return sum;
    }

    public static double average(int[] hours) {
        validateHours(hours);
        int workingDays = DAYS_IN_WEEK - addDayOffCount(hours);
        if (workingDays == 0) {
            return 0.0;
        }
        double value = (double) total(hours) / workingDays;
        return Math.round(value * 100.0) / 100.0;
    }

    public static int overtimeDays(int[] hours) {
        validateHours(hours);
        int count = 0;
        for (int value : hours) {
            if (value > DAILY_NORMAL_HOURS) {
                count++;
            }
        }
        return count;
    }

    public static int weeklyOvertime(int[] hours) {
        return Math.max(0, total(hours) - WEEKLY_NORMAL_HOURS);
    }

    public static int addDayOffCount(int[] hours) {
        validateHours(hours);
        int count = 0;
        for (int value : hours) {
            if (value == DAY_OFF) {
                count++;
            }
        }
        return count;
    }

    private static void validateHours(int[] hours) {
        if (hours == null) {
            throw new IllegalArgumentException("hours is null");
        }
        if (hours.length != DAYS_IN_WEEK) {
            throw new IllegalArgumentException(
                    "hours length must be 5, got " + hours.length);
        }
    }
}
