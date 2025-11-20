# Ce document est la pour initier l'instance docker de bulk en cas d'utilisation des données depuis le dump mongoDB

1. modify docker-compose
add 
```yml
volumes:
  elt_mongo_data: {}

services:
  mongo:
    image: mongo:bionic
    container_name: mongo
    restart: always
    ports:
    - "27017:27017"
    volumes:
    - elt_mongo_data:/data/db
    - ${PWD}/openfoodfacts-mongodbdump.gz:/openfoodfacts-mongodbdump.gz

  mongo-express:
    image: mongo-express:latest
    container_name: mongo-express
    restart: always
    ports:
    - "8081:8081"
    environment:
    ME_CONFIG_MONGODB_ADMINUSERNAME: ""
    ME_CONFIG_MONGODB_ADMINPASSWORD: ""
    ME_CONFIG_MONGODB_SERVER: mongo
```

2. lunch docker compose
```bash
docker-compose up -d
```

3. execute init.sh or execute line by line :
```bash
#A executer pour restaurer la base de donnees dans le container mongo quand il est vide (premier demarrage ou perte du volume)
docker exec -it mongo bash
mongorestore --gzip --archive=/openfoodfacts-mongodbdump.gz
exit
```