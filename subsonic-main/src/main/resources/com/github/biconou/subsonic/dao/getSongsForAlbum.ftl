{
    "constant_score" : {
        "filter" : {
            "bool" : {
                "must" : [
                    {"term" : {"albumArtist" : "${artist}"}},
                    {"term" : {"albumName" : "${album}"}},
                    {"type" : { "value" : "MEDIA_FILE" }}
                ],
                "should" : [
                    {"term" : {"mediaType" : "MUSIC"}},
                    {"term" : {"mediaType" : "AUDIOBOOK"}},
                    {"term" : {"mediaType" : "PODCAST"}}
                ]
            }
        }
    }
}