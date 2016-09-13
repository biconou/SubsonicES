{
    "constant_score" : {
        "filter" : {
            "bool" : {
                "must" : [
                    {"term" : {"artist" : "${artist}"}},
                    {"term" : {"name" : "${name}"}},
                    {"term" : {"mediaType" : "ALBUM"}},
                    {"type" : { "value" : "MEDIA_FILE" }}
                ]
            }
        }
    }
}