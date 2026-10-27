package ru.edu.pr04;

public class OrderItem {
    private static final int MIN_QUANTITY = 1;
    private static final int MAX_QUANTITY = 100;

    private final Product product;
    private int quantity;

    public OrderItem(Product product, int quantity) {
        if (product == null) {
            throw new IllegalArgumentException("product is null");
        }
        this.product = product;
        setQuantity(quantity);
    }

    private void setQuantity(int quantity) {
        if (quantity < MIN_QUANTITY || quantity > MAX_QUANTITY) {
            throw quantityError();
        }
        this.quantity = quantity;
    }

    public void increase(int delta) {
        if (delta <= 0) {
            throw new IllegalArgumentException("delta must be > 0");
        }
        if (delta > MAX_QUANTITY - quantity) {
            throw quantityError();
        }
        setQuantity(quantity + delta);
    }

    private IllegalArgumentException quantityError() {
        return new IllegalArgumentException(
                "quantity must be in [" + MIN_QUANTITY + ", " + MAX_QUANTITY + "]");
    }

    public Product getProduct() {
        return product;
    }

    public int getQuantity() {
        return quantity;
    }

    public double getLineTotal() {
        return product.getPrice() * quantity;
    }
}
