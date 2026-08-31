package com.billywater.views;

import com.billywater.config.Config;
import com.billywater.config.ConfigBanco;
import com.billywater.dao.ParametroDAO;
import com.billywater.domain.Parametro;
import com.billywater.ui.FormDialog;
import com.billywater.ui.Ui;
import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;

/** Configurações (SGV): parâmetros, dados da empresa, licença, BD, utilizadores. */
public class ConfiguracoesView extends BaseView {

    private final Label estado = new Label("");

    public ConfiguracoesView() {
        super("Configurações", "Configurações operacionais e da conta (SGV)");
        GridPane grid = new GridPane();
        grid.setHgap(14); grid.setVgap(16); grid.setPadding(new Insets(8));

        VBox par = cartao("Parâmetros de facturação");
        Button bPar = Ui.primario("Editar parâmetros"); bPar.setMaxWidth(Double.MAX_VALUE);
        par.getChildren().add(bPar);
        bPar.setOnAction(e -> parametros());

        VBox emp = cartao("Dados da empresa");
        Button bEmp = Ui.primario("Editar dados"); bEmp.setMaxWidth(Double.MAX_VALUE);
        emp.getChildren().add(bEmp);
        bEmp.setOnAction(e -> empresa());

        VBox lic = cartao("Licença");
        Label licTxt = new Label("Verificação RSA-SHA256 (SGF só lê chave pública).");
        licTxt.setWrapText(true);
        Button bVer = Ui.fantasma("Ver estado da licença"); bVer.setMaxWidth(Double.MAX_VALUE);
        Button bAct = Ui.primario("Activar licença (.lic)"); bAct.setMaxWidth(Double.MAX_VALUE);
        lic.getChildren().addAll(licTxt, bVer, bAct);
        bVer.setOnAction(e -> licenca());
        bAct.setOnAction(e -> com.billywater.servico.ServicoLicenca.activar(getScene().getWindow()));

        VBox bd = cartao("Base de dados");
        Button bBd = Ui.primario("Testar ligação"); bBd.setMaxWidth(Double.MAX_VALUE);
        bd.getChildren().add(bBd);
        bBd.setOnAction(e -> testarBd());

        VBox usr = cartao("Utilizadores");
        Button bUsr = Ui.primario("Novo utilizador"); bUsr.setMaxWidth(Double.MAX_VALUE);
        usr.getChildren().add(bUsr);
        bUsr.setOnAction(e -> novoUtilizador());

        grid.add(par, 0, 0); grid.add(emp, 1, 0); grid.add(lic, 2, 0);
        grid.add(bd, 0, 1); grid.add(usr, 1, 1);
        estado.getStyleClass().add("totais"); estado.setWrapText(true); estado.setMaxWidth(900);
        body().getChildren().addAll(grid, estado);
    }

    private VBox cartao(String titulo) {
        VBox v = new VBox(10);
        v.getStyleClass().add("card-pane");
        v.setPadding(new Insets(16)); v.setPrefWidth(290);
        Label t = new Label(titulo); t.getStyleClass().add("view-subtitle");
        v.getChildren().add(t);
        return v;
    }

    private void parametros() {
        try {
            ParametroDAO dao = new ParametroDAO();
            Parametro p = dao.unico();
            if (p == null) { stateWarn("Sem ficha de parâmetros — corrija a migração."); return; }
            FormDialog f = new FormDialog();
            f.add("IVA geral (%)", Ui.campo("IVA geral (%)"));
            f.add("IVA água (%)", Ui.campo("IVA água (%)"));
            f.add("Taxa saneamento (%)", Ui.campo("Taxa saneamento (%)"));
            f.add("Dia limite pagamento", Ui.campo("Dia limite pagamento"));
            f.add("Série FT", Ui.campo("Série FT")); f.add("Série VD", Ui.campo("Série VD"));
            f.setValor("IVA geral (%)", p.ivaGeral);
            f.setValor("IVA água (%)", p.percentagemIvaAgua);
            f.setValor("Taxa saneamento (%)", p.taxaSaneamento);
            if (!f.mostrar("Parâmetros de facturação")) return;
            p.ivaGeral = f.valor("IVA geral (%)");
            p.percentagemIvaAgua = f.valor("IVA água (%)");
            p.taxaSaneamento = f.valor("Taxa saneamento (%)");
            String dia = f.texto("Dia limite pagamento");
            if (dia != null && !dia.trim().isEmpty()) { try { p.diaLimitePagamento = Integer.parseInt(dia.trim()); } catch (NumberFormatException ignored) {} }
            if (!f.texto("Série FT").trim().isEmpty()) p.serieFt = f.texto("Série FT").trim();
            if (!f.texto("Série VD").trim().isEmpty()) p.serieVd = f.texto("Série VD").trim();
            dao.salvar(p);
            estado.setText("Parâmetros actualizados.");
        } catch (Exception ex) { Ui.erro("Erro", ex.getMessage()); }
    }

    private void empresa() {
        try {
            ParametroDAO dao = new ParametroDAO();
            Parametro p = dao.unico();
            if (p == null) { stateWarn("Sem dados da empresa — corrija a migração."); return; }
            FormDialog f = new FormDialog();
            f.add("Empresa", Ui.campo("Empresa")); f.add("NUIT", Ui.campo("NUIT"));
            f.add("Alvará", Ui.campo("Alvará")); f.add("Endereço", Ui.campo("Endereço"));
            f.add("Cidade", Ui.campo("Cidade")); f.add("Telefone", Ui.campo("Telefone")); f.add("E-mail", Ui.campo("E-mail"));
            f.setTexto("Empresa", p.nomeEmpresa); f.setTexto("NUIT", p.nuit); f.setTexto("Alvará", p.alvara);
            f.setTexto("Endereço", p.endereco); f.setTexto("Cidade", p.cidade); f.setTexto("Telefone", p.telefone); f.setTexto("E-mail", p.email);
            if (!f.mostrar("Dados da empresa")) return;
            p.nomeEmpresa = f.texto("Empresa"); p.nuit = f.texto("NUIT"); p.alvara = f.texto("Alvará");
            p.endereco = f.texto("Endereço"); p.cidade = f.texto("Cidade"); p.telefone = f.texto("Telefone"); p.email = f.texto("E-mail");
            dao.salvar(p);
            estado.setText("Dados da empresa actualizados.");
        } catch (Exception ex) { Ui.erro("Erro", ex.getMessage()); }
    }

    private void stateWarn(String msg) { estado.setText(msg); }

    private void licenca() {
        try {
            boolean valida = com.billywater.servico.ServicoLicenca.autorizada();
            estado.setText(valida ? "Licença válida." : "Licença não activada — o modo demonstração está activo (facturação por produção limitada).");
        } catch (Exception ex) { estado.setText("Erro de licença: " + ex.getMessage()); }
    }

    private void testarBd() {
        try {
            String perfil = Config.isRealProfile() ? "PostgreSQL (REAL)" : "H2 test (TESTE)";
            java.sql.Connection c = ConfigBanco.get();
            boolean ok = c != null && !c.isClosed();
            estado.setText((ok ? "Ligação OK ao perfil " : "Sem ligação ao perfil ") + perfil);
            if (ok) c.close();
        } catch (Exception ex) { estado.setText("Erro BD: " + ex.getMessage()); }
    }

    private void novoUtilizador() {
        FormDialog f = new FormDialog();
        f.add("Login", Ui.campo("Login"));
        f.add("Nome", Ui.campo("Nome completo"));
        javafx.scene.control.PasswordField pass = new javafx.scene.control.PasswordField(); f.add("Palavra-passe", pass);
        f.add("Perfil", Ui.campo("TECNICO"));
        if (!f.mostrar("Novo utilizador")) return;
        try {
            com.billywater.domain.Utilizador u = new com.billywater.domain.Utilizador();
            u.username = f.texto("Login");
            u.nomeCompleto = f.texto("Nome");
            u.senhaHash = com.billywater.servico.ServicoSeguranca.hash(f.texto("Palavra-passe"));
            u.perfil = f.texto("Perfil"); u.ativo = true; u.forcarMudancaSenha = true;
            u.tentativasFalhadas = 0; u.bloqueado = false;
            new com.billywater.dao.UtilizadorDAO().salvar(u);
            estado.setText("Utilizador criado.");
        } catch (Exception ex) { Ui.erro("Erro", ex.getMessage()); }
    }
}
