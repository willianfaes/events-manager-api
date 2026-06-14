* Code First: Build the core logic. (Entities, Controllers, Mappers, Pagination, Soft Deletes).
	Pagination & Sorting:
	Difficulty: Easy
	Time: 1–2 days. Spring Data JPA literally has a built-in Pageable object. You just pass it into your repository, and Spring writes the complex SQL limit/offset queries for you.

	Soft Deletes:
	Difficulty: Easy
	Time: 1 day. In modern Hibernate (which Spring Boot uses), this is often just adding an @SQLRestriction and @SQLDelete annotation to your entity.

	User Profiles (1-to-1 Relationship):
	Difficulty: Easy to Medium
	Time: 2–3 days. This is just practicing JPA relationships (@OneToOne). The logic is straightforward, but setting up the Mappers for it takes a little focus.

	Advanced Filtering (Search):
	Difficulty: Medium
	Time: 3–5 days. This is the hardest of the bunch. You will have to learn how to write custom @Query strings or use JPA Specifications to handle situations where a user searches by date, but leaves the category blank.

* Global Exception Handling: Make sure your API returns clean, professional errors instead of massive Java stack traces.

* Testing (JUnit/Mockito): Write tests to prove your code works. (remember CI/CD server will run my tests to then send it to Test server? yeah... my tests must be good!)

* Documentation (Swagger): Add Swagger so a frontend developer knows how to use your API.

* Containerization (Docker): Wrap your app and your MySQL database in Docker so it runs anywhere.

* The Cloud (AWS + CI/CD): Deploy that Docker container to AWS and automate the deployment with GitHub Actions.

Obs: 
	To deploy your API, you only need to learn three specific AWS services:
	RDS (Relational Database Service): To host your MySQL database in the cloud.
	EC2 (Elastic Compute Cloud): To run your Spring Boot .jar file.
	S3 (Simple Storage Service): Optional, but needed if you want users to upload profile pictures.

* Learn linux basic terminal commands. Maybe something else? 99% of backend servers run on linux! When production server crash, a tech lead expects me to SSH into the Linux server and look at the logs to find the error. If I look like a deer in headlights when the terminal opens... it's a red flag.

* Java code style convention: What tools devs use to check and enforce the Code Convention they are following? Remember: Make sure the final code fully respects the Oracle Java Convention (tho there are other, stick to this one). A professional portfolio follow professional standards!
	* ANSWER:
	* Na IDE: Instalamos o plugin SonarLint e usamos o formatador nativo do IntelliJ (já configurado para o padrão Google ou Oracle).
	* No CI/CD (GitHub Actions): Usamos plugins como Spotless ou Checkstyle no Maven/Gradle. Se o código não seguir a convenção, a esteira quebra e o Pull Request não é aprovado.

* Do devs still relly on manual git commands on terminal at the job? What is the current enterprise behaviour regarding DOCKER and GIT terminal code? Rely on manual inputs, or AI? FIND OUT!
	* ANSWER:
	* Git: Para comandos rápidos e diários (commit, push, pull, checkout), o terminal é o rei pela velocidade. Porém, para resolver conflitos complexos de merge ou analisar o histórico de alterações (diffs), 90% dos devs seniores usam a interface gráfica do próprio IntelliJ ou ferramentas como GitKraken.
	* Docker: Usamos comandos de terminal básicos (docker ps, docker logs), mas gerenciamos os containers visualmente pelo Docker Desktop.
	* O papel da IA nisso: Ninguém usa IA para rodar comando de Git. Nós usamos a IA para escrever o arquivo Dockerfile, o docker-compose.yml ou scripts complexos de pipeline no GitHub Actions.

* 🌥️ How to Include AWS Without the Exam
If you want to show employers you care about the cloud, spend just one weekend deployment-testing. Do not take an exam. Instead, open a free AWS account, take your Dockerized Spring Boot API, and manually deploy it to AWS Elastic Beanstalk or an AWS EC2 instance.
Adding the line "Successfully deployed a containerized Spring Boot API onto AWS" to your resume will grab a Turin tech recruiter's attention much faster than a theoretical certificate—and it will take you a fraction of the time to achieve.

* 🔨 The "Show, Don't Tell" Strategy for Turin Engineers
Since your goal is to talk directly to engineers and show them you are not just a "regular student" who only knows theory, your resume and GitHub need to do the talking for you. Italian university CS programs are notoriously heavy on theoretical mathematics and very light on actual coding. !!!!!----** You can exploit this gap.----!!!!!
When an engineer opens your GitHub in August, here is what will make them say "Wow, this person is ahead of local graduates":
A Spotless Git History: Since you are comfortable with Git Flow, ensure your portfolio repository doesn't have a single giant commit called "initial commit." Show a clean history of feature branches, descriptive commit messages (e.g., feat: implement user authentication endpoint), and pull requests. Engineers look at commit history to see how you think.
The "Turin-Specific" Project Idea: Instead of building a generic Todo app, build a REST API that solves a relatable problem. For example: A backend for a "Torino Bike/Scooter Sharing aggregator" or a "University Exam Scheduler."
An Impressive README: Write your project documentation in flawless English. Include a diagram of your database architecture, a list of your API endpoints, and a quick "How to Run" section using Docker Compose.
	Colocar uma seção chamada ## 🏗️ Architecture dentro do seu README.md atual. Conforme sua API for ganhando corpo, se aquela seção ficar muito grande, aí sim você extrai ela para um ARCHITECTURE.md próprio.

* Practicing JDBC (Java Database Connectivity) or building simple CLI (Command Line Interface) applications:
Write a pure Java program that connects to your Dockerized PostgreSQL database.
Practice writing raw SQL queries (INSERT, SELECT, UPDATE, DELETE) directly inside your Java code.
Handle database connection errors properly using Java's try-catch blocks.
This will give you a flawless transition into July, because Spring Boot handles all of this database connection work automatically behind the scenes. Understanding how it works manually first will make you a much stronger developer.

### 📋 TODO: Fluxo Prático para Criação de Feature com Testes (JUnit 5 + Mockito)

**Objetivo:** Criar a feature garantindo a cobertura dos cenários principais e de erro, usando a estrutura BDD para mapear o comportamento antes da implementação real.

- [ ] **1. Criar a Casca do Código Base (Service)**
  - [ ] Criar o método apenas com a assinatura (recebendo o DTO e retornando nulo ou vazio).
  - [ ] Não implementar nenhuma regra de negócio ainda.

- [ ] **2. Criar a Classe de Teste (`ServiceTest`)**
  - [ ] Anotar a classe com `@ExtendWith(MockitoExtension.class)`.
  - [ ] Instanciar a classe principal com `@InjectMocks`.
  - [ ] Mocar as dependências (Repositories, etc.) com `@Mock`.

- [ ] **3. Escrever o Roteiro de Testes (Apenas Assinaturas)**
  - [ ] Criar os métodos com `@Test` e `@DisplayName("...")` descrevendo exatamente o que deve acontecer. Ex: `deveSalvarComSucesso()`, `deveLancarExcecaoQuandoDadoInvalido()`.
  - [ ] Dentro de cada método, montar apenas os comentários da estrutura BDD:
    - `// Given (Dado o cenário X)`
    - `// When (Quando a ação Y ocorrer)`
    - `// Then (Então o resultado Z deve ser garantido)`

- [ ] **4. Implementar a Regra de Negócio**
  - [ ] Voltar na classe Service e codar a lógica real (validações, chamadas ao banco).
  - [ ] Usar o Debugger se necessário.

- [ ] **5. Preencher os Testes e Validar (O Verificador Final)**
  - [ ] Voltar na classe de teste e preencher os blocos Given/When/Then com o código real do Mockito (ex: `when(repository.save()).thenReturn(...)`).
  - [ ] Rodar os testes. Se der verde, a feature está pronta para PR.










OBS: Quando chegar a hora de usar agentes para fazer code review nos seus projetos, de guidelines para a IA. Esse é um exemplo de guideline de Code Review que o Hyperskill forneceu:
----- EXEMPLO
# Code Review Guidelines

This document outlines guidelines for reviewing code changes, focusing on aspects that might be missed by automated tools.

## Security Vulnerabilities

When reviewing code, look for these potential security issues:

- **Injection vulnerabilities**: SQL, Command, LDAP, XPath, or other injection flaws
- **Authentication issues**: Weak authentication mechanisms, hardcoded credentials
- **Authorization problems**: Missing or incorrect permission checks
- **Sensitive data exposure**: Unencrypted sensitive data, improper handling of secrets
- **Insecure cryptographic implementations**: Weak algorithms, improper key management
- **CSRF/XSS vulnerabilities**: Missing CSRF tokens, unescaped user input
- **Insecure deserialization**: Deserializing untrusted data without proper validation
- **Dependency vulnerabilities**: Outdated libraries with known security issues
- **Insecure file operations**: Path traversal vulnerabilities, unsafe file handling
- **Race conditions**: Time-of-check to time-of-use (TOCTOU) bugs

## Hard-to-Notice Bugs

Pay special attention to these subtle issues:

- **Off-by-one errors**: Boundary conditions in loops and array accesses
- **Null pointer dereferences**: Missing null checks before accessing objects
- **Resource leaks**: Unclosed files, connections, or other resources
- **Concurrency issues**: Race conditions, deadlocks, improper synchronization
- **Exception handling**: Swallowed exceptions, overly broad catch blocks
- **State management**: Incorrect state transitions, missing state validation
- **Edge cases**: Handling of empty collections, extreme values, or special inputs
- **Floating-point precision issues**: Equality comparisons with floating-point values
- **Internationalization bugs**: Locale-dependent operations, character encoding issues
- **Logical errors**: Incorrect boolean expressions, misplaced parentheses

## Unintended Code

Look for code that was likely not intended to be committed:

- **Debug print statements**: Console.log, System.out.println, print, etc.
- **Commented-out code**: Large blocks of commented code without explanation
- **TODO/FIXME comments**: Especially those indicating incomplete work
- **Test or mock data**: Hardcoded test values in production code
- **Temporary workarounds**: Code marked as temporary or with "hack" comments
- **Gibberish or placeholder text**: Random characters, "asdf", "test123", etc.
- **Development configuration**: Local paths, development API keys
- **Disabled functionality**: Commented-out method calls or conditionals
- **Debugging flags**: Enabled debug modes or verbose logging
- **Incomplete refactoring**: Partially renamed variables or methods

## Code Style Issues

Review for these code style problems:

- **Inconsistent naming conventions**: Mixed camelCase/snake_case, inconsistent prefixes
- **Poor code organization**: Overly long methods or classes, poor separation of concerns
- **Duplicated code**: Copy-pasted logic that should be refactored
- **Magic numbers/strings**: Unexplained literals that should be constants
- **Misleading comments**: Comments that don't match the actual code behavior
- **Inconsistent formatting**: Mixed indentation, line length violations
- **Poor variable names**: Cryptic or overly abbreviated identifiers
- **Excessive nesting**: Deeply nested conditionals or loops
- **Unused imports/variables**: Dead code that should be removed
- **Overly complex expressions**: Code that's difficult to understand at a glance

## Additional Concerns

Other issues to watch for:

- **Performance problems**: Inefficient algorithms, unnecessary computations
- **Maintainability issues**: Code that's difficult to modify or extend
- **Accessibility concerns**: UI changes that might affect accessibility
- **Backwards compatibility**: Breaking changes to public APIs
- **Error handling**: Missing or inappropriate error handling
- **Documentation**: Missing or outdated documentation
- **Test coverage**: Insufficient test coverage for new or modified code
- **Dependency management**: Unnecessary or conflicting dependencies
- **Configuration issues**: Hardcoded configuration that should be externalized
- **Compliance concerns**: Code that might violate legal or regulatory requirements
