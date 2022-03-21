FROM gradle:7.4-jdk17-alpine

WORKDIR /trashcan
COPY . .
RUN gradle bootWar
RUN cp `ls -1 ./build/libs/*.war | head -1` ./trashcan.war &&  ls -la