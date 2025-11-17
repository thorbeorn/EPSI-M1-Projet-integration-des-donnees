
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