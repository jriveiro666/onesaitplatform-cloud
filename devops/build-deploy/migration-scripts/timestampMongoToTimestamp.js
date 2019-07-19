var mongo = connect("platformadmin:0pen-platf0rm-2018!@localhost:27017/admin")

var db = mongo.getSiblingDB('onesaitplatform_rtdb')



print("Updating Sofia2 contextData -> Onesait contextData");

var collections = db.getCollectionNames();

collections.forEach(function(collection){
	db.getCollection(collection).find().forEach(function(f){
		var d = f;
		var id = f._id;
		if(f.contextData){
			var date = new Date(f.contextData.timestamp);
			var dateStr = date.toString().split("GMT")[0];
			db.getCollection(collection).update({"_id":id},{$set:{"contextData.timestamp":dateStr.substring(0, dateStr.length-1)}})
		}
	});
});

