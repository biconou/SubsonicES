{
    "constant_score" : {
        "filter" : {
            "bool" : {
                "must" : [
                    {"term" : {"parentPath" : "${path}"}},
                    {"type" : { "value" : "MEDIA_FILE" }}
                ]
            }
        }
    }
}