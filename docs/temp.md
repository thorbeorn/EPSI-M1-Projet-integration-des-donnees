
3. Execute code :
### - Single node
```bash
docker exec -it spark-master /bin/bash
```
```bash
cd /app
sh /spark/bin/spark-submit \
/app/etl.jar
```

### - cluster
```bash
docker exec -it spark-master /bin/bash
```
```bash
cd /app
sh /spark/bin/spark-submit \
--master spark://spark-master:7077 \
/app/etl.jar
```