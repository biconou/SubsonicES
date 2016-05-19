{
    "constant_score" : {
        "filter" : {
            "bool" : {
                "must" : [
                    {"term" : { "genre" : "${genre}" }},
                    {"term" : { "mediaType" : "MUSIC" }},
                    {"type" : { "value" : "MEDIA_FILE" }}
                ]
            }
        }
    }
}
