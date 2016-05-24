{
    "constant_score" : {
        "filter" : {
            "bool" : {
                "must" : [
                    {"term" : {"_id" : "${id}"}},
                    {"type" : { "value" : "MEDIA_FILE" }}
                ]
            }
        }
    }
}