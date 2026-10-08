FROM eclipse-temurin:21-jdk-alpine AS builder
WORKDIR /app
# Copier les fichiers Maven
COPY pom.xml mvnw ./
COPY .mvn .mvn
RUN chmod +x mvnw
RUN ./mvnw dependency:go-offline
# Télécharger les dépendances hors ligne (optimisation)
RUN ./mvnw dependency:go-offline
# Copier le code source
COPY src src
# Compiler l'application (en ignorant les tests pour la phase de build de l'image si les tests sont gérés par la CI)
RUN ./mvnw clean package -DskipTests

# Étape finale (image allégée)
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY --from=builder /app/target/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
