#A executer pour restaurer la base de donnees dans le container mongo quand il est vide (premier demarrage ou perte du volume)
docker exec -it mongodb bash
mongorestore --gzip --archive=/openfoodfacts-mongodbdump.gz
exit