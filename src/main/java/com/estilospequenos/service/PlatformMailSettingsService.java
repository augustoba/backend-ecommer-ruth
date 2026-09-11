package com.estilospequenos.service;

import com.estilospequenos.config.AppProperties;
import com.estilospequenos.dto.PlatformMailDtos.PlatformMailSettingsRequest;
import com.estilospequenos.model.PlatformMailSettings;
import com.estilospequenos.repository.PlatformMailSettingsRepository;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Properties;

@Service
@Transactional
public class PlatformMailSettingsService {

    private final PlatformMailSettingsRepository repo;
    private final AppProperties props;

    public PlatformMailSettingsService(PlatformMailSettingsRepository repo, AppProperties props) {
        this.repo = repo;
        this.props = props;
    }

    /** Devuelve la config; si no existe, la crea con los valores iniciales de `app.mail.*`. */
    public PlatformMailSettings get() {
        return repo.findById(PlatformMailSettings.SINGLETON_ID).orElseGet(() -> {
            PlatformMailSettings s = new PlatformMailSettings();
            s.setHost(props.getMail().getHost());
            s.setPort(props.getMail().getPort());
            s.setUsername(props.getMail().getUsername());
            s.setPassword(props.getMail().getPassword());
            s.setFromAddress(props.getMail().getFromAddress());
            return repo.save(s);
        });
    }

    /** Si `req.password()` viene en blanco, se conserva la clave que ya estaba guardada. */
    public PlatformMailSettings update(PlatformMailSettingsRequest req) {
        PlatformMailSettings s = get();
        s.setHost(req.host().trim());
        s.setPort(req.port());
        s.setUsername(req.username().trim());
        if (req.password() != null && !req.password().isBlank()) {
            s.setPassword(req.password().trim());
        }
        s.setFromAddress(req.fromAddress().trim());
        return repo.save(s);
    }

    /** Arma un `JavaMailSender` nuevo con las credenciales guardadas. */
    public JavaMailSenderImpl buildSender() {
        PlatformMailSettings cfg = get();
        JavaMailSenderImpl sender = new JavaMailSenderImpl();
        sender.setHost(cfg.getHost());
        sender.setPort(cfg.getPort());
        sender.setUsername(cfg.getUsername());
        sender.setPassword(cfg.getPassword());
        Properties props = sender.getJavaMailProperties();
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.starttls.enable", "true");
        return sender;
    }
}
