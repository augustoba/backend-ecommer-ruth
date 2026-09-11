package com.estilospequenos.service;

import com.estilospequenos.common.BadRequestException;
import com.estilospequenos.common.ResourceNotFoundException;
import com.estilospequenos.model.AdminUser;
import com.estilospequenos.model.Permission;
import com.estilospequenos.model.Shift;
import com.estilospequenos.repository.AdminUserRepository;
import com.estilospequenos.repository.ShiftRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

@Service
@Transactional
public class ShiftService {

    private final ShiftRepository repo;
    private final AdminUserRepository users;

    public ShiftService(ShiftRepository repo, AdminUserRepository users) {
        this.repo = repo;
        this.users = users;
    }

    @Transactional(readOnly = true)
    public Shift get(String id) {
        return repo.findById(id).orElseThrow(() -> ResourceNotFoundException.of("Turno", id));
    }

    @Transactional(readOnly = true)
    public Optional<Shift> currentOpen(String userDni) {
        return repo.findByUserDniAndClosedAtIsNull(userDni);
    }

    @Transactional(readOnly = true)
    public Page<Shift> list(String userDni, Pageable pageable) {
        return userDni == null || userDni.isBlank()
                ? repo.findAllByOrderByOpenedAtDesc(pageable)
                : repo.findByUserDniOrderByOpenedAtDesc(userDni, pageable);
    }

    public Shift open(String userDni) {
        if (repo.findByUserDniAndClosedAtIsNull(userDni).isPresent()) {
            throw new BadRequestException("Ya tenés un turno abierto.");
        }
        AdminUser user = users.findByDni(userDni)
                .orElseThrow(() -> ResourceNotFoundException.of("Usuario", userDni));
        Shift shift = new Shift();
        shift.setId(UUID.randomUUID().toString());
        shift.setUserDni(userDni);
        shift.setUserName(user.getNombre() + " " + user.getApellido());
        shift.setOpenedAt(Instant.now());
        return repo.save(shift);
    }

    /** Lo puede cerrar el dueño del turno, o alguien que puede ver la caja (turnos olvidados). */
    public Shift close(String shiftId, String actingDni) {
        Shift shift = get(shiftId);
        if (shift.getClosedAt() != null) {
            throw new BadRequestException("Ese turno ya está cerrado.");
        }
        if (!shift.getUserDni().equals(actingDni)) {
            AdminUser acting = users.findByDni(actingDni).orElse(null);
            boolean canForceClose = acting != null && acting.permissions().contains(Permission.CASH_REGISTER_VIEW);
            if (!canForceClose) {
                throw new BadRequestException("Sólo el dueño del turno (o alguien que puede ver la caja) puede cerrarlo.");
            }
        }
        shift.setClosedAt(Instant.now());
        return repo.save(shift);
    }
}
