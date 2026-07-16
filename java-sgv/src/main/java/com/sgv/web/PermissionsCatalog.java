package com.sgv.web;

import java.util.Arrays;
import java.util.List;

public class PermissionsCatalog {
    public static List<String> ALL = Arrays.asList(
            // Vendas & PDV
            "VENDAS:ACCESS","VENDAS:CREATE","VENDAS:EDIT","VENDAS:CANCEL","VENDAS:DISCOUNT","VENDAS:CLOSE","VENDAS:PRICE","VENDAS:QUOTES","VENDAS:ORDERS","VENDAS:FASTSALE",
            // Produtos & Estoque
            "PRODUTOS:ACCESS","PRODUTOS:CREATE","PRODUTOS:EDIT","PRODUTOS:DELETE","ESTOQUE:ADJUST","ESTOQUE:TRANSFER",
            // Financeiro
            "DESPESAS:ACCESS","DESPESAS:CREATE","DESPESAS:EDIT","DESPESAS:DELETE","CONTAS:RECONCILE",
            // Clientes
            "CLIENTES:ACCESS","CLIENTES:CREATE","CLIENTES:EDIT","CLIENTES:DELETE",
            // Configurações
            "SETTINGS:ACCESS","USERS:MANAGE","ROLES:MANAGE","BACKUP:RUN"
    );
}
