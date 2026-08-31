package com.billywater.ui;

import com.billywater.dao.PermissaoDAO;
import com.billywater.domain.Utilizador;

/** Sessão do operador (utilizador actual, perfil, permissões). */
public final class Sessao {

    private static Utilizador atual;
    private static boolean modoTreino = false;

    private Sessao() {}

    public static void entrar(Utilizador u, String perfil) {
        atual = u;
        Permissoes.recarregar();
    }

    public static void sair() { atual = null; }

    public static Utilizador atual() { return atual; }

    public static String nomePerfil() { return atual == null ? "-" : (atual.perfil == null ? "-" : atual.perfil); }

    public static boolean ehAdmin() { return "ADMIN".equalsIgnoreCase(nomePerfil()); }

    public static boolean pode(String permissao) { return Permissoes.pode(permissao); }

    public static void setModoTreino(boolean t) { modoTreino = t; }
    public static boolean estaEmTreino() { return modoTreino; }
}
