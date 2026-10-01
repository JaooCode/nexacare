package com.nexatech.nexacare;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import java.time.DayOfWeek;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class NexaCareApiTest {
    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;

    static String adm, rec, med, cardio, pac;
    static long consultaId;
    static LocalDate dia = proximoDiaUtil(LocalDate.now().plusDays(20));

    static LocalDate proximoDiaUtil(LocalDate d) {
        while (d.getDayOfWeek() == DayOfWeek.SATURDAY || d.getDayOfWeek() == DayOfWeek.SUNDAY) d = d.plusDays(1);
        return d;
    }

    String login(String email, String senha) throws Exception {
        String r = mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"" + email + "\",\"senha\":\"" + senha + "\"}"))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        return json.readTree(r).get("token").asText();
    }

    ResultActions chamar(MockHttpServletRequestBuilder b, String token, String corpo) throws Exception {
        b.header("Authorization", "Bearer " + token);
        if (corpo != null) b.contentType(MediaType.APPLICATION_JSON).content(corpo);
        return mvc.perform(b);
    }

    JsonNode corpo(ResultActions r) throws Exception {
        return json.readTree(r.andReturn().getResponse().getContentAsString());
    }

    String agendamento(Long pacienteId, long profId, LocalDate data, String hora) {
        return "{\"pacienteId\":" + pacienteId + ",\"profissionalId\":" + profId + ",\"data\":\"" + data
                + "\",\"hora\":\"" + hora + "\",\"observacao\":\"teste\"}";
    }

    @Test @Order(1)
    void loginEPerfis() throws Exception {
        adm = login("administrador@nexacare.com", "Admin@123");
        rec = login("recepcao@nexacare.com", "Recep@123");
        med = login("medico@nexacare.com", "Medico@123");
        cardio = login("cardiologista@nexacare.com", "Medico@123");
        pac = login("paciente@nexacare.com", "Paciente@123");
        assertEquals("PROFISSIONAL", corpo(chamar(get("/api/auth/me"), med, null).andExpect(status().isOk())).get("perfil").asText());

        mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"recepcao@nexacare.com\",\"senha\":\"errada\"}"))
                .andExpect(status().isUnauthorized());
        mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON).content("{\"email\":\"\"}"))
                .andExpect(status().isBadRequest());
        mvc.perform(get("/api/pacientes")).andExpect(status().isUnauthorized());
    }

    @Test @Order(2)
    void permissoesPorPerfil() throws Exception {
        chamar(get("/api/pacientes"), rec, null).andExpect(status().isOk());
        chamar(get("/api/pacientes"), adm, null).andExpect(status().isForbidden());
        chamar(get("/api/pacientes"), pac, null).andExpect(status().isForbidden());
        chamar(get("/api/usuarios"), rec, null).andExpect(status().isForbidden());
        chamar(get("/api/usuarios"), adm, null).andExpect(status().isOk());
        chamar(post("/api/agendamentos"), adm, agendamento(1L, 1, dia, "09:00")).andExpect(status().isForbidden());
        chamar(post("/api/agendamentos"), med, agendamento(1L, 1, dia, "09:00")).andExpect(status().isForbidden());
        chamar(put("/api/configuracoes"), rec,
                "{\"inicio\":\"08:00\",\"fim\":\"18:00\",\"duracaoMinutos\":30,\"pausaInicio\":\"12:00\",\"pausaFim\":\"13:00\"}")
                .andExpect(status().isForbidden());
    }

    @Test @Order(3)
    void pacienteCrudEValidacoes() throws Exception {
        String base = "\"nome\":\"Paciente Teste\",\"dataNascimento\":\"1990-01-01\",\"telefone\":\"(11) 97777-0000\",\"email\":\"teste@exemplo.com\"";
        chamar(post("/api/pacientes"), rec, "{" + base + ",\"cpf\":\"111.111.111-11\"}").andExpect(status().isBadRequest());
        chamar(post("/api/pacientes"), rec, "{" + base + ",\"cpf\":\"11122233396\"}").andExpect(status().isConflict());
        chamar(post("/api/pacientes"), rec, "{\"nome\":\"\"}").andExpect(status().isBadRequest());
        chamar(post("/api/pacientes"), rec, "{" + base.replace("teste@exemplo.com", "invalido") + ",\"cpf\":\"98765432100\"}")
                .andExpect(status().isBadRequest());

        JsonNode criado = corpo(chamar(post("/api/pacientes"), rec, "{" + base + ",\"cpf\":\"987.654.321-00\"}")
                .andExpect(status().isCreated()));
        long id = criado.get("id").asLong();
        assertEquals("987.654.321-00", criado.get("cpf").asText());
        chamar(put("/api/pacientes/" + id), rec, "{" + base.replace("Teste", "Editado") + ",\"cpf\":\"98765432100\"}")
                .andExpect(status().isOk());
        assertEquals(1, corpo(chamar(get("/api/pacientes?busca=Editado"), rec, null)).size());
        assertEquals(1, corpo(chamar(get("/api/pacientes?busca=987.654"), rec, null)).size());
        chamar(delete("/api/pacientes/" + id), rec, null).andExpect(status().isOk());
        assertEquals(0, corpo(chamar(get("/api/pacientes?busca=Editado"), rec, null)).size());
        assertEquals(1, corpo(chamar(get("/api/pacientes?busca=Editado&incluirInativos=true"), rec, null)).size());
    }

    @Test @Order(4)
    void fluxoPrincipalDeAgendamento() throws Exception {
        // grade mostra o horário livre
        JsonNode grade = corpo(chamar(get("/api/agenda/grade?data=" + dia + "&profissionalId=1"), rec, null));
        assertTrue(grade.get("diaUtil").asBoolean());
        assertTrue(grade.get("profissionais").get(0).get("slots").toString().contains("\"LIVRE\""));

        JsonNode c = corpo(chamar(post("/api/agendamentos"), rec, agendamento(2L, 1, dia, "09:00")).andExpect(status().isCreated()));
        consultaId = c.get("id").asLong();
        assertEquals("AGENDADA", c.get("status").asText());

        // aparece na agenda como ocupado e o horário some dos livres
        String livres = chamar(get("/api/agendamentos/horarios-livres?profissionalId=1&data=" + dia), rec, null)
                .andReturn().getResponse().getContentAsString();
        assertFalse(livres.contains("09:00"));
        assertTrue(chamar(get("/api/agenda/grade?data=" + dia + "&profissionalId=1"), rec, null)
                .andReturn().getResponse().getContentAsString().contains("Bruno Carvalho Souza"));

        // confirmação gerada
        assertTrue(chamar(get("/api/notificacoes"), rec, null).andReturn().getResponse().getContentAsString()
                .contains("foi agendada"));
    }

    @Test @Order(5)
    void regrasDeConflitoEValidacao() throws Exception {
        chamar(post("/api/agendamentos"), rec, agendamento(3L, 1, dia, "09:00")).andExpect(status().isConflict());   // profissional
        chamar(post("/api/agendamentos"), rec, agendamento(2L, 2, dia, "09:00")).andExpect(status().isConflict());   // paciente
        chamar(post("/api/agendamentos"), rec, "{\"profissionalId\":1,\"data\":\"" + dia + "\",\"hora\":\"10:00\"}")
                .andExpect(status().isBadRequest());                                                                  // sem paciente
        chamar(post("/api/agendamentos"), rec, "{\"pacienteId\":3,\"data\":\"" + dia + "\",\"hora\":\"10:00\"}")
                .andExpect(status().isBadRequest());                                                                  // sem profissional
        chamar(post("/api/agendamentos"), rec, "{\"pacienteId\":3,\"profissionalId\":1,\"hora\":\"10:00\"}")
                .andExpect(status().isBadRequest());                                                                  // sem data
        chamar(post("/api/agendamentos"), rec, "{\"pacienteId\":3,\"profissionalId\":1,\"data\":\"" + dia + "\"}")
                .andExpect(status().isBadRequest());                                                                  // sem horário
        chamar(post("/api/agendamentos"), rec, agendamento(3L, 1, LocalDate.now().minusDays(1), "10:00")).andExpect(status().isBadRequest());
        chamar(post("/api/agendamentos"), rec, agendamento(3L, 1, dia, "07:00")).andExpect(status().isBadRequest());
        chamar(post("/api/agendamentos"), rec, agendamento(3L, 1, dia, "12:00")).andExpect(status().isBadRequest()); // pausa
        chamar(post("/api/agendamentos"), rec, agendamento(3L, 1, dia, "10:10")).andExpect(status().isBadRequest());
        LocalDate sabado = dia;
        while (sabado.getDayOfWeek() != DayOfWeek.SATURDAY) sabado = sabado.plusDays(1);
        chamar(post("/api/agendamentos"), rec, agendamento(3L, 1, sabado, "10:00")).andExpect(status().isBadRequest());
        chamar(post("/api/agendamentos"), rec, agendamento(8L, 1, dia, "10:00")).andExpect(status().isBadRequest()); // paciente inativo
        chamar(post("/api/agendamentos"), rec, agendamento(3L, 5, dia, "10:00")).andExpect(status().isBadRequest()); // profissional inativo
    }

    @Test @Order(6)
    void escopoDoProfissionalEDoPaciente() throws Exception {
        // Dra. Helena vê a consulta; Dr. Rafael não
        assertTrue(chamar(get("/api/agendamentos?inicio=" + dia + "&fim=" + dia), med, null).andReturn()
                .getResponse().getContentAsString().contains("\"id\":" + consultaId));
        assertFalse(chamar(get("/api/agendamentos?inicio=" + dia + "&fim=" + dia), cardio, null).andReturn()
                .getResponse().getContentAsString().contains("\"id\":" + consultaId));
        chamar(get("/api/agendamentos/" + consultaId), cardio, null).andExpect(status().isForbidden());
        // profissional não pode escolher outra agenda via parâmetro
        assertFalse(chamar(get("/api/agendamentos?profissionalId=1&inicio=" + dia + "&fim=" + dia), cardio, null)
                .andReturn().getResponse().getContentAsString().contains("\"id\":" + consultaId));
        // paciente Ana não vê a consulta do Bruno e a grade não revela nomes
        chamar(get("/api/agendamentos/" + consultaId), pac, null).andExpect(status().isForbidden());
        String gradePac = chamar(get("/api/agenda/grade?data=" + dia + "&profissionalId=1"), pac, null)
                .andReturn().getResponse().getContentAsString();
        assertFalse(gradePac.contains("Bruno"));
        assertTrue(gradePac.contains("OCUPADO"));
        // profissional vê paciente sem CPF/e-mail; sem acesso a paciente sem vínculo
        JsonNode p = corpo(chamar(get("/api/pacientes/2"), med, null).andExpect(status().isOk()));
        assertTrue(p.get("cpf").isNull());
        chamar(get("/api/pacientes/8"), med, null).andExpect(status().isForbidden());
    }

    @Test @Order(7)
    void confirmarRemarcarEHorarioLiberadoAoCancelar() throws Exception {
        chamar(patch("/api/agendamentos/" + consultaId + "/status"), rec, "{\"status\":\"CONFIRMADA\"}")
                .andExpect(status().isOk());
        // remarcar para outro horário (troca de horário libera o antigo)
        JsonNode r = corpo(chamar(put("/api/agendamentos/" + consultaId), rec, agendamento(2L, 1, dia, "10:00")).andExpect(status().isOk()));
        assertEquals("AGENDADA", r.get("status").asText());
        chamar(post("/api/agendamentos"), rec, agendamento(3L, 1, dia, "09:00")).andExpect(status().isCreated());   // 09:00 liberado
        assertTrue(chamar(get("/api/notificacoes"), rec, null).andReturn().getResponse().getContentAsString().contains("remarcada"));

        // cancelar: some das consultas ativas, mas aparece filtrando por canceladas; horário volta a ficar livre
        chamar(delete("/api/agendamentos/" + consultaId), rec, null).andExpect(status().isOk());
        assertFalse(chamar(get("/api/agendamentos?inicio=" + dia + "&fim=" + dia), rec, null).andReturn()
                .getResponse().getContentAsString().contains("\"id\":" + consultaId));
        assertTrue(chamar(get("/api/agendamentos?status=CANCELADA&inicio=" + dia + "&fim=" + dia), rec, null).andReturn()
                .getResponse().getContentAsString().contains("\"id\":" + consultaId));
        chamar(post("/api/agendamentos"), rec, agendamento(4L, 1, dia, "10:00")).andExpect(status().isCreated());
        chamar(delete("/api/agendamentos/" + consultaId), rec, null).andExpect(status().isBadRequest()); // já cancelada
    }

    @Test @Order(8)
    void pacienteAgendaCancelaESolicitaRemarcacao() throws Exception {
        JsonNode c = corpo(chamar(post("/api/agendamentos"), pac, agendamento(5L, 2, dia, "14:00")).andExpect(status().isCreated()));
        assertEquals(1, c.get("pacienteId").asLong(), "paciente logado prevalece sobre o id enviado");
        long id = c.get("id").asLong();
        chamar(post("/api/agendamentos/" + id + "/solicitar-remarcacao"), pac, null).andExpect(status().isOk());
        chamar(post("/api/agendamentos/" + id + "/solicitar-remarcacao"), pac, null).andExpect(status().isBadRequest());
        chamar(put("/api/agendamentos/" + id), pac, agendamento(1L, 2, dia, "15:00")).andExpect(status().isForbidden());
        chamar(delete("/api/agendamentos/" + id), pac, null).andExpect(status().isOk());
    }

    @Test @Order(9)
    void profissionalMarcaRealizadaSomenteQuandoPermitido() throws Exception {
        JsonNode futura = corpo(chamar(post("/api/agendamentos"), rec, agendamento(6L, 1, dia, "16:00")));
        long id = futura.get("id").asLong();
        chamar(patch("/api/agendamentos/" + id + "/status"), med, "{\"status\":\"REALIZADA\"}").andExpect(status().isBadRequest());
        chamar(patch("/api/agendamentos/" + id + "/status"), rec, "{\"status\":\"REALIZADA\"}").andExpect(status().isForbidden());
        chamar(patch("/api/agendamentos/" + id + "/status"), cardio, "{\"status\":\"CANCELADA\"}").andExpect(status().isForbidden());

        // consulta de hoje criada pela carga demo (Helena, paciente Bruno, 08:30) pode ser marcada como realizada
        JsonNode hoje = corpo(chamar(get("/api/agendamentos?inicio=" + LocalDate.now() + "&fim=" + LocalDate.now()), med, null));
        if (hoje.size() > 0) {
            long idHoje = hoje.get(0).get("id").asLong();
            chamar(patch("/api/agendamentos/" + idHoje + "/status"), med, "{\"status\":\"REALIZADA\"}").andExpect(status().isOk());
            chamar(patch("/api/agendamentos/" + idHoje + "/status"), med, "{\"status\":\"REALIZADA\"}").andExpect(status().isBadRequest());
        }
    }

    @Test @Order(10)
    void profissionaisEUsuarios() throws Exception {
        String prof = "{\"nome\":\"Dr. Teste Novo\",\"especialidadeId\":2,\"registro\":\"CRM 1\",\"telefone\":\"(11) 95555-1111\","
                + "\"email\":\"novo.prof@exemplo.com\",\"ativo\":true,\"senhaInicial\":\"Senha1234\"}";
        JsonNode p = corpo(chamar(post("/api/profissionais"), rec, prof).andExpect(status().isCreated()));
        long id = p.get("id").asLong();
        assertTrue(p.get("possuiAcesso").asBoolean());
        login("novo.prof@exemplo.com", "Senha1234");
        chamar(post("/api/profissionais"), med, prof).andExpect(status().isForbidden());
        // paciente não recebe contato do profissional
        assertTrue(corpo(chamar(get("/api/profissionais"), pac, null)).get(0).get("telefone").isNull());
        // desativar profissional com consulta futura é bloqueado; sem consultas funciona
        chamar(patch("/api/profissionais/1/status?ativo=false"), rec, null).andExpect(status().isBadRequest());
        chamar(patch("/api/profissionais/" + id + "/status?ativo=false"), rec, null).andExpect(status().isOk());
        mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"novo.prof@exemplo.com\",\"senha\":\"Senha1234\"}")).andExpect(status().isUnauthorized());

        // administrador: altera perfil, mas não o próprio
        JsonNode novo = corpo(chamar(post("/api/usuarios"), adm,
                "{\"nome\":\"Recep Dois\",\"email\":\"recep2@exemplo.com\",\"senha\":\"Senha1234\",\"perfil\":\"RECEPCIONISTA\"}")
                .andExpect(status().isCreated()));
        chamar(patch("/api/usuarios/" + novo.get("id").asLong() + "/perfil"), adm, "{\"perfil\":\"ADMINISTRADOR\"}").andExpect(status().isOk());
        chamar(patch("/api/usuarios/1/ativo?valor=false"), adm, null).andExpect(status().isBadRequest());
        chamar(post("/api/usuarios"), adm, "{\"nome\":\"X\",\"email\":\"x@exemplo.com\",\"senha\":\"fraca\",\"perfil\":\"ADMINISTRADOR\"}")
                .andExpect(status().isBadRequest());
    }

    @Test @Order(11)
    void notificacoesDashboardEConfiguracoes() throws Exception {
        chamar(post("/api/notificacoes/lembretes?dias=0"), rec, null).andExpect(status().isOk());
        chamar(post("/api/notificacoes/lembretes"), pac, null).andExpect(status().isForbidden());
        chamar(post("/api/notificacoes/lidas"), pac, null).andExpect(status().isOk());
        assertEquals(0, corpo(chamar(get("/api/dashboard"), pac, null)).get("notificacoesNaoLidas").asInt());

        JsonNode dRec = corpo(chamar(get("/api/dashboard"), rec, null).andExpect(status().isOk()));
        assertTrue(dRec.get("pacientes").asInt() >= 7);
        assertTrue(dRec.get("horariosLivresHoje").isNumber());
        JsonNode dPac = corpo(chamar(get("/api/dashboard"), pac, null));
        assertTrue(dPac.get("pacientes").isNull());
        assertTrue(corpo(chamar(get("/api/dashboard"), adm, null)).get("usuarios").asInt() >= 5);

        chamar(put("/api/configuracoes"), adm,
                "{\"inicio\":\"18:00\",\"fim\":\"08:00\",\"duracaoMinutos\":30,\"pausaInicio\":\"12:00\",\"pausaFim\":\"13:00\"}")
                .andExpect(status().isBadRequest());
        chamar(put("/api/auth/senha"), pac, "{\"senhaAtual\":\"errada123\",\"novaSenha\":\"Nova12345\"}").andExpect(status().isBadRequest());
        chamar(put("/api/auth/preferencias"), pac, "{\"nome\":\"Ana Beatriz Lima\",\"notificacoesAtivas\":true,\"tema\":\"escuro\"}")
                .andExpect(status().isOk());
    }

    @Test @Order(12)
    void autocadastroELogoutInvalidaSessao() throws Exception {
        String corpoReg = "{\"nome\":\"Novo Paciente\",\"cpf\":\"135.792.468-28\",\"dataNascimento\":\"1999-09-09\","
                + "\"telefone\":\"(11) 96666-2222\",\"email\":\"novo.paciente@exemplo.com\",\"senha\":\"Senha1234\",\"aceiteLgpd\":true}";
        mvc.perform(post("/api/auth/registrar-paciente").contentType(MediaType.APPLICATION_JSON).content(corpoReg))
                .andExpect(status().isCreated());
        mvc.perform(post("/api/auth/registrar-paciente").contentType(MediaType.APPLICATION_JSON).content(corpoReg))
                .andExpect(status().isBadRequest());
        mvc.perform(post("/api/auth/registrar-paciente").contentType(MediaType.APPLICATION_JSON)
                .content(corpoReg.replace("\"aceiteLgpd\":true", "\"aceiteLgpd\":false"))).andExpect(status().isBadRequest());
        String tk = login("novo.paciente@exemplo.com", "Senha1234");
        chamar(post("/api/auth/logout"), tk, null).andExpect(status().isOk());
        chamar(get("/api/auth/me"), tk, null).andExpect(status().isUnauthorized());
        mvc.perform(post("/api/auth/recuperar-senha").contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"qualquer@exemplo.com\"}")).andExpect(status().isOk());
    }
}
