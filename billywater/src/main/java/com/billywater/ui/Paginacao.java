package com.billywater.ui;

/** Paginação (mín. 10 registos; offset/limite na base). */
public class Paginacao {
    public static final int MINIMO = 10;
    private int pagina = 0;
    private int limite = MINIMO;

    public Paginacao() {}
    public Paginacao(int limite) { this.limite = Math.max(MINIMO, limite); }

    public int offset() { return Math.max(0, pagina) * limite; }
    public int limite() { return limite; }
    public void proxima() { pagina++; }
    public void anterior() { if (pagina > 0) pagina--; }
    public int pagina() { return pagina; }
    public void pagina(int p) { pagina = Math.max(0, p); }
}
