# Padrão Corporativo: Docker para Desenvolvedores

No dia a dia, o desenvolvedor não cria infraestrutura do zero. O arquivo `docker-compose.yml` já vem configurado pelo time de DevOps ou Tech Lead. O seu papel é apenas "ligar e desligar" o motor para testar a sua API.

## Os 4 Comandos do Dia a Dia

### 1. O Botão de Ligar (Start/Build Automático)
Lê o `.yml`, baixa a imagem da internet (se não existir), cria o contêiner e deixa rodando silenciosamente no fundo.
* **Comando:** `docker compose up -d`
* *Nota:* Use isso todo dia de manhã. Se o contêiner já existir, ele liga em 1 segundo. Se não existir, ele constrói do zero.

### 2. O Botão de Reset (A "Bomba Atômica")
Se você bagunçou o banco de dados fazendo testes locais ou o Flyway quebrou o esquema, você usa este comando para destruir o contêiner e **formatar o volume** (o HD virtual). O próximo `up -d` trará um banco 100% zerado e limpo.
* **Comando:** `docker compose down -v`

### 3. A Investigação (Ver os Logs)
O Spring Boot deu erro dizendo que não conecta no banco? Use este comando para ver o terminal do banco de dados em tempo real e descobrir o porquê.
* **Comando:** `docker compose logs -f`
* *(A flag `-f` deixa a tela "presa" atualizando as mensagens ao vivo. Use `Ctrl+C` para sair).*

### 4. Pausa Temporária (Economizar RAM)
Diferente do `down -v` (que destrói tudo), o comando `stop` apenas "congela" o contêiner para liberar memória do seu PC sem apagar seus dados de teste.
* **Comando:** `docker compose stop`