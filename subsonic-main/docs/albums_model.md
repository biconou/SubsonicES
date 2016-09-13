
The album information is spread among mutiple tables : 

MEDIA_FILE
  - id : id (visiblement non utile)
  - path : chemin du répertoire dans lequel se trouvent les chansons de l'album
  - folder : chemin du folder
  - album : nom de l'album
  - artist : artiste de l'album
  - year : année
  - genre : genre de l'album
  - variable_bit_rate
  - cover_art_path
  - play_count

ALBUM
  - id : id différent de celui présent dans MEDIA_FILE
  - path : idem MEDIA_FILE.path
  - name : idem MEDIA_FILE.album
  - artist : idem MEDIA_FILE.artist
  * song_count : 
  * duration_seconds : 
  - cover_art_path : idem MEDIA_FILE.cover_art_path
  - play_count : idem MEDIA_FILE.play_count
  - year : idem MEDIA_FILE.year
  - genre : idem MEDIA_FILE.genre
  
Seulement deux champs sont significatifs dans la table ALBUM

Dans l'index elasticSearch voici les champs présents pour : 

MEDIA_FILE > album
- albumName 
- artist 
- changed 
- childrenLastUpdated
- coverArtPath
- created
- folder
- genre
- id
- lastScanned
- parentPath
- path
- playCount
- present
- variableBitRate
- year

ALBUM
- artist
- created
- durationSeconds
- folderId
- genre
- id
- lastPlayed
- lastScanned
- name
- path
- playCount
- present
- songCount
- year

ajouter dans album : 

- albumName 
- changed 
- childrenLastUpdated
- coverArtPath
- folder
- parentPath
