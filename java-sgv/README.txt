========================================================
SGV Desktop - Sistema de Gestão de Vendas
========================================================

COMO INSTALAR E ABRIR O SISTEMA:

1. REQUISITOS:
- Certifique-se de que tem o Java 21 instalado na sua máquina.
- O XAMPP deve estar instalado e o serviço MySQL/MariaDB deve estar "Start" e a correr.
- A base de dados do sistema (`sgv_db`) deve estar importada no phpMyAdmin. (Opcional se o Hibernate/JPA criar automaticamente, mas é sempre bom ter os dados iniciais).

2. COMO ABRIR:
- Basta fazer duplo clique no ficheiro "SGV-Desktop.bat".
- Esse script vai localizar o Java e abrir a interface do SGV.
- Não feche a janela preta (consola) que se abre, pois ela mantém o sistema a correr e mostra erros úteis se a base de dados falhar.

3. CREDENCIAIS PADRÃO (se inseridas no banco de dados base):
- Utilizador: admin
- Senha: password

Problemas comuns:
- "Access denied for user 'root'@'localhost'": O seu XAMPP tem password no root. O SGV está configurado para "root" sem password na porta 3306.
- A janela abre e fecha rápido: Abra o "SGV-Desktop.bat" pelo terminal (cmd) para ver a mensagem de erro que é apresentada.
