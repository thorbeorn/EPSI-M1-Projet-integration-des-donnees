1. lunch docker-compose in /elt

2. execute init.sh or execute line by line :
```bash
#A executer pour restaurer la base de donnees dans le container mongo quand il est vide (premier demarrage ou perte du volume)
docker exec -it mongo bash
mongorestore --gzip --archive=/openfoodfacts-mongodbdump.gz
exit
```

3. Execute code :
### - Single node
```bash
docker exec -it spark-master /bin/bash
```
```bash
/spark/bin/spark-submit \
  --class fr.thorbeorn.etl.Main \
  --master spark://spark-master:7077 \
  --deploy-mode client \
  /tmp/etl.jar
```

### - cluster