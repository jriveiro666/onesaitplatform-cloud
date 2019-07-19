var mongo = connect("platformadmin:0pen-platf0rm-2018!@localhost:27017/admin")

var db = mongo.getSiblingDB('onesaitplatform_rtdb')



print("Updating Sofia2 contextData -> Onesait contextData");

var collections = db.getCollectionNames();

collections.forEach(function(collection){
	printjson(db.getCollection(collection).updateMany( {}, { $rename: { "contextData.kp": "contextData.deviceTemplate","contextData.kpInstance":"contextData.device", "contextData.sessionKey":"contextData.clientSession"}}))

});

