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
    public void run(String... args) {
        try {
            // 1. Garante a criação da tabela de usuários caso o Hibernate ainda não a tenha criado
            jdbcTemplate.execute("""
                CREATE TABLE IF NOT EXISTS usuarios (
                    id BIGINT AUTO_INCREMENT PRIMARY KEY,
                    nome VARCHAR(120) NOT NULL,
                    email VARCHAR(120) NOT NULL UNIQUE,
                    senha_hash VARCHAR(100) NOT NULL,
                    perfil VARCHAR(20) NOT NULL,
                    ativo BOOLEAN NOT NULL DEFAULT TRUE,
                    profissional_id BIGINT,
                    paciente_id BIGINT,
                    notificacoes_ativas BOOLEAN NOT NULL DEFAULT TRUE,
                    tema VARCHAR(10) NOT NULL DEFAULT 'claro',
                    criado_em TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
                ) ENGINE = InnoDB;
            """);

            // 2. Insere os usuários de teste de forma segura (sem duplicar)
            String sqlUsuarios = """
                INSERT IGNORE INTO usuarios (id, nome, email, senha_hash, perfil, ativo) VALUES
                (1, 'Carlos Administrador', 'administrador@nexacare.com', '$2a$10$mD74aFYgX9mWUEQtJRFSb.gVHKdEw5kyKmUVzFt/XvIRnLyVYPLr.', 'ADMINISTRADOR', TRUE),
                (2, 'Renata Recepção', 'recepcao@nexacare.com', '$2a$10$u3SKs1VKIXS2oUhXno3/seIXnjr5IvZdnezEn1oA5/P90SG6A8CVK', 'RECEPCIONISTA', TRUE),
                (3, 'Dra. Helena Duarte', 'medico@nexacare.com', '$2a$10$rLADG1cGLtSwi9Oes57sFu/z3znzNtPVpiYUp5gvch5Yua4hiujie', 'PROFISSIONAL', TRUE),
                (5, 'Ana Beatriz Lima', 'paciente@nexacare.com', '$2a$10$fcv6iiitvaY2Eq86hVWo.ebqYT8tDFrhB3kMFRoO7t9MBTGpIVPoa', 'PACIENTE', TRUE);
            """;

            jdbcTemplate.execute(sqlUsuarios);
            System.out.println(">>> USUARIOS INICIAIS CARREGADOS COM SUCESSO <<<");
        } catch (Exception e) {
            // Se houver qualquer falha no banco, apenas loga e deixa a aplicação abrir a porta no Render
            System.err.println(">>> AVISO DATA INITIALIZER: " + e.getMessage());
        }
    }
}