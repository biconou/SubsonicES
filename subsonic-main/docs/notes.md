**Access to head**
http://localhost:9200/_plugin/head/


**Elasticsearch API**
https://www.elastic.co/guide/en/elasticsearch/reference/current/docs.html

Request to create a music folder

`curl -XPOST 'http://localhost:9200/musicfolders/MUSIC_FOLDER/' -d '{
    "path" : "/develop/biconouSubsonic_elasticSearch/subsonic-main/target/test-classes/MEDIAS/Music",
    "name" : "music",
    "enabled" : "true"
}'`

