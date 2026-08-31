package com.billywater.domain;

/** Registo de permissão (RBAC) por perfil/página/acção. */
public class Permissao {
    public Long id;
    public String perfil;
    public String pagina;
    public String acao;
    public Boolean permitir = true;

    public String getPerfil() { return perfil; }
    public String getPagina() { return pagina; }
    public String getAcao() { return acao; }
    public Boolean getPermitir() { return permitir; }
}
