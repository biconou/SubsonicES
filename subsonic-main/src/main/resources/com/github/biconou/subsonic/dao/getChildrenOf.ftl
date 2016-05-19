{
    "constant_score" : {
        "filter" : {
            "bool" : {
                "must" : [
                    {"term" : {"parentPath" : "${path}"}},
                    {"term" : {"present" : "true"}},
                    {"type" : { "value" : "MEDIA_FILE" }}
                ]
            }
        }
    }
}