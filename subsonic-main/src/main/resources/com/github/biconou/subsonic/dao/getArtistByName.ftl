{
    "constant_score" : {
        "filter" : {
            "bool" : {
                "must" : [
                    {"term" : { "artist" : "${artist}" }},
                    {"term" : { "mediaType" : "DIRECTORY" }},
                    {"type" : { "value" : "MEDIA_FILE" }}
                ]
            }
        }
    }
}
