package com.estilospequenos.model;

import jakarta.persistence.*;

import java.time.Instant;

/**
 * Turno de un vendedor/cajero: lo abre al empezar y lo cierra al terminar.
 * La caja de un turno (ver {@code CashRegisterService.forShift}) sólo cuenta
 * lo que ESE usuario cobró/procesó dentro de ese rango de tiempo — así puede
 * cerrar su propio turno aunque otra persona esté vendiendo/cobrando al
 * mismo tiempo. Un usuario no puede tener dos turnos abiertos a la vez.
 */
@Entity
@Table(name = "shift")
public class Shift {

    @Id
    private String id;

    @Column(nullable = false, length = 20)
    private String userDni;

    /** Snapshot del nombre (no se rompe si el usuario cambia de nombre después). */
    @Column(nullable = false, length = 200)
    private String userName;

    @Column(nullable = false)
    private Instant openedAt = Instant.now();

    /** null = turno abierto. */
    private Instant closedAt;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getUserDni() {
        return userDni;
    }

    public void setUserDni(String userDni) {
        this.userDni = userDni;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public Instant getOpenedAt() {
        return openedAt;
    }

    public void setOpenedAt(Instant openedAt) {
        this.openedAt = openedAt;
    }

    public Instant getClosedAt() {
        return closedAt;
    }

    public void setClosedAt(Instant closedAt) {
        this.closedAt = closedAt;
    }
}
