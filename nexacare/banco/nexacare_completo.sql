-- =====================================================================
-- NexaCare (NexaTech Solutions) - script completo do banco MySQL 8+
-- Cria o banco, as tabelas, os relacionamentos e os dados FICTICIOS de teste.
-- Execute no MySQL Workbench / linha de comando:  mysql -u root -p < nexacare_completo.sql
-- (Opcional: a aplicacao Spring Boot ja executa estes mesmos scripts ao iniciar.)
-- =====================================================================
CREATE DATABASE IF NOT EXISTS nexacare CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci;
USE nexacare;
SET NAMES utf8mb4;
-- NexaCare - estrutura do banco (MySQL 8+). Executado automaticamente na inicialização; é idempotente.

CREATE TABLE IF NOT EXISTS especialidades (
    id   BIGINT AUTO_INCREMENT PRIMARY KEY,
    nome VARCHAR(80) NOT NULL UNIQUE
) ENGINE = InnoDB;

CREATE TABLE IF NOT EXISTS pacientes (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    nome            VARCHAR(120) NOT NULL,
    cpf             VARCHAR(11)  NOT NULL UNIQUE,
    data_nascimento DATE         NOT NULL,
    telefone        VARCHAR(20)  NOT NULL,
    email           VARCHAR(120) NOT NULL,
    observacao      VARCHAR(255),
    ativo           BOOLEAN      NOT NULL DEFAULT TRUE,
    criado_em       TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP
) ENGINE = InnoDB;

CREATE TABLE IF NOT EXISTS profissionais (
    id               BIGINT AUTO_INCREMENT PRIMARY KEY,
    nome             VARCHAR(120) NOT NULL,
    especialidade_id BIGINT       NOT NULL,
    registro         VARCHAR(30),
    telefone         VARCHAR(20)  NOT NULL,
    email            VARCHAR(120) NOT NULL,
    ativo            BOOLEAN      NOT NULL DEFAULT TRUE,
    criado_em        TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_profissional_especialidade FOREIGN KEY (especialidade_id) REFERENCES especialidades (id)
) ENGINE = InnoDB;

CREATE TABLE IF NOT EXISTS usuarios (
    id                  BIGINT AUTO_INCREMENT PRIMARY KEY,
    nome                VARCHAR(120) NOT NULL,
    email               VARCHAR(120) NOT NULL UNIQUE,
    senha_hash          VARCHAR(100) NOT NULL,
    perfil              VARCHAR(20)  NOT NULL,
    ativo               BOOLEAN      NOT NULL DEFAULT TRUE,
    profissional_id     BIGINT,
    paciente_id         BIGINT,
    notificacoes_ativas BOOLEAN      NOT NULL DEFAULT TRUE,
    tema                VARCHAR(10)  NOT NULL DEFAULT 'claro',
    criado_em           TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_usuario_profissional FOREIGN KEY (profissional_id) REFERENCES profissionais (id),
    CONSTRAINT fk_usuario_paciente FOREIGN KEY (paciente_id) REFERENCES pacientes (id)
) ENGINE = InnoDB;

CREATE TABLE IF NOT EXISTS agendamentos (
    id                    BIGINT AUTO_INCREMENT PRIMARY KEY,
    paciente_id           BIGINT      NOT NULL,
    profissional_id       BIGINT      NOT NULL,
    data                  DATE        NOT NULL,
    hora                  TIME        NOT NULL,
    status                VARCHAR(15) NOT NULL DEFAULT 'AGENDADA',
    observacao            VARCHAR(255),
    remarcacao_solicitada BOOLEAN     NOT NULL DEFAULT FALSE,
    criado_em             TIMESTAMP   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    atualizado_em         TIMESTAMP   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_agendamento_paciente FOREIGN KEY (paciente_id) REFERENCES pacientes (id),
    CONSTRAINT fk_agendamento_profissional FOREIGN KEY (profissional_id) REFERENCES profissionais (id),
    INDEX idx_agendamento_prof_data (profissional_id, data, hora),
    INDEX idx_agendamento_paciente_data (paciente_id, data, hora)
) ENGINE = InnoDB;

CREATE TABLE IF NOT EXISTS notificacoes (
    id             BIGINT AUTO_INCREMENT PRIMARY KEY,
    agendamento_id BIGINT       NOT NULL,
    tipo           VARCHAR(25)  NOT NULL,
    mensagem       VARCHAR(400) NOT NULL,
    lida           BOOLEAN      NOT NULL DEFAULT FALSE,
    criada_em      TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_notificacao_agendamento FOREIGN KEY (agendamento_id) REFERENCES agendamentos (id)
) ENGINE = InnoDB;

CREATE TABLE IF NOT EXISTS configuracoes (
    chave VARCHAR(50)  NOT NULL PRIMARY KEY,
    valor VARCHAR(100) NOT NULL
) ENGINE = InnoDB;

-- NexaCare - dados FICTÍCIOS de demonstração. INSERT IGNORE: não duplica nem sobrescreve em novas execuções.
-- Nenhum dado abaixo pertence a pessoas reais (nomes, CPFs, telefones e e-mails são inventados).
-- As consultas e notificações de exemplo são criadas pela aplicação (DadosDemoInicializador) para
-- sempre caírem em dias úteis próximos da data atual.

INSERT IGNORE INTO especialidades (id, nome) VALUES
    (1, 'Clínica Geral'), (2, 'Cardiologia'), (3, 'Dermatologia'), (4, 'Pediatria'), (5, 'Ortopedia');

INSERT IGNORE INTO configuracoes (chave, valor) VALUES
    ('horario_inicio', '08:00'), ('horario_fim', '18:00'), ('duracao_minutos', '30'),
    ('pausa_inicio', '12:00'), ('pausa_fim', '13:00');

INSERT IGNORE INTO profissionais (id, nome, especialidade_id, registro, telefone, email, ativo) VALUES
    (1, 'Dra. Helena Duarte',   1, 'CRM-SP 100001', '(11) 98000-0001', 'helena.duarte@exemplo.com',   TRUE),
    (2, 'Dr. Rafael Nogueira',  2, 'CRM-SP 100002', '(11) 98000-0002', 'rafael.nogueira@exemplo.com', TRUE),
    (3, 'Dra. Marina Castro',   3, 'CRM-SP 100003', '(11) 98000-0003', 'marina.castro@exemplo.com',   TRUE),
    (4, 'Dr. Paulo Menezes',    4, 'CRM-SP 100004', '(11) 98000-0004', 'paulo.menezes@exemplo.com',   TRUE),
    (5, 'Dra. Sofia Andrade',   5, 'CRM-SP 100005', '(11) 98000-0005', 'sofia.andrade@exemplo.com',   FALSE);

INSERT IGNORE INTO pacientes (id, nome, cpf, data_nascimento, telefone, email, observacao, ativo) VALUES
    (1, 'Ana Beatriz Lima',      '11122233396', '1990-03-14', '(11) 99000-0001', 'ana.lima@exemplo.com',      NULL, TRUE),
    (2, 'Bruno Carvalho Souza',  '22233344405', '1985-07-22', '(11) 99000-0002', 'bruno.souza@exemplo.com',   'Prefere horários pela manhã', TRUE),
    (3, 'Carla Mendes Rocha',    '33344455508', '1978-11-05', '(11) 99000-0003', 'carla.rocha@exemplo.com',   NULL, TRUE),
    (4, 'Diego Farias Pinto',    '44455566619', '2001-01-30', '(11) 99000-0004', 'diego.pinto@exemplo.com',   NULL, TRUE),
    (5, 'Elisa Martins Alves',   '55566677720', '1969-09-18', '(11) 99000-0005', 'elisa.alves@exemplo.com',   'Necessita de acessibilidade', TRUE),
    (6, 'Felipe Ramos Teixeira', '66677788830', '2015-05-09', '(11) 99000-0006', 'responsavel.felipe@exemplo.com', 'Menor de idade - contato do responsável', TRUE),
    (7, 'Gabriela Nunes Costa',  '77788899941', '1995-12-02', '(11) 99000-0007', 'gabriela.costa@exemplo.com', NULL, TRUE),
    (8, 'Henrique Barros Dias',  '12345678909', '1982-04-27', '(11) 99000-0008', 'henrique.dias@exemplo.com',  NULL, FALSE);

-- Senhas de demonstração (armazenadas apenas como hash BCrypt):
--   administrador@nexacare.com  -> Admin@123
--   recepcao@nexacare.com       -> Recep@123
--   medico@nexacare.com         -> Medico@123   (Dra. Helena Duarte)
--   cardiologista@nexacare.com  -> Medico@123   (Dr. Rafael Nogueira)
--   paciente@nexacare.com       -> Paciente@123 (Ana Beatriz Lima)
INSERT IGNORE INTO usuarios (id, nome, email, senha_hash, perfil, ativo, profissional_id, paciente_id) VALUES
    (1, 'Carlos Administrador', 'administrador@nexacare.com',
        '$2a$10$mD74aFYgX9mWUEQtJRFSb.gVHKdEw5kyKmUVzFt/XvIRnLyVYPLr.', 'ADMINISTRADOR', TRUE, NULL, NULL),
    (2, 'Renata Recepção', 'recepcao@nexacare.com',
        '$2a$10$u3SKs1VKIXS2oUhXno3/seIXnjr5IvZdnezEn1oA5/P90SG6A8CVK', 'RECEPCIONISTA', TRUE, NULL, NULL),
    (3, 'Dra. Helena Duarte', 'medico@nexacare.com',
        '$2a$10$rLADG1cGLtSwi9Oes57sFu/z3znzNtPVpiYUp5gvch5Yua4hiujie', 'PROFISSIONAL', TRUE, 1, NULL),
    (4, 'Dr. Rafael Nogueira', 'cardiologista@nexacare.com',
        '$2a$10$rLADG1cGLtSwi9Oes57sFu/z3znzNtPVpiYUp5gvch5Yua4hiujie', 'PROFISSIONAL', TRUE, 2, NULL),
    (5, 'Ana Beatriz Lima', 'paciente@nexacare.com',
        '$2a$10$fcv6iiitvaY2Eq86hVWo.ebqYT8tDFrhB3kMFRoO7t9MBTGpIVPoa', 'PACIENTE', TRUE, NULL, 1);
