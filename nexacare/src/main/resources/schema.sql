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
