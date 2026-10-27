package ru.edu.pr04;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import java.util.Scanner;

public class SelfCheck {
    private static int passed;
    private static int total;

    public static void main(String[] args) {
        runUnitChecks();
        System.out.println("Unit: " + passed + "/" + total + " passed");
        boolean unitOk = passed == total;
        passed = 0;
        total = 0;
        runDataChecks();
        System.out.println("Data-driven: " + passed + "/" + total + " passed");
        if (!unitOk || passed != total) System.exit(1);
        runInteractiveMode();
    }

    private static Order newOrder() {
        return new OrderService().createOrder(new Employee("E01", "Иван Иванов"));
    }

    private static void record(String name, boolean ok, String details) {
        total++;
        if (ok) passed++;
        System.out.println((ok ? "PASS " : "FAIL ") + name + (ok ? "" : ": " + details));
    }

    private static void runUnitChecks() {
        for (int number = 1; number <= 43; number++) {
            String expected = expectedError(number);
            try {
                boolean ok = runScenario(number);
                record("unit/" + number, ok && expected == null, "неверный результат или нет исключения");
            } catch (RuntimeException e) {
                record("unit/" + number, e.getClass().getSimpleName().equals(expected), e.toString());
            }
        }
    }

    private static String expectedError(int number) {
        return switch (number) {
            case 2, 3, 5, 10, 19, 20, 21, 22, 23, 24, 25, 26, 27, 28 -> "IllegalArgumentException";
            case 9, 16, 18, 31 -> "IllegalStateException";
            case 17, 39 -> "UnsupportedOperationException";
            case 29, 30 -> "NullPointerException";
            default -> null;
        };
    }

    private static boolean runScenario(int number) {
        Product product = new Product("P1", "Mouse", 1000);
        Order order = newOrder();
        if (number > 18) return extraScenario(number, product, order);
        if (number <= 6) return productScenario(number, product);
        if (number <= 12) return changeScenario(number, product, order);
        return orderScenario(number, product, order);
    }

    private static boolean productScenario(int number, Product product) {
        switch (number) {
            // Product: корректное создание.
            case 1: return product.getPrice() == 1000;
            // Product: цена 0 запрещена.
            case 2: new Product("P2", "X", 0); break;
            // Product: отсутствующий id запрещён.
            case 3: new Product(null, "X", 100); break;
            // OrderItem: корректное количество.
            case 4: return new OrderItem(product, 5).getQuantity() == 5;
            // OrderItem.increase: превышение 100.
            case 5: new OrderItem(product, 5).increase(96); break;
            // OrderItem.getLineTotal: цена умножается на количество.
            case 6: return new OrderItem(product, 3).getLineTotal() == 3000.0;
            default: throw new IllegalArgumentException("unknown scenario");
        }
        return false;
    }

    private static boolean changeScenario(int number, Product product, Order order) {
        order.addProduct(product, number == 10 ? 50 : 2);
        switch (number) {
            // Order.addProduct: новая позиция.
            case 7: return order.getItems().size() == 1;
            // Order.addProduct: объединение одинакового id.
            case 8:
                order.addProduct(new Product("P1", "Mouse", 1000), 3);
                return order.getItems().size() == 1 && order.getItems().get(0).getQuantity() == 5;
            // Order.addProduct: изменение подтверждённого заказа запрещено.
            case 9: order.confirm(); order.addProduct(product, 1); break;
            // Order.addProduct: объединение не обходит лимит.
            case 10: order.addProduct(product, 51); break;
            // Order.removeProduct: удаление существующего товара.
            case 11: order.removeProduct("P1"); return order.getItems().isEmpty();
            // Order.removeProduct: отсутствующий товар не является ошибкой.
            case 12: order.removeProduct("P99"); return order.getItems().size() == 1;
            default: throw new IllegalArgumentException("unknown scenario");
        }
        return false;
    }

    private static boolean orderScenario(int number, Product product, Order order) {
        switch (number) {
            // Order.total: пустой заказ.
            case 13: return order.total() == 0.0;
            // Order.total: сумма нескольких позиций.
            case 14:
                order.addProduct(product, 2);
                order.addProduct(new Product("P2", "Keyboard", 500), 3);
                return order.total() == 3500.0;
            // Order.confirm: переход в CONFIRMED.
            case 15:
                order.addProduct(product, 1); order.confirm();
                return order.getStatus() == OrderStatus.CONFIRMED;
            // Order.confirm: повторное подтверждение запрещено.
            case 16: order.addProduct(product, 1); order.confirm(); order.confirm(); break;
            // Order.getItems: коллекция неизменяема.
            case 17: order.addProduct(product, 1); order.getItems().add(new OrderItem(product, 1)); break;
            // Order.confirm: пустой заказ запрещён.
            case 18: order.confirm(); break;
            default: throw new IllegalArgumentException("unknown scenario");
        }
        return false;
    }

    private static OrderItem findItem(Order order, String id) {
        for (OrderItem item : order.getItems()) {
            if (item.getProduct().getId().equals(id)) return item;
        }
        return null;
    }

    private static void runDataChecks() {
        Order order = newOrder();
        try {
            List<String> lines = Files.readAllLines(Paths.get("data/sample.csv"), StandardCharsets.UTF_8);
            boolean headerSeen = false;
            String totals = null;
            int number = 0;
            for (String line : lines) {
                if (line.isEmpty() || line.startsWith("#")) continue;
                if (!headerSeen) {
                    headerSeen = true;
                    record("data/header", line.equals("productId;name;price;quantity;expected"), line);
                } else if (line.startsWith("TOTAL;")) {
                    totals = line;
                } else {
                    checkDataProduct(order, line, ++number);
                }
            }
            checkDataTotal(order, totals);
            printOrder(order);
        } catch (IOException e) {
            record("data/file", false, e.getMessage());
        }
    }

    private static void checkDataProduct(Order order, String line, int number) {
        String[] parts = line.split(";", -1);
        if (parts.length != 5) {
            record("data/" + number, false, "ожидалось 5 колонок");
            return;
        }
        int oldSize = order.getItems().size();
        double oldTotal = order.total();
        int oldHistory = order.addHistorySize(order);
        try {
            Product product = parseProduct(parts);
            boolean existed = findItem(order, product.getId()) != null;
            order.addProduct(product, Integer.parseInt(parts[3]));
            int quantity = findItem(order, product.getId()).getQuantity();
            String actual = "OK:" + (existed ? "merged," : "") + "qty=" + quantity;
            record("data/" + number, actual.equals(parts[4])
                    && order.addHistorySize(order) == oldHistory + 1, "получено " + actual);
        } catch (IllegalArgumentException e) {
            boolean ok = parts[4].equals("SKIP:" + e.getMessage())
                    && oldSize == order.getItems().size() && oldTotal == order.total()
                    && oldHistory == order.addHistorySize(order);
            record("data/" + number, ok, e.getMessage());
        }
    }

    private static Product parseProduct(String[] parts) {
        for (int i = 0; i < 4; i++) {
            if (!parts[i].equals(parts[i].strip())) {
                throw new IllegalArgumentException("surrounding spaces are not allowed");
            }
        }
        return new Product(parts[0], parts[1], Double.parseDouble(parts[2]));
    }

    private static void checkDataTotal(Order order, String line) {
        try {
            if (line == null) throw new IllegalArgumentException("нет TOTAL");
            String[] parts = line.split(";", -1);
            int positions = Integer.parseInt(parts[1].substring("positions=".length()));
            double sum = Double.parseDouble(parts[2].substring("sum=".length()));
            record("data/TOTAL", order.getItems().size() == positions
                    && Math.abs(order.total() - sum) < 0.000001, "неверный состав или сумма");
            int history = Integer.parseInt(parts[3].substring("history=".length()));
            record("data/HISTORY", order.addHistorySize(order) == history, "неверная длина истории");
        } catch (IllegalArgumentException | IndexOutOfBoundsException e) {
            record("data/TOTAL", false, e.getMessage());
        }
    }

    private static void printOrder(Order order) {
        for (OrderItem item : order.getItems()) {
            System.out.println(item.getProduct().getId() + " x " + item.getQuantity()
                    + " = " + item.getLineTotal());
        }
        System.out.println("Итог: " + order.total());
        System.out.println("Статус: " + order.getStatus());
        System.out.println("Позиций: " + order.getItems().size());
        System.out.println("Записей истории: " + order.addHistorySize(order));
    }

    private static boolean extraScenario(int number, Product product, Order order) {
        if (number <= 30) return validationScenario(number, product, order);
        if (number == 31) {
            // Order.removeProduct: запрет после подтверждения.
            order.addProduct(product, 1); order.confirm(); order.removeProduct("P1");
            return false;
        }
        if (number == 32) {
            // OrderItem: обе границы включены.
            return new OrderItem(product, 1).getQuantity() == 1
                    && new OrderItem(product, 100).getQuantity() == 100;
        }
        return historyScenario(number, product, order);
    }

    private static boolean validationScenario(int number, Product product, Order order) {
        switch (number) {
            // Product: пустой id.
            case 19: new Product(" ", "Mouse", 1000); break;
            // Product: отсутствующее имя.
            case 20: new Product("P2", null, 1000); break;
            // Product: пустое имя.
            case 21: new Product("P2", " ", 1000); break;
            // Product: отрицательная цена.
            case 22: new Product("P2", "Mouse", -1); break;
            // OrderItem: отсутствующий товар.
            case 23: new OrderItem(null, 1); break;
            // OrderItem: ниже нижней границы.
            case 24: new OrderItem(product, 0); break;
            // OrderItem: выше верхней границы.
            case 25: new OrderItem(product, 101); break;
            // OrderItem.increase: нулевое приращение.
            case 26: new OrderItem(product, 1).increase(0); break;
            // OrderItem.increase: отрицательное приращение.
            case 27: new OrderItem(product, 1).increase(-1); break;
            // OrderItem.increase: переполнение int не обходит лимит.
            case 28: new OrderItem(product, 1).increase(Integer.MAX_VALUE); break;
            // Order: отсутствующий сотрудник.
            case 29: new Order(null); break;
            // Order.removeProduct: отсутствующий id.
            case 30: order.removeProduct(null); break;
            default: throw new IllegalArgumentException("unknown scenario");
        }
        return false;
    }

    private static boolean historyScenario(int number, Product product, Order order) {
        if (number >= 39) return snapshotScenario(number, product, order);
        switch (number) {
            // Часть А: успешное добавление создаёт запись.
            case 33:
                order.addProduct(product, 2);
                return order.getHistory().equals(List.of("Добавлено: P1 x 2"));
            // Часть А: объединение также создаёт запись.
            case 34:
                order.addProduct(product, 2); order.addProduct(product, 3);
                return order.getHistory().equals(List.of("Добавлено: P1 x 2", "Добавлено: P1 x 3"));
            // Часть А: remove и confirm сохраняют порядок операций.
            case 35:
                order.addProduct(product, 1); order.removeProduct("P1");
                order.addProduct(product, 2); order.confirm();
                return order.getHistory().equals(List.of("Добавлено: P1 x 1", "Удалено: P1",
                        "Добавлено: P1 x 2", "Заказ подтверждён"));
            // Часть А: отказ не меняет количество и историю.
            case 36: return checkFailedChanges(order, product);
            // Часть Б: у нового заказа нет записей.
            case 37: return order.addHistorySize(order) == 0;
            // Часть Б: три успешных вызова дают три записи.
            case 38:
                order.addProduct(product, 1); order.removeProduct("P1"); order.addProduct(product, 1);
                return order.addHistorySize(order) == 3;
            default: throw new IllegalArgumentException("unknown scenario");
        }
    }

    private static boolean checkFailedChanges(Order order, Product product) {
        order.addProduct(product, 100);
        try {
            order.addProduct(product, 1);
            return false;
        } catch (IllegalArgumentException e) {
            if (order.getItems().get(0).getQuantity() != 100 || order.addHistorySize(order) != 1) {
                return false;
            }
        }
        order.confirm();
        try {
            order.removeProduct("P1");
            return false;
        } catch (IllegalStateException e) {
            return order.getItems().size() == 1 && order.addHistorySize(order) == 2;
        }
    }

    private static boolean snapshotScenario(int number, Product product, Order order) {
        switch (number) {
            // getHistory: внешняя модификация запрещена.
            case 39: order.getHistory().add("Поддельная запись"); return false;
            // getHistory: ранее выданная копия не меняется.
            case 40:
                List<String> history = order.getHistory(); order.addProduct(product, 1);
                return history.isEmpty() && order.addHistorySize(order) == 1;
            // getItems: ранее выданный состав не меняется.
            case 41:
                List<OrderItem> items = order.getItems(); order.addProduct(product, 1);
                return items.isEmpty() && order.getItems().size() == 1;
            // removeProduct: отсутствующий товар — успешный вызов с записью.
            case 42:
                order.removeProduct("P99");
                return order.getHistory().equals(List.of("Удалено: P99 (не найден)"));
            // OrderItem.increase: неудачное приращение не меняет количество.
            case 43:
                OrderItem item = new OrderItem(product, 100);
                try { item.increase(1); return false; }
                catch (IllegalArgumentException e) { return item.getQuantity() == 100; }
            default: throw new IllegalArgumentException("unknown scenario");
        }
    }

    private static void runInteractiveMode() {
        Order order = newOrder();
        System.out.println("Команды: add, remove, total, items, confirm, status, exit");
        Scanner scanner = new Scanner(System.in, StandardCharsets.UTF_8);
        while (scanner.hasNextLine()) {
            String line = scanner.nextLine();
            if (line.equals("exit")) break;
            if (line.isBlank()) continue;
            try {
                executeCommand(order, line);
            } catch (IllegalArgumentException | IllegalStateException e) {
                System.out.println("Ошибка: " + e.getMessage());
            }
        }
    }

    private static void executeCommand(Order order, String line) {
        String[] parts = line.strip().split("\\s+");
        switch (parts[0]) {
            case "add":
                if (parts.length < 5) throw new IllegalArgumentException("add <id> <name> <price> <qty>");
                StringBuilder name = new StringBuilder(parts[2]);
                for (int i = 3; i < parts.length - 2; i++) name.append(" ").append(parts[i]);
                Product product = new Product(parts[1], name.toString(),
                        Double.parseDouble(parts[parts.length - 2]));
                order.addProduct(product, Integer.parseInt(parts[parts.length - 1]));
                System.out.println("Товар добавлен");
                break;
            case "remove":
                if (parts.length != 2) throw new IllegalArgumentException("remove <id>");
                order.removeProduct(parts[1]);
                System.out.println("Удаление обработано");
                break;
            default:
                executeSimpleCommand(order, parts);
        }
    }

    private static void executeSimpleCommand(Order order, String[] parts) {
        if (parts.length != 1) throw new IllegalArgumentException("лишние аргументы команды");
        switch (parts[0]) {
            case "total": System.out.println("Итог: " + order.total()); break;
            case "items": printOrder(order); break;
            case "confirm": order.confirm(); System.out.println("Заказ подтверждён"); break;
            case "status": System.out.println("Статус: " + order.getStatus()); break;
            default: throw new IllegalArgumentException("неизвестная команда");
        }
    }
}
