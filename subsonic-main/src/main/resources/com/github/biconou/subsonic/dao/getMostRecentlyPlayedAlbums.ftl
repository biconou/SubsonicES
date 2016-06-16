{
    "constant_score" : {
        "filter" : {
            "bool" : {
                "must" : [
                    {"term" : { "mediaType" : "ALBUM" }},
                    {"type" : { "value" : "MEDIA_FILE" }}
                ],
                "filter" : {
            		"exists" : {"field" : "lastPlayed"}
            	}
        	}
    	}
    }
}