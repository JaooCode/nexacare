package com.nexatech.nexacare;

import org.springframework.boot.CommandLineRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
public class DataInitializer implements CommandLineRunner {

    private final JdbcTemplate jdbcTemplate;

    public DataInitializer(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public void run(String... args) throws Exception {
        String sql = """
            INSERT IGNORE INTO usuarios (id, nome, email, senha_hash, perfil, ativo) VALUES
            (1, 'Carlos Administrador', 'administrador@nexacare.com', '$2a$10$mD74aFYgX9mWUEQtJRFSb.gVHKdEw5kyKmUVzFt/XvIRnLyVYPLr.', 'ADMINISTRADOR', TRUE),
            (2, 'Renata Recepção', 'recepcao@nexacare.com', '$2a$10$u3SKs1VKIXS2oUhXno3/seIXnjr5IvZdnezEn1oA5/P90SG6A8CVK', 'RECEPCIONISTA', TRUE),
            (3, 'Dra. Helena Duarte', 'medico@nexacare.com', '$2a$10$rLADG1cGLtSwi9Oes57sFu/z3znzNtPVpiYUp5gvch5Yua4hiujie', 'PROFISSIONAL', TRUE),
            (5, 'Ana Beatriz Lima', 'paciente@nexacare.com', '$2a$10$fcv6iiitvaY2Eq86hVWo.ebqYT8tDFrhB3kMFRoO7t9MBTGpIVPoa', 'PACIENTE', TRUE);
            """;

        jdbcTemplate.execute(sql);
        System.out.println(">>> USUARIOS INICIAIS INSERIDOS COM SUCESSO <<<");
    }
}