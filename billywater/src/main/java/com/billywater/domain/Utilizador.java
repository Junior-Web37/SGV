package com.billywater.domain;

import java.time.LocalDateTime;

/** Utilizador/operador do sistema. */
public class Utilizador {
    public Long id;
    public String nomeCompleto;
    public String username;
    public String senhaHash;
    public String email;
    public String perfil;        // ADMIN, GESTOR, CAIXA, LEITOR, LOJA
    public Boolean ativo = true;
    public Boolean forcarMudancaSenha = false;
    public Integer tentativasFalhadas = 0;
    public Boolean bloqueado = false;
    public LocalDateTime criadoEm;

    public String getUsername() { return username; }
    public String getNomeCompleto() { return nomeCompleto; }
    public String getPerfil() { return perfil; }
    public Boolean getAtivo() { return ativo; }
    public Boolean getBloqueado() { return bloqueado; }
}
