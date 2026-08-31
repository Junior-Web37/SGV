package com.billywater.servico;

import com.billywater.dao.AuditoriaDAO;
import com.billywater.dao.UtilizadorDAO;
import com.billywater.domain.Auditoria;
import com.billywater.domain.Utilizador;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Base64;

/** Autenticação, hash de senha, bloqueio por falhas, auditoria. */
public class ServicoSeguranca {

    private final UtilizadorDAO utilizadorDAO = new UtilizadorDAO();
    private final AuditoriaDAO auditoriaDAO = new AuditoriaDAO();

    /** Hash de senha (SHA-256 + salt) — em produção usar BCrypt/argon2. */
    public static String hash(String senha) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] salt = "billywater".getBytes(StandardCharsets.UTF_8);
            md.update(salt);
            return Base64.getEncoder().encodeToString(md.digest(senha.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception e) { throw new IllegalStateException(e.getMessage(), e); }
    }

    public Utilizador autenticar(String username, String senha) {
        try {
            Utilizador u = utilizadorDAO.porUsername(username);
            if (u == null) { log(username, "LOGIN", "FALHOU", "LOGIN", "Credenciais inválidas"); return null; }
            if (Boolean.TRUE.equals(u.bloqueado)) { log(username, "SEGURANCA", "BLOQUEADO", "LOGIN", "Conta bloqueada"); return null; }
            if (!Boolean.TRUE.equals(u.ativo)) return null;
            if (u.senhaHash == null || !u.senhaHash.equals(hash(senha))) {
                if (u.tentativasFalhadas == null) u.tentativasFalhadas = 0;
                u.tentativasFalhadas = u.tentativasFalhadas + 1;
                boolean bloquear = u.tentativasFalhadas >= 5;
                u.bloqueado = bloquear;
                try { utilizadorDAO.registrarFalha(u.id); } catch (Exception ignored) {}
                log(username, "SEGURANCA", bloquear ? "BLOQUEADO" : "FALHOU", "LOGIN", "Senha incorrecta (" + u.tentativasFalhadas + ")");
                return null;
            }
            u.tentativasFalhadas = 0;
            try { utilizadorDAO.salvar(u); } catch (Exception ignored) {}
            log(username, "LOGIN", "SUCESSO", "LOGIN", "Autenticado");
            return u;
        } catch (Exception e) { return null; }
    }

    public void log(String utilizador, String categoria, String acao, String entidade, String detalhe) {
        Auditoria a = new Auditoria();
        a.utilizador = utilizador; a.categoria = categoria; a.acao = acao; a.entidade = entidade; a.detalhe = detalhe;
        try { auditoriaDAO.inserir(a); } catch (Exception ignored) {}
    }
}
