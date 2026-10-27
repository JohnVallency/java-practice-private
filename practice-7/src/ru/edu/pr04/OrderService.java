package ru.edu.pr04;

public class OrderService {
    public Order createOrder(Employee employee) {
        return new Order(employee);
    }
}
