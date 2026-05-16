# Padrão Corporativo de Versionamento (Git Workflow)

Este documento descreve o fluxo de trabalho diário de desenvolvimento, desde a configuração inicial até a entrega de novas funcionalidades via Pull Request.

## 1. O Dia 1: Setup Inicial (Apenas uma vez por máquina)
O computador não reconhece um repositório Git magicamente. Apenas na primeira vez que for trabalhar em uma máquina nova, é necessário baixar todo o histórico e criar a pasta oculta `.git`.
* **Comando:** `git clone https://github.com/willsaeff/events-manager.git`
* **Próximo passo:** Entrar na pasta gerada (`cd events-manager`). A partir deste momento, o comando `clone` nunca mais é utilizado nesta máquina.

## 2. O Ritual Matinal: Sincronizando a Base
Nenhum dia de trabalho começa sem garantir que o código local está idêntico à nuvem, evitando sobrescrever o trabalho de outros desenvolvedores.
1. `git checkout main` (Garante que está no quadro de avisos principal).
2. `git pull origin main` (Baixa todas as novidades aprovadas pelo time).

## 3. Trabalhando em Tickets: A Regra do Isolamento
Nunca se coda diretamente na `main`. Todo novo ticket (tarefa, bug, feature) exige uma área de trabalho isolada.
* **Comando:** `git checkout -b feature/nome-da-tarefa` (Cria a branch a partir da `main` atualizada e já move para ela).

## 4. O Malabarismo de Tarefas: Troca de Contexto
É comum precisar pausar o Ticket A para olhar o Ticket B. O Git exige que a área de trabalho atual esteja limpa antes da troca. Existem duas abordagens:

### Abordagem A: Pausas Rápidas (Git Stash)
Ideal para quando é necessário trocar de branch por alguns minutos e você não quer criar um commit "lixo". O `stash` guarda os arquivos não salvos localmente no disco (não na RAM). Funciona como uma pilha (LIFO - Last In, First Out).
* **Guardar:** `git stash` (ou `git stash save "nome amigável"` para facilitar a identificação).
* **Listar guardados:** `git stash list`
* **Recuperar o último:** `git stash pop`
* **Recuperar um específico:** `git stash pop stash@{0}`

### Abordagem B: Fim do dia ou Pausas Longas (Commit WIP)
Ideal para salvar o progresso real. O commit garante que a alteração faz parte do histórico oficial da branch.
* `git add .`
* `git commit -m "wip: controller de eventos pela metade"`

## 5. Sincronização Contínua: Evitando o Inferno do Merge
Se uma branch leva vários dias para ser concluída, ela fica defasada em relação à `main`. **Cada branch isolada** precisa ser atualizada individualmente para evitar conflitos gigantes na hora do Pull Request.
1. `git checkout main` -> `git pull origin main` (Atualiza a base).
2. `git checkout feature/sua-tarefa` (Volta para o trabalho em andamento).
3. `git merge main` (Injeta as novidades da empresa no seu trabalho).

## 6. O Fim do Expediente: Push de Segurança
Código apenas no computador local corre risco (falhas de hardware, roubo, etc.). Todo final de dia, o trabalho deve ser enviado à nuvem, mesmo que a funcionalidade esteja incompleta.
* **Primeiro envio da branch:** `git push -u origin feature/sua-tarefa`
* **Envios subsequentes:** `git push`
* *Nota:* Fazer o push **não** significa abrir um Pull Request. O código sobe invisível para a `main`, servindo como um backup corporativo.

## 7. A Entrega: O Pull Request
Quando a funcionalidade for concluída, testada e com os commits organizados:
1. Faça o último `git push`.
2. Acesse o GitHub pelo navegador.
3. Clique em "Compare & pull request".
4. Aguarde a revisão (Code Review) do Tech Lead ou da equipe.
5. Após aprovação, o código é mesclado (merged) oficialmente na `main`.

## 8. Apagando local branches inuteis e rodando um .mvnw clean install!
Em começo de expediente voce deve atualizar a main, dar um Pull. Se voce deletou a branch usada no ultimo PR na pagina no GitHub, agora voce deve apagar essas branches localmente:
1. `git pull origin`
2. `git fetch --prune` (mostra quais das suas branches foram de fato deletadas no GitHub, apaga o pointer local pra elas)
3. `git branch -D "path da branch"` (talvez o git reclame dizendo que nao foi dado merge, ai voce precisa usar o -D maiusculo)
4. `git status` (so para garantir que nao tem arquivo solto no projeto)
5. `./mvnw clean install` OU `./mvnw clean install -DskipTests` (obrigatorio usar, a nao ser que as mudancas na main tenham sido minimas e bobas... nao confie, builde de novo!)
6. FIM