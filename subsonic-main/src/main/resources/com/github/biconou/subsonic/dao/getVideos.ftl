{
    "constant_score" : {
        "filter" : {
            "bool" : {
                "must" : [
                    {"term" : { "mediaType" : "VIDEO" }},
                    {"type" : { "value" : "MEDIA_FILE" }}
                ]
            }
        }
    }
}
