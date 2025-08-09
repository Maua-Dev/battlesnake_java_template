# 🐍 BattleSnake Lambda Java

Este projeto é um exemplo simples de integração do [BattleSnake](https://play.battlesnake.com/) usando **AWS Lambda** com **Java** e **API Gateway**.  
Ele foi escrito para rodar na AWS sem necessidade de servidor próprio, usando **Maven** para gerenciamento de dependências.

---

## 📦 Pré-requisitos

- **Java JDK** (versão 11 ou superior)  
  [Download do JDK](https://www.oracle.com/java/technologies/javase-downloads.html) ou use o OpenJDK.
- **Maven CLI**  
  [Download Maven](https://maven.apache.org/download.cgi)  
  Instalação:
  ```bash
  # Windows (chocolatey)
  choco install maven
  
  # Mac (homebrew)
  brew install maven
  
  # Linux (apt)
  sudo apt install maven
  ```

- Conta na **AWS** e **AWS CLI** configurada (`aws configure`)
- Conhecimento básico sobre **API Gateway** e **Lambda**

---

## 📂 Estrutura do Projeto

O projeto segue a estrutura **Maven Standard Directory Layout**:

```
.
├── pom.xml                  # Arquivo de dependências Maven
├── src
│   ├── main
│   │   ├── java
│   │   │   └── com
│   │   │       └── mauadev
│   │   │           └── code
│   │   │               └── Handler.java   # Código principal
│   │   └── resources
│   │       └── (se necessário, arquivos de configuração)
│   └── test
│       └── java (testes unitários)
```

---

## ⚙️ Dependências (`pom.xml`)

Um `pom.xml` mínimo para rodar este projeto seria:

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd"
         xmlns="http://maven.apache.org/POM/4.0.0" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance">
    <modelVersion>4.0.0</modelVersion>

    <groupId>com.mauadev</groupId>
    <artifactId>battlesnake-lambda</artifactId>
    <version>1.0</version>

    <properties>
        <project.build.sourceEncoding>UTF-8</project.build.sourceEncoding>
        <maven.compiler.release>17</maven.compiler.release>
    </properties>

    <dependencies>
        <dependency>
            <groupId>com.amazonaws</groupId>
            <artifactId>aws-lambda-java-core</artifactId>
            <version>1.2.3</version>
        </dependency>
        <dependency>
            <groupId>com.amazonaws</groupId>
            <artifactId>aws-lambda-java-events</artifactId>
            <version>3.13.0</version>
        </dependency>
        <dependency>
            <groupId>com.google.code.gson</groupId>
            <artifactId>gson</artifactId>
            <version>2.10.1</version>
        </dependency>
    </dependencies>

    <build>
        <plugins>
            <plugin>
                <groupId>org.apache.maven.plugins</groupId>
                <artifactId>maven-compiler-plugin</artifactId>
                <version>3.13.0</version>
                <configuration>
                    <release>${maven.compiler.release}</release>
                </configuration>
            </plugin>

            <plugin>
                <groupId>org.apache.maven.plugins</groupId>
                <artifactId>maven-shade-plugin</artifactId>
                <version>3.6.0</version>
                <executions>
                    <execution>
                        <phase>package</phase>
                        <goals>
                            <goal>shade</goal>
                        </goals>
                        <configuration>
                            <createDependencyReducedPom>false</createDependencyReducedPom>
                        </configuration>
                    </execution>
                </executions>
            </plugin>
        </plugins>
    </build>
</project>
```

---

## 🚀 Como compilar e empacotar

```bash
# Baixar dependências e compilar
mvn clean install

# Criar um JAR empacotado para AWS Lambda
mvn package
```

O JAR final ficará em:
```
target/battlesnake-lambda-1.0-SNAPSHOT-shaded.jar
```

---

## ☁️ Deploy na AWS Lambda

O processo de deploy deste projeto já está automatizado via **Terraform**.  
Não é necessário modificar nada no código ou no Terraform para implantar na AWS.

Assim que o Terraform concluir o deploy, será criada automaticamente uma URL pública do **API Gateway**.

---

## 🎯 Configurando no site do BattleSnake

1. Acesse sua conta no [BattleSnake](https://play.battlesnake.com/)
2. Vá até **My Battlesnakes** → **New Battlesnake**
3. Escolha um nome e uma cor para sua cobra
4. No campo **URL**, insira a URL gerada pelo Terraform (exemplo: `https://<ID>.execute-api.<REGIÃO>.amazonaws.com/`)

![alt text](images/Screenshot%202025-08-09%20at%2013.22.04.png)
![alt text](images/image.png)
![alt text](images/image-1.png)
![alt text](images/image-2.png)

5. Salve as configurações
6. Agora você pode testar a cobra nos jogos e desafios!


---

## 📜 O que o código faz

O `Handler.java` implementa a interface do **AWS Lambda RequestHandler**, recebendo eventos do API Gateway e respondendo com JSON.

### Principais partes:
- **`handleRequest()`** → ponto de entrada da Lambda
  - Lê o **path** da requisição
  - Direciona para o método correto (`handleInfo`, `handleStart`, `handleMove`, `handleEnd`)
  - Retorna resposta JSON ou erro 404
- **`handleInfo()`** → informações da cobra (cor, cabeça, cauda, autor, versão da API)
- **`handleStart()`** → chamado no início do jogo (log inicial)
- **`handleMove()`** → lógica da jogada (neste exemplo, sempre "up")
- **`handleEnd()`** → chamado no final do jogo (log final)
- Usa **Gson** para converter objetos Java para JSON
- Usa **APIGatewayProxyRequestEvent** e **APIGatewayProxyResponseEvent** para receber/enviar dados

---

## 🧪 Testando localmente

Você pode simular requisições HTTP com `curl`:

```bash
curl -X GET https://<SEU_API_GATEWAY_URL>/
curl -X POST https://<SEU_API_GATEWAY_URL>/start -d '{}'
curl -X POST https://<SEU_API_GATEWAY_URL>/move -d '{}'
curl -X POST https://<SEU_API_GATEWAY_URL>/end -d '{}'
```

---

## 📌 Observações

- O método `handleMove()` deve conter a inteligência do jogo.  
- No deploy da AWS Lambda, o **Handler** deve ser exatamente:
  ```
  com.mauadev.code.Handler::handleRequest
  ```
- O uso do **maven-shade-plugin** é obrigatório para empacotar todas as dependências no JAR final.
