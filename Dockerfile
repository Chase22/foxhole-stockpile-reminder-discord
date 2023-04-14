FROM eclipse-temurin:17-alpine
COPY build/libs/foxhole-stockpile-renewal-bot.jar foxhole-stockpile-renewal-bot.jar
CMD ["java","-jar","foxhole-stockpile-renewal-bot.jar"]

EXPOSE 8080