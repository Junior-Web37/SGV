package com.billywater.ui;

import com.billywater.dao.PermissaoDAO;

import java.util.HashMap;
import java.util.Map;

/** Permissões por perfil — matriz padrão + overrides da base (RBAC). */
public final class Permissoes {

    private static final Map<String, Map<String, Boolean>> overrides = new HashMap<>();

    /** Matriz padrão: perfil → PAGINA:ACAO → permissão. */
    private static final Map<String, Map<String, Boolean>> PADRAO = new HashMap<>();

    static {
        // ADMIN tem acesso a tudo
        Map<String, Boolean> admin = new HashMap<>();
        admin.put("*", true);
        PADRAO.put("ADMIN", admin);
        // GESTOR
        Map<String, Boolean> gestor = new HashMap<>();
        gestor.put("FACTURACAO:CREATE", true); gestor.put("FACTURACAO:READ", true);
        gestor.put("CAIXA:CREATE", true); gestor.put("LOJA:CREATE", true);
        gestor.put("CLIENTE:CREATE", true); gestor.put("RELATORIOS:READ", true);
        gestor.put("STOCK:CREATE", true); gestor.put("CONFIG:READ", true);
        PADRAO.put("GESTOR", gestor);
        // CAIXA
        Map<String, Boolean> caixa = new HashMap<>();
        caixa.put("CAIXA:CREATE", true); caixa.put("CAIXA:READ", true);
        caixa.put("FACTURACAO:READ", true); caixa.put("RECIBO:READ", true);
        PADRAO.put("CAIXA", caixa);
        // LEITOR
        Map<String, Boolean> leitor = new HashMap<>();
        leitor.put("LEITURA:CREATE", true); leitor.put("LEITURA:READ", true);
        PADRAO.put("LEITOR", leitor);
        // LOJA
        Map<String, Boolean> loja = new HashMap<>();
        loja.put("LOJA:CREATE", true); loja.put("LOJA:READ", true);
        loja.put("STOCK:READ", true); loja.put("COMPRA:READ", true);
        PADRAO.put("LOJA", loja);
    }

    private Permissoes() {}

    public static void recarregar() {
        overrides.clear();
        try {
            Map<String, Map<String, Boolean>> mapa = new PermissaoDAO().mapaPorPerfil();
            if (mapa != null) overrides.putAll(mapa);
        } catch (Exception ignored) {}
    }

    public static void limpar() { overrides.clear(); }

    public static boolean pode(String permissao) {
        String perfil = Sessao.nomePerfil();
        // overrides da base têm prioridade
        Map<String, Boolean> mapaPerfil = overrides.get(perfil);
        if (mapaPerfil != null) {
            if (mapaPerfil.containsKey("*") && mapaPerfil.get("*")) return true;
            if (mapaPerfil.containsKey(permissao)) return Boolean.TRUE.equals(mapaPerfil.get(permissao));
        }
        // matriz padrão
        Map<String, Boolean> padrao = PADRAO.get(perfil);
        if (padrao == null) return false;
        if (padrao.containsKey("*") && padrao.get("*")) return true;
        return Boolean.TRUE.equals(padrao.get(permissao));
    }
}
