class UrlMappings {

	static mappings = {
		"/$controller/$action?/$id?"{
			constraints {
				// apply constraints here
			}
		}

		"/"(view:"/index")
		"500"(view:'/error')

        "/upload"(controller:"storage"){
            action = [POST:"upload"]
        }

        "/download"(controller:"storage"){
            action = [GET : "download"]
        }


        "/image/associated" (controller:"image") {
            action = [GET:"associated"]
        }

        "/image/nested" (controller:"image") {
            action = [GET:"nested"]
        }

        "/image/properties" (controller:"image") {
            action = [GET:"properties"]
        }

        "/image/thumb.$format" (controller:"image") {
            action = [GET:"thumb"]
        }


	}
}
