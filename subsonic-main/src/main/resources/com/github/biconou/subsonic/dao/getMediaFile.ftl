{
    "constant_score" : {
        "filter" : {
            "bool" : {
                "must" : [
                    {"term" : {"id" : "${id}"}},
                    {"type" : { "value" : "MEDIA_FILE" }}
                ]
            }
        }
    }
}