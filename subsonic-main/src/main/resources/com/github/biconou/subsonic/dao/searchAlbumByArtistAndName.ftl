{
    "constant_score" : {
        "filter" : {
            "bool" : {
                "must" : [
                    {"term" : {"artist" : "${artist}"}},
                    {"term" : {"name" : "${name}"}},
                    {"type" : { "value" : "ALBUM" }}
                ]
            }
        }
    }
}