package com.billywater.config;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyFactory;
import java.security.PublicKey;
import java.security.Signature;
import java.security.spec.X509EncodedKeySpec;
import java.time.LocalDate;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Licença BILLY WATER / SGF.
 *
 * Formato do ficheiro de licença (texto simples, um campo por linha, {@code chave=valor}):
 * <pre>
 *   produto=BILLY WATER
 *   versao=5
 *   titular=Nome do cliente
 *   nuit=100000000
 *   emissao=2026-08-31
 *   expira=2027-08-31
 *   dispositivos=3
 *   assinatura=&lt;base64 RSA-SHA256 da assinatura&gt;
 * </pre>
 *
 * A assinatura cobre TODOS os campos excepto {@code assinatura}, na ordem canónica.
 * A verificação usa a chave PÚBLICA embebida em {@code /licenca/publica.pem}.
 * Assinar é responsabilidade do SISTEMA SEPARADO {@code gerador-licencas} (que contém a chave PRIVADA
 * e NÃO faz parte do SGF).
 */
public final class Licenca {

    public static final String PRODUTO = "BILLY WATER";
    public static final int VERSAO = 5;

    private static final String CANONICAIS = "produto,versao,titular,nuit,emissao,expira,dispositivos";

    public String produto;
    public String versao;
    public String titular;
    public String nuit;
    public String emissao;
    public String expira;
    public String dispositivos;
    public String assinatura;

    public boolean validaEm() {
        try { return LocalDate.now().isBefore(LocalDate.parse(expira).plusDays(1)); } catch (Exception e) { return false; }
    }

    /** Constrói o payload canónico que é assinado (mesma ordem do gerador). */
    public String payload() {
        Map<String, String> m = new LinkedHashMap<>();
        m.put("produto", produto); m.put("versao", versao); m.put("titular", titular);
        m.put("nuit", nuit); m.put("emissao", emissao); m.put("expira", expira); m.put("dispositivos", dispositivos);
        StringBuilder sb = new StringBuilder();
        for (String k : CANONICAIS.split(",")) {
            String v = m.get(k);
            sb.append(k).append('=').append(v == null ? "" : v).append('\n');
        }
        return sb.toString();
    }

    /** Verifica a assinatura com a chave pública embebida. */
    public boolean assinaturaValida() {
        try (InputStream in = getClass().getResourceAsStream("/licenca/publica.pem")) {
            if (in == null) return false;
            String pem = new String(in.readAllBytes(), StandardCharsets.UTF_8)
                    .replace("-----BEGIN PUBLIC KEY-----", "")
                    .replace("-----END PUBLIC KEY-----", "")
                    .replaceAll("\\s", "");
            byte[] der = Base64.getDecoder().decode(pem);
            PublicKey pub = KeyFactory.getInstance("RSA").generatePublic(new X509EncodedKeySpec(der));
            Signature sig = Signature.getInstance("SHA256withRSA");
            sig.initVerify(pub);
            sig.update(payload().getBytes(StandardCharsets.UTF_8));
            return sig.verify(Base64.getDecoder().decode(assinatura));
        } catch (Exception ex) { return false; }
    }

    /** Validação completa: assinatura correcta + produto + versão + não expirada. */
    public boolean valida() {
        if (assinatura == null || assinatura.isBlank()) return false;
        if (!PRODUTO.equalsIgnoreCase(produto)) return false;
        if (!Integer.toString(VERSAO).equals(versao)) return false;
        if (emissao == null || expira == null) return false;
        try {
            LocalDate.parse(emissao);
            LocalDate entre = LocalDate.parse(expira);
            if (!entre.isAfter(LocalDate.parse(emissao))) return false;
        } catch (Exception ex) { return false; }
        return assinaturaValida() && validaEm();
    }

    public static Licenca carregar(Path ficheiro) throws IOException {
        Map<String, String> m = new LinkedHashMap<>();
        for (String linha : Files.readAllLines(ficheiro, StandardCharsets.UTF_8)) {
            String l = linha.trim();
            if (l.isEmpty() || l.startsWith("#")) continue;
            int i = l.indexOf('=');
            if (i > 0) m.put(l.substring(0, i).trim(), l.substring(i + 1).trim());
        }
        Licenca l = new Licenca();
        l.produto = m.get("produto"); l.versao = m.get("versao"); l.titular = m.get("titular");
        l.nuit = m.get("nuit"); l.emissao = m.get("emissao"); l.expira = m.get("expira");
        l.dispositivos = m.get("dispositivos"); l.assinatura = m.get("assinatura");
        return l;
    }

    public static Path caminhoPadrao() { return Path.of("config", "licenca.bin"); }

    public String resumo() {
        return "Titular: " + titular + "  ·  NUIT: " + nuit + "  ·  Válida até: " + expira + "  ·  Dispositivos: " + dispositivos;
    }
}
