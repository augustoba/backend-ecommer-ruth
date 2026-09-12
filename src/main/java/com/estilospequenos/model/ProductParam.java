package com.estilospequenos.model;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

import java.util.Objects;

/** Una opción de parametría elegida por un producto (grupo + opción). */
@Embeddable
public class ProductParam {

    @Column(name = "group_id", nullable = false)
    private String groupId;

    @Column(name = "option_id", nullable = false)
    private String optionId;

    public ProductParam() {
    }

    public ProductParam(String groupId, String optionId) {
        this.groupId = groupId;
        this.optionId = optionId;
    }

    public String getGroupId() {
        return groupId;
    }

    public void setGroupId(String groupId) {
        this.groupId = groupId;
    }

    public String getOptionId() {
        return optionId;
    }

    public void setOptionId(String optionId) {
        this.optionId = optionId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ProductParam that)) return false;
        return Objects.equals(groupId, that.groupId) && Objects.equals(optionId, that.optionId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(groupId, optionId);
    }
}
