package com.estilospequenos.product;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Una opción de parametría elegida por un producto (grupo + opción). */
@Embeddable
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
public class ProductParam {

    @Column(name = "group_id", nullable = false)
    private String groupId;

    @Column(name = "option_id", nullable = false)
    private String optionId;
}
