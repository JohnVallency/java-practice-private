package ru.edu.pr04;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;

public class Order {
    private final Employee employee;
    private final List<OrderItem> items = new ArrayList<>();
    private final List<String> history = new ArrayList<>();
    private OrderStatus status = OrderStatus.DRAFT;

    public Order(Employee employee) {
        this.employee = Objects.requireNonNull(employee, "employee is null");
    }

    public void addProduct(Product product, int quantity) {
        ensureDraft();
        OrderItem newItem = new OrderItem(product, quantity);
        for (OrderItem item : items) {
            if (item.getProduct().getId().equals(product.getId())) {
                item.increase(quantity);
                history.add("Добавлено: " + product.getId() + " x " + quantity);
                return;
            }
        }
        items.add(newItem);
        history.add("Добавлено: " + product.getId() + " x " + quantity);
    }

    public void removeProduct(String productId) {
        ensureDraft();
        Objects.requireNonNull(productId, "productId is null");
        Iterator<OrderItem> iterator = items.iterator();
        while (iterator.hasNext()) {
            if (iterator.next().getProduct().getId().equals(productId)) {
                iterator.remove();
                history.add("Удалено: " + productId);
                return;
            }
        }
        history.add("Удалено: " + productId + " (не найден)");
    }

    public double total() {
        double sum = 0;
        for (OrderItem item : items) {
            sum += item.getLineTotal();
        }
        return sum;
    }

    public void confirm() {
        ensureDraft();
        if (items.isEmpty()) {
            throw new IllegalStateException("order is empty");
        }
        status = OrderStatus.CONFIRMED;
        history.add("Заказ подтверждён");
    }

    private void ensureDraft() {
        if (status != OrderStatus.DRAFT) {
            throw new IllegalStateException("order is confirmed");
        }
    }

    public Employee getEmployee() {
        return employee;
    }

    public List<OrderItem> getItems() {
        return List.copyOf(items);
    }

    public OrderStatus getStatus() {
        return status;
    }

    public List<String> getHistory() {
        return List.copyOf(history);
    }

    public int addHistorySize(Order order) {
        return order.getHistory().size();
    }
}
