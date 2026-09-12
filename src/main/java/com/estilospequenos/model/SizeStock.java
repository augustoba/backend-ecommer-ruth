package com.estilospequenos.model;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

/** Stock de un talle puntual de un producto. */
@Embeddable
public class SizeStock {

    @Column(name = "size_value", nullable = false)
    private String size;

    @Column(nullable = false)
    private int stock;

    public SizeStock() {
    }

    public SizeStock(String size, int stock) {
        this.size = size;
        this.stock = stock;
    }

    public String getSize() {
        return size;
    }

    public void setSize(String size) {
        this.size = size;
    }

    public int getStock() {
        return stock;
    }

    public void setStock(int stock) {
        this.stock = stock;
    }
}
