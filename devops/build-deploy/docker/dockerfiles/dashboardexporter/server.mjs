import express from 'express';
import {port, host, appname} from "./conf/vars.mjs";
import * as controller from "./lib/controllers.mjs";
import logger from "./lib/logger.service.mjs";

logger.info("Starting " + appname + " server " + host + " in port " + port);
const app = express();

(async () => {
    
    logger.info("Generating endpoints");
    app.get('/imgfromurl', controller.imgController);
    app.get('/pdffromurl', controller.pdfController);

    app.listen(port, host, (err) => {
    if (err) {
        return logger.err('Something bad happened', err)
    }
    logger.info(appname + ` By onesaitPlatform is listening on ${port}`)

    })
})();