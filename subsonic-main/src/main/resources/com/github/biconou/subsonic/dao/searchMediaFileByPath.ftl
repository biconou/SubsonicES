{
    "constant_score" : {
        "filter" : {
            "bool" : {
                "must" : [
                    {"term" : {"path" : "${path}"}},
                    {"type" : { "value" : "MEDIA_FILE" }}
                ]
            }
        }
    }
}